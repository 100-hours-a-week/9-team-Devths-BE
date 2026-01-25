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

# 3. deployment.env에서 브랜치 정보 읽기
if [ -f $REPOSITORY/deployment.env ]; then
	source $REPOSITORY/deployment.env
	echo "> deployment.env 파일 로드 완료"
else
	echo "> ⚠️ deployment.env 파일이 없습니다. 기본값(develop) 사용"
	DEPLOY_BRANCH="develop"
fi

# 4. 브랜치별 프로필 및 파라미터 스토어 경로 매핑
case $DEPLOY_BRANCH in
	develop)
		SPRING_PROFILE="dev"
		PARAM_STORE_PATH="/Dev/BE"
		;;
	release)
		SPRING_PROFILE="stg"
		PARAM_STORE_PATH="/Stg/BE"
		;;
	main)
		SPRING_PROFILE="prod"
		PARAM_STORE_PATH="/Main/BE"
		;;
	*)
		echo "> ⚠️ 알 수 없는 브랜치: $DEPLOY_BRANCH. 기본값(dev) 사용"
		SPRING_PROFILE="dev"
		PARAM_STORE_PATH="/Dev/BE"
		;;
esac

echo "> DEPLOY_BRANCH: $DEPLOY_BRANCH"
echo "> SPRING_PROFILE: $SPRING_PROFILE"
echo "> PARAM_STORE_PATH: $PARAM_STORE_PATH"

# 5. AWS Parameter Store에서 환경 변수 가져오기
echo "> AWS Parameter Store에서 환경 변수 로드 중..."
PARAMS=$(aws ssm get-parameters-by-path \
	--path "$PARAM_STORE_PATH" \
	--recursive \
	--with-decryption \
	--query 'Parameters[*].[Name,Value]' \
	--output text 2>/dev/null)

if [ $? -eq 0 ] && [ -n "$PARAMS" ]; then
	echo "> ✅ 파라미터 스토어에서 환경 변수를 성공적으로 로드했습니다."

	# 환경 변수로 설정 (파일로 저장)
	ENV_FILE=$REPOSITORY/.env.ssm
	> $ENV_FILE  # 파일 초기화

	while IFS=$'\t' read -r NAME VALUE; do
		# 파라미터 이름에서 경로 제거 후 환경 변수 이름으로 변환
		# 예: /Dev/BE/DATABASE_URL -> DATABASE_URL
		VAR_NAME=$(echo $NAME | sed "s|$PARAM_STORE_PATH/||" | tr '[:lower:]' '[:upper:]' | tr '-' '_')
		echo "export $VAR_NAME='$VALUE'" >> $ENV_FILE
		echo "  - $VAR_NAME 설정 완료"
	done <<< "$PARAMS"

	source $ENV_FILE
	echo "> ✅ 환경 변수 설정 완료"
else
	echo "> ⚠️ 파라미터 스토어에서 환경 변수를 가져올 수 없습니다. 계속 진행합니다."
fi

BUILD_JAR=$(ls $REPOSITORY/application.jar)

echo "> 새 애플리케이션 배포"
nohup java -jar \
    -Dspring.config.location=classpath:/application.yml \
    -Dspring.profiles.active=$IDLE_PROFILE,$SPRING_PROFILE \
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
