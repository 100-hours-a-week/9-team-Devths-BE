#!/usr/bin/env bash

ABSPATH=$(readlink -f $0)
ABSDIR=$(dirname $ABSPATH)
source ${ABSDIR}/profile.sh

IDLE_PORT=$(find_idle_port)
HEALTH_URL="http://localhost:${IDLE_PORT}/actuator/health"

echo "> Health Check Start!"
echo "> IDLE_PORT: $IDLE_PORT"
echo "> curl -s $HEALTH_URL"
sleep 10

for RETRY_COUNT in {1..10}
do
  RESPONSE=$(curl -s "$HEALTH_URL" || true)
  UP_COUNT=$(echo "${RESPONSE}" | grep -E '"status"[[:space:]]*:[[:space:]]*"UP"' | wc -l)

  if [ ${UP_COUNT} -ge 1 ]
  then # $up_count >= 1 ("status":"UP" 이 포함되어 있다면 성공)
      echo "> Health check 성공"
      break
  else
      echo "> Health check의 응답을 알 수 없거나 혹은 실행 상태가 아닙니다."
      echo "> Health check: ${RESPONSE}"
  fi

  if [ ${RETRY_COUNT} -eq 10 ]
  then
    echo "> Health check 실패. "
    echo "> Nginx에 연결하지 않고 배포를 종료합니다."
    exit 1
  fi

  echo "> Health check 연결 실패. 재시도..."
  sleep 10
done
