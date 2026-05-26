# Stage 7 요약 (마이페이지 구현 및 배포 준비)

## 1. 마이페이지 (My Page) 기능 구현
* **`MypageController` 구현**: 사용자가 본인의 정보를 확인하고 관리할 수 있는 마이페이지 라우팅 추가 (`/mypage`).
* **개인화된 통계 대시보드 추가**:
  * 사용자가 추천받았던 **최근 레시피 목록** 5개 노출.
  * 사용자가 요청했던 레시피의 **총 개수** 계산.
  * 사용자가 가장 많이 찾은 **식재료 랭킹(Top 5)** 분석 및 차트용 데이터 제공.
  * **헬스 타입(다이어트, 벌크업 등) 비율** 통계 제공 기능 추가.
* **사용자 정보 관리 기능**:
  * 닉네임, 이메일, 비밀번호 등 **회원 정보 수정** 기능 (`/mypage/update`).
  * **회원 탈퇴(계정 삭제)** 및 탈퇴 시 자동 로그아웃 처리 기능 (`/mypage/delete`).
* **UI/UX 개선**: `index.html` (또는 `base.html`)의 헤더 상단에 로그인한 사용자만 볼 수 있는 **[마이페이지] 버튼** 추가.

## 2. 데이터베이스 및 리포지토리(Repository) 개선
* **`RecipekrApplication.java` 자동 마이그레이션**:
  * 기존 `recipes` 테이블에 사용자 매핑을 위한 `username` 컬럼이 없을 경우, 앱 실행 시 자동으로 컬럼을 추가하도록 마이그레이션 로직 추가.
* **`RecipeRepository` 쿼리 메서드 추가**:
  * `findByUsername(username)`: 특정 사용자의 레시피 목록 조회.
  * `countByUsername(username)`: 특정 사용자의 총 레시피 수 조회.
  * `findIngredientsByUsername(username)`: 특정 사용자의 레시피에 사용된 식재료 목록 조회.
* **`User.java` (도메인)**:
  * 정보 수정을 원활하게 할 수 있도록 Lombok `@Setter` 어노테이션 추가 적용 (컴파일 에러 해결).

## 3. 클라우드 배포(Render) 준비
* **`Dockerfile` 생성**:
  * Render 클라우드 플랫폼 배포를 위한 **멀티 스테이지 빌드(Multi-stage build)** 구성.
  * **Build Stage**: `gradle:8.5-jdk21`을 사용하여 소스 코드를 `.jar` 파일로 빌드 (테스트 과정 생략으로 속도 최적화).
  * **Run Stage**: `eclipse-temurin:21-jre-alpine`의 가벼운 런타임 환경에서 앱 실행.
  * **동적 포트 맵핑**: Render에서 할당하는 `$PORT` 환경 변수를 Spring Boot의 `server.port`로 동적 인식하도록 `ENTRYPOINT` 설정.

## 4. 트러블슈팅(Troubleshooting) 요약
* **윈도우(Windows) 파일 잠금(Lock) 에러 해결**: 자바 서버 구동 중 Java 소스 코드 저장 시 발생하는 파일 권한 엑세스 거부(Access Denied) 및 컴파일 누락 문제 해결 안내 (서버 중지 -> 저장 -> 재시작 프로세스 정립).
* **Lombok 컴파일 에러 해결**: `User` 엔티티 업데이트 시 발생한 `setNickname` 등 존재하지 않는 메서드 호출 에러 파악 및 `@Setter` 추가하여 해결.
