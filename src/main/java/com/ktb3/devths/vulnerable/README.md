# CodeQL 테스트용 취약점 코드 (Vulnerable Code)

**⚠️ 경고: 의도적으로 취약하게 작성된 코드입니다 ⚠️**

이 패키지는 CodeQL의 정적 분석 능력을 테스트하기 위해 **의도적으로 보안 취약점을 포함**시킨 코드들입니다.

## 절대 실제 서비스(Production)에 사용하지 마세요

이 패키지의 모든 코드는 일반적인 보안 취약점을 시연하기 위해 설계되었습니다. 실제 운영 환경에서 이 코드를 사용하는 것은 **절대로 금지**됩니다.

## 목적

이 코드는 CodeQL이 다음과 같은 다양한 보안 취약점을 탐지할 수 있는지 테스트하기 위해 작성되었습니다.

### SQL 인젝션 (SQL Injection)

* `VulnerableController.java` - API 엔드포인트의 SQL 인젝션
* `VulnerableService.java` - 서비스 레이어의 SQL 인젝션
* `VulnerableRepository.java` - 다양한 패턴의 SQL 인젝션

### 크로스 사이트 스크립팅 (XSS)

* `VulnerableController.java` - 반사형(Reflected) 및 저장형(Stored) XSS
* `VulnerableService.java` - 데이터 처리 과정의 XSS

### 커맨드 인젝션 (Command Injection)

* `VulnerableService.java` - `Runtime.exec()`를 이용한 OS 명령 실행 취약점

### 경로 조작 (Path Traversal)

* `VulnerableFileController.java` - 파일 경로 조작 취약점
* `VulnerableFileService.java` - 파일 작업 중 디렉토리 탐색 취약점

### 하드코딩된 인증 정보 (Hard-coded Credentials)

* `VulnerableService.java` - 소스 내 암호 및 API 키 포함
* `VulnerableAuthUtil.java` - 하드코딩된 비밀 정보(Secret)
* `application-vulnerable.yml` - 설정 파일 내 인증 정보 노출

### 안전하지 않은 암호화 (Insecure Cryptography)

* `VulnerableCryptoUtil.java` - 취약한 알고리즘 사용 (DES, MD5, SHA-1)
* `VulnerableAuthUtil.java` - 취약한 비밀번호 해싱 방식

### 안전하지 않은 역직렬화 (Insecure Deserialization)

* `VulnerableService.java` - 검증되지 않은 객체 역직렬화

### SSRF (Server-Side Request Forgery)

* `VulnerableService.java` - 검증되지 않은 URL 호출

### 권한 관리 미흡 (Broken Access Control)

* `VulnerableAccessController.java` - IDOR(객체 참조) 취약점
* `VulnerableAccessService.java` - 권한 검사 누락

### 보안 설정 오류 (Security Misconfiguration)

* `VulnerableSecurityConfig.java` - 취약하게 설정된 Spring Security
* `application-vulnerable.yml` - 안전하지 않은 애플리케이션 설정

### 민감 데이터 노출 (Sensitive Data Exposure)

* `VulnerableLoggingUtil.java` - 로그에 민감 정보 기록
* `VulnerableService.java` - 개인정보(PII) 노출

### 파일 업로드 취약점

* `VulnerableFileController.java` - 제한 없는 파일 업로드
* `VulnerableFileService.java` - Zip Slip, 경로 조작 등

### 오픈 리다이렉트 (Open Redirect)

* `VulnerableAccessController.java` - 검증되지 않은 리다이렉트

### 입력값 검증 미흡

* `VulnerableInputValidation.java` - ReDoS(정규표현식 서비스 거부), 불충분한 검증

### XXE (XML External Entity)

* `VulnerableService.java` - XML 파싱 시 외부 엔티티 취약점

### LDAP 인젝션

* `VulnerableService.java` - LDAP 쿼리 주입 취약점

## 파일 구조

```
vulnerable/
├── config/
│   └── VulnerableSecurityConfig.java       # 취약한 보안 설정
├── controller/
│   ├── VulnerableController.java           # 주요 취약점 API 엔드포인트
│   ├── VulnerableFileController.java       # 파일 관련 취약점
│   └── VulnerableAccessController.java     # 접근 제어 이슈
├── service/
│   ├── VulnerableService.java              # 핵심 취약 비즈니스 로직
│   ├── VulnerableFileService.java          # 파일 핸들링 취약점
│   └── VulnerableAccessService.java        # 권한 우회 로직
├── repository/
│   └── VulnerableRepository.java           # SQL 인젝션 패턴 모음
├── util/
│   ├── VulnerableAuthUtil.java             # 취약한 인증 방식
│   ├── VulnerableCryptoUtil.java           # 암호화 실패 사례
│   ├── VulnerableLoggingUtil.java          # 정보 유출 로깅
│   └── VulnerableInputValidation.java      # 입력값 검증 누락 사례
└── README.md                               # 본 파일

```

## CodeQL 테스트 방법

CodeQL의 탐지 능력을 테스트하려면 다음 단계를 따르세요:

1. 이 코드베이스에 대해 CodeQL 분석을 실행합니다.
2. CodeQL이 위에 나열된 모든 취약점을 탐지하는지 확인합니다.
3. CodeQL이 생성한 알림(Alerts) 내역을 검토합니다.
4. 탐지의 정확성(정탐)과 커버리지를 확인합니다.

## 예상 CodeQL 결과

CodeQL은 다음 항목들을 탐지해야 합니다:

* ✓ SQL 인젝션 (다수 사례)
* ✓ XSS (반사형 및 저장형)
* ✓ 커맨드 인젝션
* ✓ 경로 조작
* ✓ 하드코딩된 인증 정보
* ✓ 취약한 암호화
* ✓ 안전하지 않은 역직렬화
* ✓ SSRF
* ✓ IDOR
* ✓ 오픈 리다이렉트
* ✓ XXE
* ✓ 그 외 다수...

## 중요 참고 사항

1. **절대 배포 금지**: 이 코드는 오직 테스트 목적으로만 존재합니다.
2. **실제 비밀 정보 없음**: 포함된 모든 인증 정보는 가짜(Example) 데이터입니다.
3. **교육용 목적**: 보안 취약점에 대해 학습하는 용도로 사용하세요.
4. **CodeQL 검증**: 여러분의 보안 스캐닝 도구가 제대로 작동하는지 검증하세요.

---

**기억하세요: 실제 서비스 코드는 보안 베스트 프랙티스를 따라야 하며, 적절한 입력값 검증, 파라미터화된 쿼리, 안전한 암호화 및 올바른 접근 제어를 사용해야 합니다.**
