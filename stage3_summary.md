# 3단계: 레시피 데이터베이스 연동 완료 요약

## 1. 아키텍처 변경 적용 (TiDB Cloud ↔ AWS RDS)
초기 프리티어 개발 환경의 비용 효율을 극대화하기 위해 MySQL 100% 호환 클라우드인 **TiDB Serverless**를 우선 사용하도록 구성했습니다.
이후 서비스 운영(Production) 단계 진입 시 설정만 바꿔 AWS RDS로 즉각 전환할 수 있도록 스프링 프로필(Spring Profiles) 구조를 적용했습니다.

## 1-1. 보안 강화 (GitHub Secrets 호환)
데이터베이스 비밀번호 등 민감한 정보가 GitHub 코드 저장소에 노출되지 않도록 `application-tidb.yml`과 `application-rds.yml`에 **환경 변수 주입 방식**(`${TIDB_URL:기본값}`)을 적용했습니다. 향후 CI/CD 구축 시 GitHub Secrets를 통해 안전하게 값을 주입받을 수 있습니다.

## 2. 생성 및 수정된 파일 리스트

### 설정 파일 (Profile 분리)
- `[수정]` `application.yml` : 기본 DB 세팅 제거 및 `spring.profiles.active: tidb` 설정
- `[신규]` `application-tidb.yml` : TiDB 접속을 위한 JDBC 설정 템플릿
- `[신규]` `application-rds.yml` : AWS RDS 접속을 위한 JDBC 설정 템플릿

### 데이터베이스 (SQL)
- `[신규]` `sql/recipe_schema.sql` : `recipes` 레시피 테이블 생성용 DDL 스크립트 작성

### 백엔드 (Domain & Repository & Service)
- `[신규]` `domain/Recipe.java` : 데이터 계층 간 통신을 위한 엔티티(DTO) 객체
- `[신규]` `repository/RecipeRepository.java` : JPA 없이 **JdbcTemplate**을 순수하게 활용한 조회/저장(DAO) 로직
- `[신규]` `service/RecipeService.java` : 레시피 정보 관리를 위한 서비스 계층

## 3. 다음 작업 시 필요한 조치사항
1. [TiDB Serverless 콘솔](https://tidbcloud.com)에 로그인하시어 무료 클러스터를 생성해 주세요.
2. 클러스터 생성 후 제공되는 `Connect -> JDBC` 정보를 복사하여 `application-tidb.yml` 안의 `<tidb-gateway-host>`, `<username>`, `<password>` 자리에 붙여넣기 해 주시면 DB 연동이 완료됩니다!
