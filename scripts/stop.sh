#!/bin/bash

# 블루그린 배포 - 유휴 포트의 기존 프로세스 종료
# develop 브랜치는 단순 재시작 (8080 포트 고정)
# release, main 브랜치는 블루그린 배포

set -e

BLUE_PORT=8080
GREEN_PORT=8081
APP_DIR=/home/ubuntu/app/be
NGINX_CONF=/etc/nginx/conf.d/service-url.inc

# 배포 환경 설정 로드
if [ -f "$APP_DIR/deploy_env.sh" ]; then
  echo "🔧 배포 환경 설정 로드 중..."
  source $APP_DIR/deploy_env.sh
  echo "  ✅ BRANCH_NAME: $BRANCH_NAME"
  echo "  ✅ SPRING_PROFILE: $SPRING_PROFILE"
  echo "  ✅ PARAMETER_STORE_PATH: $PARAMETER_STORE_PATH"
else
  echo "⚠️  deploy_env.sh 파일을 찾을 수 없습니다. 기본값(develop)을 사용합니다."
fi

BRANCH_NAME="${BRANCH_NAME:-develop}"

echo "==== [ApplicationStop] 프로세스 종료 (브랜치: $BRANCH_NAME) ===="

# develop 브랜치: 단순 재시작 (8080 포트 고정)
if [ "$BRANCH_NAME" = "develop" ]; then
  echo "📍 개발 환경: 8080 포트에서 단순 재시작합니다."

  TARGET_PORT=$BLUE_PORT
  TARGET_PID=$(lsof -ti tcp:$TARGET_PORT || echo "")

  if [ -z "$TARGET_PID" ]; then
    echo "✅ 포트($TARGET_PORT)에 실행 중인 프로세스가 없습니다."
  else
    echo "🔄 포트($TARGET_PORT)에서 실행 중인 프로세스(PID: $TARGET_PID)를 종료합니다..."
    kill -15 $TARGET_PID

    # 종료 대기 (최대 30초)
    for i in {1..30}; do
      if ! ps -p $TARGET_PID > /dev/null 2>&1; then
        echo "✅ 프로세스가 정상적으로 종료되었습니다."
        break
      fi

      if [ $i -eq 30 ]; then
        echo "⚠️  프로세스가 30초 내에 종료되지 않았습니다. 강제 종료합니다."
        kill -9 $TARGET_PID || true
      fi

      sleep 1
    done
  fi

  # develop은 항상 8080 포트 사용
  echo $BLUE_PORT > $APP_DIR/current_port.txt
  echo $BLUE_PORT > $APP_DIR/idle_port.txt

else
  # release, main 브랜치: 블루그린 배포
  echo "📍 블루그린 배포 모드: 유휴 포트를 확인합니다."

  # nginx 설정 파일에서 현재 사용 중인 포트 확인
  if [ -f "$NGINX_CONF" ]; then
    CURRENT_PORT=$(grep -oP '127\.0\.0\.1:\K\d+' $NGINX_CONF || echo "")
  fi

  if [ -z "$CURRENT_PORT" ]; then
    # nginx 설정에서 포트를 확인할 수 없으면 실행 중인 프로세스 확인
    if lsof -ti tcp:$BLUE_PORT > /dev/null 2>&1; then
      CURRENT_PORT=$BLUE_PORT
      echo "ℹ️  BLUE 포트($BLUE_PORT)에서 실행 중인 프로세스 발견"
    elif lsof -ti tcp:$GREEN_PORT > /dev/null 2>&1; then
      CURRENT_PORT=$GREEN_PORT
      echo "ℹ️  GREEN 포트($GREEN_PORT)에서 실행 중인 프로세스 발견"
    else
      # 둘 다 없으면 기본적으로 BLUE를 현재로 가정
      echo "⚠️  현재 포트를 확인할 수 없습니다. BLUE(8080)를 현재 포트로 가정합니다."
      CURRENT_PORT=$BLUE_PORT
    fi
  fi

  echo "📍 현재 운영 중인 포트: $CURRENT_PORT"

  # 유휴 포트 결정
  if [ "$CURRENT_PORT" -eq "$BLUE_PORT" ]; then
    IDLE_PORT=$GREEN_PORT
  else
    IDLE_PORT=$BLUE_PORT
  fi

  echo "🎯 유휴 포트: $IDLE_PORT (새 버전이 여기에 배포됩니다)"

  # 유휴 포트에서 실행 중인 프로세스 찾기
  IDLE_PID=$(lsof -ti tcp:$IDLE_PORT || echo "")

  if [ -z "$IDLE_PID" ]; then
    echo "✅ 유휴 포트($IDLE_PORT)에 실행 중인 프로세스가 없습니다."
  else
    echo "🔄 유휴 포트($IDLE_PORT)에서 실행 중인 프로세스(PID: $IDLE_PID)를 종료합니다..."
    kill -15 $IDLE_PID

    # 종료 대기 (최대 30초)
    for i in {1..30}; do
      if ! ps -p $IDLE_PID > /dev/null 2>&1; then
        echo "✅ 프로세스가 정상적으로 종료되었습니다."
        break
      fi

      if [ $i -eq 30 ]; then
        echo "⚠️  프로세스가 30초 내에 종료되지 않았습니다. 강제 종료합니다."
        kill -9 $IDLE_PID || true
      fi

      sleep 1
    done
  fi

  # 현재/유휴 포트 정보를 파일로 저장 (다른 스크립트에서 사용)
  echo $CURRENT_PORT > $APP_DIR/current_port.txt
  echo $IDLE_PORT > $APP_DIR/idle_port.txt
fi

echo "==== [ApplicationStop] 완료 ===="
