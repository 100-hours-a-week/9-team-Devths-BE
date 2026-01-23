#!/bin/bash

# BeforeInstall: Nginx 설정 복사 및 Maintenance 모드 활성화
# CodeDeploy가 파일을 배포하기 전에 실행됩니다.

set -e

APP_DIR=/home/ubuntu/app/be
NGINX_SITES_AVAILABLE=/etc/nginx/sites-available
NGINX_SITES_ENABLED=/etc/nginx/sites-enabled
MAINTENANCE_HTML=/var/www/html/maintenance.html

echo "==== [BeforeInstall] 시작 ===="

# 배포 환경 설정 로드 (이전 배포에서 생성된 파일)
if [ -f "$APP_DIR/deploy_env.sh" ]; then
  echo "🔧 이전 배포 환경 설정 로드 중..."
  source "$APP_DIR/deploy_env.sh"
  echo "  ✅ BRANCH_NAME: $BRANCH_NAME"
else
  echo "⚠️  deploy_env.sh 파일을 찾을 수 없습니다. 환경 변수로 확인합니다."
fi

BRANCH_NAME="${BRANCH_NAME:-develop}"

echo "📍 배포 대상 브랜치: $BRANCH_NAME"

# Maintenance HTML 디렉토리 생성
if [ ! -d "/var/www/html" ]; then
  echo "📁 /var/www/html 디렉토리 생성"
  sudo mkdir -p /var/www/html
fi

# Maintenance 모드 활성화 (develop은 제외)
if [ "$BRANCH_NAME" != "develop" ]; then
  echo "🚧 Maintenance 모드 활성화 중..."

  # Maintenance HTML 파일 복사 (이전 배포의 파일 사용, 없으면 건너뜀)
  if [ -f "$APP_DIR/nginx/maintenance.html" ]; then
    sudo cp "$APP_DIR/nginx/maintenance.html" "$MAINTENANCE_HTML"
  else
    echo "⚠️  maintenance.html 파일을 찾을 수 없습니다. 첫 배포일 수 있습니다."
    echo "📍 Maintenance 모드를 건너뜁니다."
    echo "==== [BeforeInstall] 완료 ===="
    exit 0
  fi

  # 브랜치별 Nginx 설정 파일 결정
  if [ "$BRANCH_NAME" = "main" ]; then
    NGINX_CONFIG="prod-api"
    SERVER_NAME="api.devths.com"
  elif [ "$BRANCH_NAME" = "release" ]; then
    NGINX_CONFIG="staging-api"
    SERVER_NAME="staging.api.devths.com"
  fi

  # Maintenance 전용 임시 설정 생성
  sudo tee "$NGINX_SITES_AVAILABLE/${NGINX_CONFIG}-maintenance" > /dev/null <<EOF
server {
    listen 443 ssl;
    server_name $SERVER_NAME;

    ssl_certificate /etc/letsencrypt/live/$(echo $SERVER_NAME | sed 's/api\.//')/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$(echo $SERVER_NAME | sed 's/api\.//')/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    root /var/www/html;
    index maintenance.html;

    location / {
        try_files \$uri /maintenance.html;
    }
}

server {
    listen 80;
    server_name $SERVER_NAME;
    return 301 https://\$host\$request_uri;
}
EOF

  # 기존 설정 백업 및 Maintenance 설정 활성화
  if [ -L "$NGINX_SITES_ENABLED/$NGINX_CONFIG" ]; then
    sudo rm "$NGINX_SITES_ENABLED/$NGINX_CONFIG"
  fi
  sudo ln -sf "$NGINX_SITES_AVAILABLE/${NGINX_CONFIG}-maintenance" "$NGINX_SITES_ENABLED/${NGINX_CONFIG}-maintenance"

  # Nginx 설정 테스트 및 reload
  if sudo nginx -t; then
    sudo nginx -s reload
    echo "✅ Maintenance 모드가 활성화되었습니다."
  else
    echo "❌ Nginx 설정 테스트 실패"
    exit 1
  fi
else
  echo "📍 개발 환경: Maintenance 모드를 건너뜁니다."
fi

echo "==== [BeforeInstall] 완료 ===="
