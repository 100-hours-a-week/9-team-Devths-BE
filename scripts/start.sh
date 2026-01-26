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
if [ -z "$BUILD_JAR" ]; then
    echo "❌ [Artifact Error] 실행 가능한 JAR 파일을 찾을 수 없습니다."
    exit 1
fi

# 배포 그룹에 따른 SSM Path 파싱 (CodeDeploy 환경변수: DEPLOYMENT_GROUP_NAME 사용)
SSM_PATH="/Dev/BE/" # 기본값 (Dev)

if [[ "$DEPLOYMENT_GROUP_NAME" == *"Prod"* ]]; then
    SSM_PATH="/Prod/BE/"
fi

echo "> AWS SSM Parameter Store에서 환경변수 주입 (Path: $SSM_PATH)"
# jq 존재 여부 확인 (AMI에 미리 설치되어 있어야 함)
if ! command -v jq &> /dev/null
then
    echo "❌ [Dependency Error] jq가 설치되어 있지 않습니다. 서버 셋팅을 확인해주세요."
    exit 1
fi

# 경로 하위의 모든 파라미터를 가져와서 export (페이지네이션 이슈 방지를 위해 max-items 설정)
PARAMETERS=$(aws ssm get-parameters-by-path --path "$SSM_PATH" --recursive --with-decryption --region ap-northeast-2 --max-items 100 --output json)

if [ -z "$PARAMETERS" ] || [ "$PARAMETERS" == "null" ]; then
    echo "❌ [Configuration Error] SSM 파라미터를 가져오지 못했습니다. IAM 권한이나 경로($SSM_PATH)를 확인해주세요."
    exit 1
else
    # jq로 "KEY=VALUE" 형식으로 변환 후, while loop로 읽어서 export 수행
    # < <(...) 프로세스 치환을 사용하여 서브쉘 문제 방지
    while IFS='=' read -r key value; do
        if [ -n "$key" ]; then
            export "$key"="$value"
            echo "> Exported: $key"
        fi
    done < <(echo "$PARAMETERS" | jq -r --arg path "$SSM_PATH" '.Parameters[] | ((.Name | sub($path; "") | gsub("[.-]"; "_") | ascii_upcase) + "=" + .Value)')
fi

# 포트 확인
echo "> $IDLE_PORT 포트가 비어있는지 2차 확인"
if lsof -Pi :$IDLE_PORT -sTCP:LISTEN -t >/dev/null ; then
    echo "⚠️ [Port Warning] 포트 $IDLE_PORT 가 아직 사용 중입니다. 강제 종료를 시도합니다."
    kill -15 $(lsof -Pi :$IDLE_PORT -sTCP:LISTEN -t) || true
    sleep 3
fi

echo "> 새 애플리케이션 배포"

# [수정] CodeDeploy 프로세스 정리 대상에서 제외
export FOR_CODELDP_IGN=true

nohup java -jar \
    -Dspring.config.location=classpath:/application.yml \
    -Dspring.profiles.active=$IDLE_PROFILE,$SPRING_PROFILE \
    -Dserver.port=$IDLE_PORT \
    $BUILD_JAR >> $LOG_FILE 2>&1 &

# [수정] 현재 세션에서 프로세스 분리
disown

echo "> 배포 완료. 구동 대기 중..."

# 3. 구동 대기 (최대 60초)
SUCCESS=false
for i in {1..20}
do
    sleep 3
    echo "> 구동 확인 ($i/60)..."
    
    # 1) lsof로 포트가 열렸는지 1차 확인 (빠른 실패 감지용)
    if ! lsof -Pi :$IDLE_PORT -sTCP:LISTEN -t >/dev/null; then
         echo "   ... 아직 포트가 열리지 않음"
         continue
    fi    

    # 2) curl로 실제 응답 확인 (Health Check)
    # Actuator Health 엔드포인트(/actuator/health)가 UP인지 확인
    # 실패 시(Connection Refused 등)에는 000 반환됨
    HEALTH_URL="http://localhost:$IDLE_PORT/actuator/health"
    TMP_BODY=$(mktemp)
    HTTP_CODE=$(curl -s -o "$TMP_BODY" -w "%{http_code}" "$HEALTH_URL" || echo "000")
    RESPONSE=$(cat "$TMP_BODY" 2>/dev/null || echo "")
    rm -f "$TMP_BODY"

    if [ "$HTTP_CODE" == "200" ] && echo "$RESPONSE" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
        # 성공 시 PID 추출
        CURRENT_PID=$(lsof -ti tcp:${IDLE_PORT})
        echo "> ✅ Health Check 성공 (HTTP 200). 구동 완료 (PID: $CURRENT_PID)."
        SUCCESS=true
        break
    else
        echo "   ... 포트는 열렸으나 아직 UP 응답 없음 (HTTP $HTTP_CODE)"
    fi
done

if [ "$SUCCESS" = false ]; then
    echo "❌ [Boot Error] 지정된 시간(180초) 내에 애플리케이션이 포트($IDLE_PORT)를 점유하지 못했습니다."
    
    # 로그 분석
    echo "=========== 실패 원인 분석 (로그 스캔) ==========="
    if grep -q "PortInUseException" $LOG_FILE || grep -q "BindException" $LOG_FILE; then
        echo "❌ [Reason: Port] 포트($IDLE_PORT)가 이미 사용 중입니다."
    
    elif grep -q "AccessDenied" $LOG_FILE || grep -q "Connection refused" $LOG_FILE; then
        echo "❌ [Reason: Database/Network] DB 연결 실패 또는 네트워크 문제입니다."
        
    elif grep -q "ClassNotFoundException" $LOG_FILE || grep -q "NoClassDefFoundError" $LOG_FILE; then
        echo "❌ [Reason: Artifact] 빌드된 JAR 파일에 문제가 있습니다 (클래스 누락)."
        
    elif grep -q "Error creating bean" $LOG_FILE; then
        echo "❌ [Reason: Config/Bean] 스프링 빈 생성 실패 (설정 오류 가능성)."
        
    else
        echo "❌ [Reason: Unknown] 로그 파일($LOG_FILE)을 직접 확인해주세요."
    fi
    echo "================================================="
    
    echo "=========== 실행 로그 (마지막 100줄) ==========="
    tail -n 100 $LOG_FILE
    echo "==========================================="
    exit 1
else
    echo "> ✅ 배포 성공 (PID: $CURRENT_PID)"
fi
