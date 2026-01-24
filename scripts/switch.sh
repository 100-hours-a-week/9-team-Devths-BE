#!/usr/bin/env bash

ABSPATH=$(readlink -f $0)
ABSDIR=$(dirname $ABSPATH)
source ${ABSDIR}/profile.sh

    IDLE_PORT=$(find_idle_port)

    echo "> 전환할 Port: $IDLE_PORT"
    echo "> Port 전환"
    
    # Nginx가 바라보는 포트 변경 (service-url.inc 파일 덮어쓰기)
    # /etc/nginx/conf.d/service-url.inc에 "set $service_url http://127.0.0.1:[포트];" 형식으로 작성됨
    echo "set \$service_url http://127.0.0.1:${IDLE_PORT};" | sudo tee /etc/nginx/conf.d/service-url.inc

    echo "> Nginx Reload"
    sudo service nginx reload
