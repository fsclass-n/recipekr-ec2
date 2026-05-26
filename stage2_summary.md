# 2단계: 회원가입 및 로그인 구현 완료 요약

본 문서는 냉장고 파먹기 AI 레시피 추천 시스템의 2단계 작업 완료 결과를 요약합니다.

## 1. 개요
- **목표**: Spring Boot 기반 애플리케이션 초기화 및 세션 기반 회원가입/로그인 기능 구현
- **기술 스택**: Java 21, Spring Boot 3.3.5, Spring Security, JdbcTemplate, MySQL, Thymeleaf, Bootstrap 5

## 2. 생성된 파일 및 디렉토리 구조
```text
recipekr/
├── build.gradle                        # Gradle 빌드 설정 (Spring Boot 3.3.5, Java 21)
├── settings.gradle                     # 프로젝트 이름 설정
├── gradlew / gradlew.bat               # Gradle Wrapper 실행 스크립트
├── gradle/wrapper/
│   └── gradle-wrapper.properties       # Gradle 8.8 설정
│
├── src/main/java/com/recipekr/
│   ├── RecipekrApplication.java        # Spring Boot 메인 클래스
│   ├── domain/
│   │   └── User.java                   # 사용자 엔티티 (Lombok Builder, 순수 자바 객체)
│   ├── dto/
│   │   ├── SignupDto.java              # 회원가입 폼 DTO (Bean Validation 포함)
│   │   └── LoginDto.java               # 로그인 폼 DTO
│   ├── repository/
│   │   └── UserRepository.java         # JdbcTemplate 기반 CRUD, RowMapper, 중복 체크 로직
│   ├── service/
│   │   ├── UserService.java            # 회원가입 비즈니스 로직 및 BCrypt 비밀번호 암호화
│   │   └── CustomUserDetailsService.java # Spring Security 인증 처리를 위한 UserDetailsService 구현체
│   ├── controller/
│   │   ├── AuthController.java         # 로그인/회원가입 라우팅 및 처리 (GET/POST)
│   │   └── HomeController.java         # 메인 홈 페이지 렌더링
│   └── config/
│       └── SecurityConfig.java         # Spring Security 보안 설정 (접근 권한, 세션, Form 로그인)
│
├── src/main/resources/
│   ├── application.yml                 # DB 연결 정보, Thymeleaf, 세션 타임아웃 설정
│   ├── sql/schema.sql                  # users 테이블 생성 DDL 및 테스트용 admin 계정 초기화 쿼리
│   ├── templates/
│   │   ├── layout/
│   │   │   └── base.html               # 전체 페이지 공통 레이아웃 (Navbar, Footer 포함)
│   │   ├── index.html                  # 메인 홈 페이지 (히어로 섹션, 서비스 특징 소개)
│   │   └── auth/
│   │       ├── login.html              # 로그인 페이지 (2패널 레이아웃)
│   │       └── signup.html             # 회원가입 페이지 (실시간 비밀번호 강도 측정 및 일치 확인)
│   └── static/css/
│       ├── global.css                  # 다크 테마 기반 공통 디자인 시스템 (CSS 변수 활용)
│       └── auth.css                    # 인증 페이지 전용 애니메이션 및 스타일
```

## 3. 핵심 구현 내용

### 3.1. 보안 및 인증 (Spring Security)
- **비밀번호 암호화**: `BCryptPasswordEncoder(strength=10)`를 사용하여 단방향 해시 처리
- **로그인 인증**: `CustomUserDetailsService`를 통해 DB에서 회원 정보를 조회하여 Spring Security Form Login과 연동
- **세션 제어**: 동일 계정 중복 로그인 최대 1개 제한, 타임아웃 30분 설정, 로그아웃 시 `JSESSIONID` 쿠키 삭제 및 세션 무효화
- **접근 권한 제어**: 메인 홈(`/`), 로그인/회원가입(`/auth/**`), 정적 리소스는 누구나 접근 가능(`permitAll`), 그 외는 인증된 사용자만 접근 가능

### 3.2. 데이터베이스 (JdbcTemplate)
- JPA 없이 **JdbcTemplate**을 사용하여 가볍고 빠른 데이터 접근 계층 구현
- **중복 체크**: 이메일 및 아이디 중복 확인 기능
- `schema.sql`을 통해 `users` 테이블 생성 및 `username`, `email` 인덱스 생성
- 기본 관리자 계정(`admin` / `Admin1234!`) 자동 생성 스크립트 포함

### 3.3. 프론트엔드 (Thymeleaf + Bootstrap)
- **공통 레이아웃(`base.html`)**: Spring Security `sec:authorize` 태그를 활용해 로그인 상태에 따른 Navbar 동적 렌더링
- **입력 검증**: 회원가입 시 Spring Boot의 `@Valid`와 Bean Validation을 활용하여 백엔드 검증을 수행하고, 화면(Thymeleaf)에 즉각적인 에러 메시지 노출
- **UX 개선**:
  - 비밀번호 표시/숨기기 토글 버튼
  - 실시간 비밀번호 강도 측정 프로그레스 바
  - 실시간 비밀번호 일치 여부 확인
  - 제출 시 버튼 로딩 상태(Spinner) 표시 방지 중복 제출

## 4. 로컬 환경 실행 가이드

1. **데이터베이스 구성**
   - MySQL에 접속하여 `schema.sql`의 스크립트를 실행해 `recipekr` 데이터베이스와 `users` 테이블을 생성합니다.
   - `application.yml` 파일에서 `spring.datasource.password` 값을 실제 로컬 MySQL 비밀번호로 변경합니다.

2. **애플리케이션 실행**
   - 터미널에서 아래 명령어를 실행하여 서버를 가동합니다:
     ```bash
     .\gradlew.bat bootRun
     ```
   - 브라우저에서 `http://localhost:8080`에 접속하여 정상 동작을 확인합니다.
