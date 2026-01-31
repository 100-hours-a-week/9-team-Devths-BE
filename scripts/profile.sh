#!/usr/bin/env bash

# 쉬고 있는 profile 찾기: set1이 사용 중이면 set2가 쉬고 있고, 반대면 set1이 쉬고 있음
function find_idle_profile()
{
    # curl 대신 Nginx 설정을 보고 현재 포트를 확인 (배포 중 502 에러 등 방지)
    # service-url.inc 내용 예시: set $service_url http://127.0.0.1:8080;
    
    # 1. service-url.inc 파일이 없으면 초기 배포로 간주 -> set1(8080) 배포 유도
    # (이를 위해 CURRENT=set2로 가정하면 IDLE=set1이 됨)
    if [ ! -f /etc/nginx/conf.d/service-url.inc ]; then
        CURRENT_PROFILE="set2"
    else
        # 2. 파일에서 포트 번호 추출 (8080 or 8081)
        # grep으로 숫자 포트만 추출
        CURRENT_PORT=$(grep -oE '127.0.0.1:[0-9]+' /etc/nginx/conf.d/service-url.inc | cut -d: -f2)
        
        if [ "$CURRENT_PORT" == "8080" ]; then
            CURRENT_PROFILE="set1"
        else
            CURRENT_PROFILE="set2"
        fi
    fi

    if [ "${CURRENT_PROFILE}" == "set1" ]
    then
      IDLE_PROFILE="set2"
    else
      IDLE_PROFILE="set1"
    fi

    echo "${IDLE_PROFILE}"
}

# 쉬고 있는 profile의 port 찾기
function find_idle_port()
{
    IDLE_PROFILE=$(find_idle_profile)

    if [ ${IDLE_PROFILE} == set1 ]
    then
      echo "8080"
    else
      echo "8081"
    fi
}
