# Stage 10 Summary: 실행 방식 정리 및 데모 모드 추가

## 목표

RecipeKR을 여러 환경에서 안전하게 실행할 수 있도록 실행 방식을 정리했다.

- 개발자 PC: `.env`와 VSCode `launch.json`으로 실제 TiDB, Gemini, RPA 기능 사용
- 일반 로컬 PC: Docker Desktop만 설치하면 JDK/Python 설치 없이 실행
- Docker 없는 PC: Java 21만 있으면 임시 데이터 기반 데모 실행
- Render 서버: TiDB 환경변수로 서버 배포
- AWS EC2: GitHub Secrets 기반 `.env` 생성 후 RDS로 배포

## 주요 변경 내역

### 1. 보안 설정 정리

`application-tidb.yml`에 있던 실제 TiDB 접속 정보를 제거했다.

현재는 실제 키 없이 환경변수만 참조한다.

```yml
url: ${TIDB_URL:jdbc:mysql://localhost:4000/recipekr?useSSL=false&serverTimezone=Asia/Seoul}
username: ${TIDB_USERNAME:root}
password: ${TIDB_PASSWORD:password}
```

`launch.json`에서도 실제 TiDB 값을 제거하고 `.env`를 읽도록 변경했다.

```json
"envFile": "${workspaceFolder}/.env"
```

실제 보안 키는 다음 위치에서만 관리한다.

- 로컬 개발: `.env`
- Render: Render 환경변수
- AWS EC2: GitHub Secrets에서 배포 시 `.env` 생성

### 2. Docker 실행 방식 정리

`start-recipekr.bat`는 이제 Java/Gradle을 직접 실행하지 않고 Docker Compose로 실행한다.

Docker 이미지 안에는 다음이 포함된다.

- Java 21 런타임
- Python 가상환경
- `python-ai/requirements.txt` 패키지
- Playwright Chromium
- Spring Boot JAR

따라서 Docker 실행에서는 로컬 PC에 JDK, Python, pip 패키지, Playwright를 따로 설치하지 않아도 된다.

### 3. Docker 로컬 기본값을 데모 모드로 변경

`.env`가 없는 로컬 Docker 실행은 기본적으로 `demo` 프로필로 시작한다.

```yml
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-demo}
```

이 경우 실제 TiDB, Gemini, RPA를 호출하지 않고 H2 임시 DB와 샘플 응답을 사용한다.

실제 개발 모드로 실행하려면 `.env`에 아래처럼 설정한다.

```text
SPRING_PROFILES_ACTIVE=tidb
TIDB_URL=...
TIDB_USERNAME=...
TIDB_PASSWORD=...
GEMINI_API_KEY=...
```

### 4. 데모 프로필 추가

새 파일을 추가했다.

- `src/main/resources/application-demo.yml`
- `src/main/resources/sql/demo_schema.sql`
- `src/main/resources/sql/demo_data.sql`

데모 모드는 H2 인메모리 DB를 사용한다.

포함된 샘플:

- 데모 사용자
- 관리자 사용자
- 샘플 레시피
- 샘플 할인 재료

데모 로그인 계정:

```text
ID: demo
PW: Admin1234!
```

### 5. AI/RPA 데모 응답 처리

`demo` 프로필에서는 외부 기능을 실제 호출하지 않는다.

- `AiRecommendService`: Gemini/Python 호출 대신 샘플 레시피 추천 반환
- `GeminiChatService`: Gemini API 호출 대신 샘플 챗봇 응답 반환
- `DiscountCrawlerService`: 실제 Playwright RPA 크롤링 없이 성공 처리

이 덕분에 `.env`, TiDB, Gemini API 키, Python/RPA가 없어도 주요 화면 흐름을 확인할 수 있다.

### 6. Docker 없는 데모 실행 파일 추가

`start-recipekr-demo.bat`를 추가했다.

이 파일은 Docker 없이 `demo` 프로필로 서버를 실행한다.

필요 조건:

- Java 21 설치

필요하지 않은 것:

- Docker Desktop
- `.env`
- TiDB 접속 정보
- Gemini API 키
- Python 라이브러리
- Playwright

## 실행 방법

### 방법 1. 개발자 PC에서 VSCode 디버그 실행

개발자 PC에서는 `.env` 파일을 준비한다.

```text
SPRING_PROFILES_ACTIVE=tidb
TIDB_URL=...
TIDB_USERNAME=...
TIDB_PASSWORD=...
GEMINI_API_KEY=...
```

VSCode에서 `Spring Boot-RecipekrApplication<recipekr>` 디버그 구성을 실행한다.

이 방식은 `launch.json`이 `.env`를 읽어 실제 TiDB, Gemini, RPA 환경으로 실행한다.

### 방법 2. Docker Desktop으로 로컬 실행

Docker Desktop을 설치하고 실행한 뒤 프로젝트 루트에서 아래 파일을 실행한다.

```bat
start-recipekr.bat
```

또는 직접 실행한다.

```bat
docker compose up --build
```

접속 주소:

```text
http://localhost:8080
```

`.env`가 없으면 자동으로 데모 모드로 실행된다.

`.env`에 `SPRING_PROFILES_ACTIVE=tidb`와 TiDB/Gemini 값을 넣으면 실제 기능으로 실행된다.

### 방법 3. Docker 없이 데모 실행

Java 21이 설치되어 있다면 Docker 없이 데모 모드로 실행할 수 있다.

```bat
start-recipekr-demo.bat
```

접속 주소:

```text
http://localhost:8080
```

데모 로그인:

```text
ID: demo
PW: Admin1234!
```

이 방식은 H2 임시 DB를 사용하므로 서버를 재시작하면 초기 샘플 데이터로 돌아간다.

### 방법 4. Render 서버 + TiDB

Render에서는 Dockerfile로 서버를 빌드하고 환경변수를 설정한다.

필수 환경변수:

```text
SPRING_PROFILES_ACTIVE=tidb
TIDB_URL=...
TIDB_USERNAME=...
TIDB_PASSWORD=...
GEMINI_API_KEY=...
```

사용자는 Render 웹 주소로 접속하므로 로컬 설치가 필요 없다.

### 방법 5. AWS EC2 + RDS

GitHub Actions가 Docker 이미지를 빌드해 Docker Hub에 올리고, EC2에서 `docker-compose.yml`을 실행한다.

EC2 배포 시 GitHub Secrets 값으로 `.env`가 자동 생성된다.

```text
SPRING_PROFILES_ACTIVE=rds
APP_PORT=80
RDS_URL=...
RDS_USERNAME=...
RDS_PASSWORD=...
GEMINI_API_KEY=...
```

EC2에서는 기존처럼 80 포트로 서비스된다.

## 실행 방식별 기능 비교

| 실행 방식 | DB | AI 추천 | RPA | 설치 필요 |
| --- | --- | --- | --- | --- |
| VSCode 개발 실행 | TiDB | 실제 Gemini/Python | 실제 Playwright | JDK, Python 환경 |
| Docker + `.env` 있음 | TiDB 또는 RDS | 실제 Gemini/Python | 실제 Playwright | Docker Desktop |
| Docker + `.env` 없음 | H2 데모 DB | 샘플 응답 | 샘플 성공 처리 | Docker Desktop |
| Docker 없는 데모 | H2 데모 DB | 샘플 응답 | 샘플 성공 처리 | Java 21 |
| Render + TiDB | TiDB | 실제 Gemini/Python | 실제 Playwright | 사용자 설치 없음 |
| EC2 + RDS | RDS | 실제 Gemini/Python | 실제 Playwright | 사용자 설치 없음 |

## 검증 결과

다음 항목을 확인했다.

- `.\gradlew.bat test --console=plain` 성공
- `demo` 프로필로 서버 부팅 성공
- `http://localhost:18080` 요청에 HTTP 200 응답 확인
- `git diff --check` 통과

## 주의 사항

아무것도 설치되지 않은 PC에서 로컬 서버를 직접 실행하는 것은 불가능하다. 최소한 Docker Desktop 또는 Java 21 같은 실행 환경은 필요하다.

완전 무설치 사용자에게 제공하려면 Render 또는 EC2에 배포한 뒤 웹 주소로 접속하게 해야 한다.

이미 Git에 실제 TiDB 또는 API 키가 커밋된 적이 있다면, 파일에서 제거했더라도 Git 히스토리에 남아 있을 수 있으므로 해당 키는 새로 발급하거나 비밀번호를 교체해야 한다.
