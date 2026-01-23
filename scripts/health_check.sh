#!/bin/bash

# 블루그린 배포 - Actuator 기반 헬스체크
# develop 브랜치는 단순 재시작 (8080 포트 고정)
# release, main 브랜치는 블루그린 배포

set -e

APP_DIR=/home/ubuntu/app/be

# APP_DIR 디렉토리 확인 및 생성 (sudo 사용)
if [ ! -d "$APP_DIR" ]; then
  echo "📁 APP_DIR 디렉토리가 없습니다. 생성합니다: $APP_DIR"
  sudo mkdir -p $APP_DIR
  sudo chown ubuntu:ubuntu $APP_DIR
  sudo chmod 755 $APP_DIR
fi

# 배포 환경 설정 로드
if [ -f "$APP_DIR/deploy_env.sh" ]; then
  source $APP_DIR/deploy_env.sh
fi

BRANCH_NAME="${BRANCH_NAME:-develop}"

echo "==== [ValidateService] 헬스체크 시작 (브랜치: $BRANCH_NAME) ===="

# develop 브랜치: 블루그린 없이 8080 포트 고정
if [ "$BRANCH_NAME" = "develop" ]; then
  echo "📍 개발 환경: 8080 포트 헬스체크"
  IDLE_PORT=8080
else
  # release/main 브랜치: 블루그린 배포
  echo "📍 블루그린 배포 모드: 유휴 포트를 확인합니다."

  if [ ! -f "$APP_DIR/idle_port.txt" ]; then
    echo "⚠️  유휴 포트 정보를 찾을 수 없습니다. 기본 포트를 사용합니다."
    IDLE_PORT=8080
    echo "📍 기본 포트 사용: $IDLE_PORT"
  else
    IDLE_PORT=$(cat $APP_DIR/idle_port.txt)
  fi
fi
HEALTH_CHECK_URL="http://localhost:$IDLE_PORT/actuator/health"

echo "🏥 헬스체크 URL: $HEALTH_CHECK_URL"

# 헬스체크 수행 (최대 30회, 10초 간격)
MAX_RETRIES=30
RETRY_INTERVAL=10

for i in $(seq 1 $MAX_RETRIES); do
  echo "  시도 $i/$MAX_RETRIES..."

  # HTTP 상태 코드 확인
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_CHECK_URL" || echo "000")

  if [ "$HTTP_CODE" = "200" ]; then
    # 헬스체크 응답 내용 확인
    HEALTH_STATUS=$(curl -s "$HEALTH_CHECK_URL" | jq -r '.status' 2>/dev/null || echo "")

    if [ "$HEALTH_STATUS" = "UP" ]; then
      echo "✅ 헬스체크 성공! (포트: $IDLE_PORT, 상태: UP)"
      echo "==== [ValidateService] 헬스체크 완료 ===="
      exit 0
    else
      echo "⚠️  HTTP 200이지만 상태가 UP이 아닙니다. (상태: $HEALTH_STATUS)"
    fi
  else
    echo "⚠️  헬스체크 실패 (HTTP $HTTP_CODE). ${RETRY_INTERVAL}초 후 재시도..."
  fi

  if [ $i -lt $MAX_RETRIES ]; then
    sleep $RETRY_INTERVAL
  fi
done

# 최대 재시도 횟수 초과
echo "❌ 헬스체크 실패: 최대 재시도 횟수 초과"

# 실패 시 로그 출력
if [ -f "$APP_DIR/deploy.log" ]; then
  echo ""
  echo "==== 애플리케이션 로그 (마지막 50줄) ===="
  tail -n 50 "$APP_DIR/deploy.log"
fi

exit 1
