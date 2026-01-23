#!/bin/bash

# 블루그린 배포 - Nginx 포트 스위칭
# develop 브랜치는 단순 재시작 (스위칭 없음, 8080 포트 고정)
# release, main 브랜치는 블루그린 배포 (포트 스위칭)

set -e

APP_DIR=/home/ubuntu/app/be
NGINX_CONF=/etc/nginx/conf.d/service-url.inc

# 배포 환경 설정 로드
if [ -f "$APP_DIR/deploy_env.sh" ]; then
  echo "🔧 배포 환경 설정 로드 중..."
  source "$APP_DIR/deploy_env.sh"
  echo "  ✅ BRANCH_NAME: $BRANCH_NAME"
else
  echo "⚠️  deploy_env.sh 파일을 찾을 수 없습니다. 기본값(develop)을 사용합니다."
fi

BRANCH_NAME="${BRANCH_NAME:-develop}"

echo "==== [ValidateService] Nginx 포트 스위칭 (브랜치: $BRANCH_NAME) ===="

# develop 브랜치: 스위칭 없이 종료
if [ "$BRANCH_NAME" = "develop" ]; then
  echo "📍 개발 환경: 포트 스위칭을 건너뜁니다 (8080 포트 고정)."
  echo "✅ 배포가 완료되었습니다 (포트: 8080)."
  echo "==== [ValidateService] Nginx 포트 스위칭 완료 ===="
  exit 0
fi

# release, main 브랜치: 블루그린 배포
echo "📍 블루그린 배포 모드: 포트 스위칭을 진행합니다."

# 현재 포트와 유휴 포트 확인
if [ ! -f "$APP_DIR/current_port.txt" ] || [ ! -f "$APP_DIR/idle_port.txt" ]; then
  echo "❌ 포트 정보를 찾을 수 없습니다."
  exit 1
fi

CURRENT_PORT=$(cat "$APP_DIR/current_port.txt")
IDLE_PORT=$(cat "$APP_DIR/idle_port.txt")

echo "📍 현재 포트: $CURRENT_PORT"
echo "🎯 새 포트: $IDLE_PORT"

# Nginx 설정 파일 변경
echo "🔄 Nginx 설정을 변경합니다..."

echo "set \$service_url http://127.0.0.1:$IDLE_PORT;" | sudo tee "$NGINX_CONF" > /dev/null

# Nginx 설정 테스트
echo "🧪 Nginx 설정을 테스트합니다..."
if ! sudo nginx -t; then
  echo "❌ Nginx 설정 테스트 실패"
  exit 1
fi

# Nginx reload
echo "🔄 Nginx를 reload합니다..."
sudo nginx -s reload

echo "✅ Nginx가 포트 $IDLE_PORT로 전환되었습니다."

# 잠시 대기 후 이전 버전 종료
echo "⏳ 5초 대기 후 이전 버전을 종료합니다..."
sleep 5

# 이전 포트에서 실행 중인 프로세스 찾기
OLD_PID=$(lsof -ti "tcp:$CURRENT_PORT" || echo "")

if [ -z "$OLD_PID" ]; then
  echo "ℹ️  이전 포트($CURRENT_PORT)에 실행 중인 프로세스가 없습니다."
else
  echo "🔄 이전 버전(PID: $OLD_PID, PORT: $CURRENT_PORT)을 종료합니다..."
  kill -15 "$OLD_PID"

  # 종료 대기 (최대 30초)
  for i in {1..30}; do
    if ! ps -p "$OLD_PID" > /dev/null 2>&1; then
      echo "✅ 이전 버전이 정상적으로 종료되었습니다."
      break
    fi

    if [ "$i" -eq 30 ]; then
      echo "⚠️  이전 버전이 30초 내에 종료되지 않았습니다. 강제 종료합니다."
      kill -9 "$OLD_PID" || true
    fi

    sleep 1
  done
fi

# PID 파일 정리
rm -f "$APP_DIR/app_${CURRENT_PORT}.pid"

echo "==== [ValidateService] Nginx 포트 스위칭 완료 ===="
echo "🎉 블루그린 배포가 성공적으로 완료되었습니다!"
echo "   - 이전 포트: $CURRENT_PORT (종료됨)"
echo "   - 현재 포트: $IDLE_PORT (운영 중)"
