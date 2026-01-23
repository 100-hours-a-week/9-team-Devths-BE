#!/bin/bash

# AfterInstall: Nginx 설정 파일 복사
# CodeDeploy가 파일 배포를 완료한 후 실행됩니다.

set -e

APP_DIR=/home/ubuntu/app/be
NGINX_SITES_AVAILABLE=/etc/nginx/sites-available
NGINX_SITES_ENABLED=/etc/nginx/sites-enabled

echo "==== [AfterInstall] Nginx 설정 파일 복사 시작 ===="

# 배포 환경 설정 로드
if [ -f "$APP_DIR/deploy_env.sh" ]; then
  echo "🔧 배포 환경 설정 로드 중..."
  source "$APP_DIR/deploy_env.sh"
  echo "  ✅ BRANCH_NAME: $BRANCH_NAME"
else
  echo "⚠️  deploy_env.sh 파일을 찾을 수 없습니다. 기본값(develop)을 사용합니다."
fi

BRANCH_NAME="${BRANCH_NAME:-develop}"

echo "📍 배포 대상 브랜치: $BRANCH_NAME"

# 브랜치별 Nginx 설정 파일 복사
if [ "$BRANCH_NAME" = "develop" ]; then
  echo "📝 개발 환경 Nginx 설정 복사 중..."

  # dev-api-simple 파일 복사
  sudo cp "$APP_DIR/nginx/dev-api-simple" "$NGINX_SITES_AVAILABLE/dev-api"

  # 심볼릭 링크 생성 (기존 링크가 있으면 삭제)
  if [ -L "$NGINX_SITES_ENABLED/dev-api" ]; then
    sudo rm "$NGINX_SITES_ENABLED/dev-api"
  fi
  sudo ln -sf "$NGINX_SITES_AVAILABLE/dev-api" "$NGINX_SITES_ENABLED/dev-api"

  echo "✅ dev-api 설정 파일 복사 완료"

elif [ "$BRANCH_NAME" = "release" ]; then
  echo "📝 스테이징 환경 Nginx 설정 복사 중..."

  # staging-api-bluegreen 파일 복사
  sudo cp "$APP_DIR/nginx/staging-api-bluegreen" "$NGINX_SITES_AVAILABLE/staging-api"
  sudo cp "$APP_DIR/nginx/service-url.inc" /etc/nginx/conf.d/service-url.inc

  echo "✅ staging-api 설정 파일 복사 완료"

elif [ "$BRANCH_NAME" = "main" ]; then
  echo "📝 운영 환경 Nginx 설정 복사 중..."

  # prod-api-bluegreen 파일 복사
  sudo cp "$APP_DIR/nginx/prod-api-bluegreen" "$NGINX_SITES_AVAILABLE/prod-api"
  sudo cp "$APP_DIR/nginx/service-url.inc" /etc/nginx/conf.d/service-url.inc

  echo "✅ prod-api 설정 파일 복사 완료"
fi

# maintenance.html 파일 복사
echo "📝 maintenance.html 파일 복사 중..."
if [ ! -d "/var/www/html" ]; then
  sudo mkdir -p /var/www/html
fi
sudo cp "$APP_DIR/nginx/maintenance.html" /var/www/html/maintenance.html

echo "✅ maintenance.html 파일 복사 완료"

# Nginx 설정 테스트
echo "🧪 Nginx 설정 테스트 중..."
if sudo nginx -t; then
  echo "✅ Nginx 설정 테스트 통과"
else
  echo "❌ Nginx 설정 테스트 실패"
  exit 1
fi

echo "==== [AfterInstall] 완료 ===="
