#!/usr/bin/env bash

set -euo pipefail

REPOSITORY=/home/ubuntu/app
cd $REPOSITORY

echo "> 배포 시작"

# 1. image-info.env 파일에서 이미지 정보 로드
if [ ! -f "$REPOSITORY/image-info.env" ]; then
    echo "❌ [Error] image-info.env 파일을 찾을 수 없습니다."
    exit 1
fi

source $REPOSITORY/image-info.env

# 환경 변수 export (docker-compose에서 사용)
export DEVELOP
export FULL_IMAGE
export ECR_REGISTRY
export ECR_REPOSITORY
export IMAGE_TAG

echo "> ECR 이미지: $FULL_IMAGE"
echo "> 브랜치: $DEVELOP"

# 2. AWS Parameter Store에서 환경 변수 로드
echo "> AWS Parameter Store에서 환경 변수 로드"
AWS_REGION=ap-northeast-2

# 브랜치명으로 Parameter Store 경로와 Spring Profile 결정
case $DEVELOP in
  develop)
    PARAM_PATH="/Dev/BE"
    SPRING_PROFILE="dev"
    ;;
  staging)
    PARAM_PATH="/Stg/BE"
    SPRING_PROFILE="stg"
    ;;
  main|master)
    PARAM_PATH="/Prod/BE"
    SPRING_PROFILE="prod"
    ;;
  *)
    echo "⚠️  [Warning] 알 수 없는 브랜치 '$DEVELOP' - 기본값(dev) 사용"
    PARAM_PATH="/Dev/BE"
    SPRING_PROFILE="dev"
    ;;
esac

echo "> Spring Profile: $SPRING_PROFILE"

echo "> Parameter Store 경로: $PARAM_PATH"

# Parameter Store에서 파라미터 가져와서 메모리 기반 임시 .env 파일 생성
# --recursive: 하위 경로의 모든 파라미터 가져오기
# --with-decryption: SecureString 타입 파라미터 복호화
echo "> Parameter Store에서 파라미터 가져오는 중..."

# /dev/shm (메모리 기반 파일시스템)에 임시 .env 파일 생성
# 디스크에 저장되지 않고 메모리에만 존재
ENV_FILE="/dev/shm/devths-env-$$"
rm -f "$ENV_FILE"

# 파라미터 개수 카운트
PARAM_COUNT=0

# Parameter Store에서 파라미터를 가져와서 메모리상 .env 파일에 저장
while IFS=$'\t' read -r name value; do
    # 파라미터 이름에서 경로 제거하고 환경 변수 이름 추출
    # 예: /Dev/BE/DB_HOST -> DB_HOST
    env_name=${name#$PARAM_PATH/}

    # 메모리 기반 .env 파일에 저장
    echo "$env_name=$value" >> "$ENV_FILE"

    PARAM_COUNT=$((PARAM_COUNT + 1))
done < <(aws ssm get-parameters-by-path \
  --path "$PARAM_PATH" \
  --recursive \
  --with-decryption \
  --region "$AWS_REGION" \
  --query 'Parameters[*].[Name,Value]' \
  --output text)

# SPRING_PROFILES_ACTIVE도 .env 파일에 추가
echo "SPRING_PROFILES_ACTIVE=$SPRING_PROFILE" >> "$ENV_FILE"

echo "> 환경 변수 $PARAM_COUNT 개 로드 완료 (메모리 기반)"

if [ "$PARAM_COUNT" -eq 0 ]; then
    echo "❌ [Error] Parameter Store에서 파라미터를 가져오지 못했습니다."
    rm -f "$ENV_FILE"
    exit 1
fi

# 3. ECR 로그인
echo "> ECR 로그인"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"

# 4. 이미지 Pull
echo "> Docker 이미지 Pull"
docker pull "$FULL_IMAGE"

# 5. 기존 컨테이너 정리 (있다면)
echo "> 기존 컨테이너 정리"
if docker ps -a --filter "name=devths-be" --format "{{.Names}}" | grep -q "devths-be"; then
    docker stop devths-be || true
    docker rm devths-be || true
fi

# 6. docker-compose로 컨테이너 실행
echo "> Docker Compose로 애플리케이션 시작 (환경 변수 주입)"
if [ ! -f "$REPOSITORY/docker-compose.yml" ]; then
    echo "❌ [Error] docker-compose.yml 파일을 찾을 수 없습니다."
    rm -f "$ENV_FILE"
    exit 1
fi

# docker-compose 실행 (detached mode)
# 메모리 기반 .env 파일을 --env-file로 전달
cd "$REPOSITORY"

# 임시 심볼릭 링크 생성 (docker-compose는 현재 디렉토리의 .env를 자동으로 참조)
ln -sf "$ENV_FILE" "$REPOSITORY/.env"

docker compose up -d

# 컨테이너 시작 완료 후 심볼릭 링크와 메모리 파일 즉시 삭제 (보안)
echo "> 임시 환경 변수 파일 삭제"
rm -f "$REPOSITORY/.env"
rm -f "$ENV_FILE"

echo "> 배포 완료. Health Check 대기 중..."
