# RecipeKR

Spring Boot와 Python AI/RPA를 함께 사용하는 레시피 추천 서비스입니다.

## Docker로 실행

실행 PC에는 JDK나 Python 라이브러리를 따로 설치하지 않아도 됩니다. Docker Desktop만 설치한 뒤 프로젝트 루트에서 실행합니다.

```bat
start-recipekr.bat
```

또는 직접 실행할 수 있습니다.

```bash
docker compose up --build
```

서버가 시작되면 브라우저에서 접속합니다.

```text
http://localhost:8080
```

## Docker 없이 데모 실행

Docker Desktop 없이 UI와 주요 흐름만 확인하려면 Java 21이 설치된 PC에서 실행합니다.

```bat
start-recipekr-demo.bat
```

이 모드는 `.env`, TiDB, Gemini API 키, Python RPA 없이 H2 임시 DB와 샘플 응답으로 동작합니다.

## 실행 모드

`.env`가 없으면 Docker 로컬 실행은 `demo` 프로필로 시작합니다. 이 모드는 H2 임시 DB와 샘플 데이터를 사용하므로 TiDB, Gemini API 키, Python RPA 없이도 화면 흐름을 확인할 수 있습니다.

개발자 PC에서 실제 TiDB, AI 추천, RPA를 모두 사용하려면 `.env`에 값을 설정합니다.

AI 레시피 추천에는 Gemini API 키가 필요합니다. DB를 외부 TiDB/RDS에 연결하려면 DB 접속 정보도 `.env`에 설정합니다.

```text
SPRING_PROFILES_ACTIVE=tidb
TIDB_URL=...
TIDB_USERNAME=...
TIDB_PASSWORD=...
GEMINI_API_KEY=...
```

AWS EC2 배포에서는 GitHub Actions가 `.env`에 `SPRING_PROFILES_ACTIVE=rds`와 `APP_PORT=80`을 생성해 RDS와 80 포트를 사용합니다.

Docker 이미지는 Java 런타임, Python 패키지, Playwright Chromium을 포함하므로 AI 추천과 RPA 크롤링 기능도 컨테이너 안에서 실행됩니다.
