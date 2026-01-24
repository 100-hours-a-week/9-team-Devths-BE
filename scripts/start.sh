#!/usr/bin/env bash

ABSPATH=$(readlink -f $0)
ABSDIR=$(dirname $ABSPATH)
source ${ABSDIR}/profile.sh

IDLE_PROFILE=$(find_idle_profile)
IDLE_PORT=$(find_idle_port)

echo "> IDLE_PROFILE: $IDLE_PROFILE"
echo "> IDLE_PORT: $IDLE_PORT"

# 1. 새 배포를 시작하기 전, IDLE 포트에서 돌고 있는 애플리케이션이 있다면 종료 (혹시 모를 충돌 방지)
echo "> $IDLE_PORT 포트에서 구동 중인 애플리케이션 pid 확인"
IDLE_PID=$(lsof -ti tcp:${IDLE_PORT})

if [ -z ${IDLE_PID} ]
then
  echo "> 현재 구동 중인 애플리케이션이 없으므로 종료하지 않습니다."
else
  echo "> kill -15 $IDLE_PID"
  kill -15 ${IDLE_PID}
  sleep 5
fi

# 2. 배포 파일 준비
echo "> 배포 파일 복사"
APP_NAME="devths-be"
REPOSITORY=/home/ubuntu/be

# 로그 디렉토리 생성
mkdir -p $REPOSITORY/logs
LOG_FILE=$REPOSITORY/logs/application.log

BUILD_JAR=$(ls $REPOSITORY/application.jar)

echo "> 새 애플리케이션 배포"
nohup java -jar \
    -Dspring.config.location=classpath:/application.yml \
    -Dspring.profiles.active=$IDLE_PROFILE \
    -Dserver.port=$IDLE_PORT \
    $BUILD_JAR >> $LOG_FILE 2>&1 &

echo "> 배포 완료. PID 확인..."
# 잠시 대기 후 확인
sleep 3
NEW_PID=$(lsof -ti tcp:${IDLE_PORT})
if [ -z ${NEW_PID} ]
then
    echo "> ❌ 배포 실패: 프로세스가 실행되지 않았습니다."
    echo "=========== 실행 로그 ($LOG_FILE) ==========="
    cat $LOG_FILE
    echo "==========================================="
    exit 1
else
    echo "> ✅ 배포 성공 (PID: $NEW_PID)"
fi
