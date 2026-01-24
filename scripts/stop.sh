#!/usr/bin/env bash

ABSPATH=$(readlink -f $0)
ABSDIR=$(dirname $ABSPATH)
source ${ABSDIR}/profile.sh

echo "> 기존 서비스 종료 (Idle이 아닌 Active 포트 찾아서 종료)"

# 지금 시점(switch 이후)에서 find_idle_profile을 호출하면 
# 이미 switch가 되었으므로, 우리가 종료해야 할 '구버전'은 
# 현재 profile 함수가 가리키는 'IDLE' Port입니다.
# (예: 8080->8081로 배포/스위치함. 이제 Nginx는 8081을 봄. 
#       이때 find_idle을 하면 8080이 나옴. 이게 구버전임!)

TARGET_PORT=$(find_idle_port)

echo "> 종료 대상 포트: $TARGET_PORT"
TARGET_PID=$(lsof -ti tcp:${TARGET_PORT})

if [ -z ${TARGET_PID} ]
then
  echo "> 현재 구동 중인 기존 애플리케이션이 없으므로 종료하지 않습니다."
else
  echo "> kill -15 $TARGET_PID"
  kill -15 ${TARGET_PID}
  sleep 5
fi
