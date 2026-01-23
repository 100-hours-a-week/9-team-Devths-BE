#!/usr/bin/env bash

# 쉬고 있는 profile 찾기: set1이 사용 중이면 set2가 쉬고 있고, 반대면 set1이 쉬고 있음
function find_idle_profile()
{
    # nginx 코드를 통해 현재 포트 확인 (service-url.inc 파일 활용)
    # 파일 내에 service_url http://127.0.0.1:8080; 형태로 저장되어 있다고 가정
    RESPONSE_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/profile)

    # 응답이 없거나 400 이상이면 현재 구동 중인 서비스가 없다고 판단 -> set1 배포
    # 응답이 없거나 400 이상이면 현재 구동 중인 서비스가 없다고 판단 -> set1 배포
    if [ -z "${RESPONSE_CODE}" ] || [ "${RESPONSE_CODE}" -ge 400 ]; then
        CURRENT_PROFILE="set2"
    else
        CURRENT_PROFILE=$(curl -s http://localhost/profile)
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
