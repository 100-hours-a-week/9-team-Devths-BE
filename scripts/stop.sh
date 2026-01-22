#!/bin/bash

# 블루그린 배포 - 유휴 포트의 기존 프로세스 종료

set -e

BLUE_PORT=8080
GREEN_PORT=8081
APP_DIR=/home/ubuntu/app
NGINX_CONF=/etc/nginx/conf.d/service-url.inc

echo "==== [ApplicationStop] 유휴 포트 확인 및 기존 프로세스 종료 ===="

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

echo "==== [ApplicationStop] 완료 ===="
