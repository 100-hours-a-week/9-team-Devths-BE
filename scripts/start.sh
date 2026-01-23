#!/bin/bash

# 블루그린 배포 - 유휴 포트에서 새 애플리케이션 시작
# develop 브랜치는 단순 재시작 (8080 포트 고정)
# release, main 브랜치는 블루그린 배포

set -e

APP_DIR=/home/ubuntu/app/be
DEPLOY_LOG=$APP_DIR/deploy.log
ENV_FILE=$APP_DIR/.env

echo "==== [ApplicationStart] 애플리케이션 시작 ===="

# 배포 환경 설정 로드
if [ -f "$APP_DIR/deploy_env.sh" ]; then
  echo "🔧 배포 환경 설정을 로드합니다..."
  source $APP_DIR/deploy_env.sh
  echo "  - 브랜치: $BRANCH_NAME"
  echo "  - 프로파일: $SPRING_PROFILE"
  echo "  - Parameter Store 경로: $PARAMETER_STORE_PATH"
else
  echo "⚠️  배포 환경 설정 파일을 찾을 수 없습니다. 기본값을 사용합니다."
  export SPRING_PROFILE="${SPRING_PROFILE:-prod}"
  export PARAMETER_STORE_PATH="${PARAMETER_STORE_PATH:-/devths/prod/}"
fi

# 유휴 포트 확인
if [ ! -f "$APP_DIR/idle_port.txt" ]; then
  echo "❌ 유휴 포트 정보를 찾을 수 없습니다."
  exit 1
fi

IDLE_PORT=$(cat $APP_DIR/idle_port.txt)
echo "🎯 배포 대상 포트: $IDLE_PORT"

# JAR 파일 찾기
JAR_FILE=$(find $APP_DIR -maxdepth 1 -name "*.jar" | grep -v 'plain' | head -n 1)

if [ -z "$JAR_FILE" ]; then
  echo "❌ JAR 파일을 찾을 수 없습니다."
  exit 1
fi

echo "📦 JAR 파일: $JAR_FILE"

# AWS Parameter Store에서 환경변수 가져오기
echo "🔐 AWS Parameter Store에서 환경변수를 가져옵니다..."

# Parameter Store 경로 (deploy_env.sh에서 로드됨)
PARAM_PATH="$PARAMETER_STORE_PATH"

# Parameter Store에서 모든 파라미터 가져오기
aws ssm get-parameters-by-path \
  --path "$PARAM_PATH" \
  --with-decryption \
  --region ap-northeast-2 \
  --query 'Parameters[*].[Name,Value]' \
  --output text | while read -r name value; do
    # 경로에서 파라미터 이름만 추출 (예: /devths/prod/DB_URL -> DB_URL)
    PARAM_NAME=$(echo "$name" | awk -F'/' '{print $NF}')
    echo "export $PARAM_NAME='$value'" >> $ENV_FILE
  done

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ 환경변수 파일 생성에 실패했습니다."
  exit 1
fi

echo "✅ 환경변수 로드 완료"

# 환경변수 로드
source $ENV_FILE

# 애플리케이션 시작
echo "🚀 애플리케이션을 시작합니다..."

nohup java -jar \
  -Dserver.port=$IDLE_PORT \
  -Dspring.profiles.active=${SPRING_PROFILE:-prod} \
  $JAR_FILE \
  > $DEPLOY_LOG 2>&1 &

APP_PID=$!
echo $APP_PID > $APP_DIR/app_${IDLE_PORT}.pid

echo "✅ 애플리케이션이 시작되었습니다 (PID: $APP_PID, PORT: $IDLE_PORT)"

# 애플리케이션 시작 대기 (최대 30초)
echo "⏳ 애플리케이션 초기화를 기다립니다..."
sleep 10

# 프로세스가 정상적으로 실행 중인지 확인
if ! ps -p $APP_PID > /dev/null 2>&1; then
  echo "❌ 애플리케이션 시작에 실패했습니다. 로그를 확인하세요."
  tail -n 50 $DEPLOY_LOG
  exit 1
fi

echo "==== [ApplicationStart] 완료 ===="
