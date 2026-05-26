# 5단계: RPA 자동화 크롤러 개발 완료 요약

## 1. 구현 개요

이마트, 롯데마트, 홈플러스 **3사 대상 Playwright 기반 RPA 크롤러**를 완성하고,
Spring Boot의 `@Scheduled`와 연동하여 **매일 새벽 1시 자동 실행**되는 파이프라인을 구축했습니다.

---

## 2. 생성/수정된 파일 목록

### Python 크롤러 (`python-ai/crawler/`)

| 파일 | 역할 |
| :--- | :--- |
| `emart_crawler.py` | 이마트 행사/특가 페이지 Playwright 크롤러 |
| `lottemart_crawler.py` | 롯데마트 이번주 특가 페이지 Playwright 크롤러 |
| `homeplus_crawler.py` | 홈플러스 이번주 특가 페이지 Playwright 크롤러 |
| `run_crawler.py` | 3사 통합 실행 + MySQL UPSERT 저장 + `--output-json` 연동 모드 |
| `__init__.py` | Python 패키지 초기화 파일 |

### DB 스키마 (`src/main/resources/sql/`)

| 파일 | 역할 |
| :--- | :--- |
| `discount_schema.sql` | `market_discount` 테이블 생성 SQL (UNIQUE KEY + INDEX 포함) |

### Java 백엔드

| 파일 | 역할 |
| :--- | :--- |
| `domain/DiscountItem.java` | 할인 식재료 도메인 엔티티 (Lombok 빌더) |
| `repository/DiscountItemRepository.java` | JdbcTemplate CRUD — UPSERT, 오늘 조회, 정리 |
| `service/DiscountCrawlerService.java` | ProcessBuilder로 Python 크롤러 실행 + AI 연동 메서드 제공 |
| `service/CrawlerScheduler.java` | `@Scheduled` 새벽 1시 자동 크롤링 / 새벽 2시 데이터 정리 |
| `RecipekrApplication.java` | `@EnableScheduling` 추가 |

### 패키지 변경

| 파일 | 변경 내용 |
| :--- | :--- |
| `python-ai/requirements.txt` | `playwright==1.44.0`, `mysql-connector-python==8.4.0` 추가 |

---

## 3. 핵심 설계 포인트

### UPSERT 중복 방지
- `market_discount` 테이블에 `UNIQUE KEY uk_market_product_date (market_name, product_name, crawled_date)` 설정
- `ON DUPLICATE KEY UPDATE`로 같은 날짜 재실행 시 가격/할인율만 갱신

### 식재료명 정규화
- 상품명("이마트 농협한우 1++ 등심 100g")에서 핵심 식재료("소고기")를 추출
- 50개 이상의 한국 주요 식재료 키워드 매핑으로 AI 추천 엔진과 연동

### 봇 탐지 우회
- `--disable-blink-features=AutomationControlled` 플래그 적용
- 한국어 로케일 (`ko-KR`) 및 일반 UserAgent 설정
- 팝업 자동 닫기 + 더보기 버튼 클릭 + 스크롤 지연 로딩 처리

### Spring ↔ Python 연동 두 가지 모드
1. **배치 모드** (`CrawlerScheduler`): Python이 직접 DB에 저장 후 종료
2. **조회 모드** (`--output-json`): DB에서 오늘 할인 재료를 읽어 JSON으로 stdout 출력 → Spring이 파싱

---

## 4. 로컬 테스트 방법

### 1) 패키지 설치
```bash
cd python-ai
pip install -r requirements.txt
playwright install --with-deps chromium
```

### 2) dry-run (DB 저장 없이 콘솔 확인)
```bash
cd python-ai/crawler
python run_crawler.py --dry-run --market emart
```

### 3) 특정 마트만 크롤링
```bash
python run_crawler.py --market lottemart
python run_crawler.py --market homeplus
```

### 4) 전체 3사 크롤링 + DB 저장
```bash
# 환경변수 설정 후 실행
set TIDB_URL=jdbc:mysql://gateway01...
set TIDB_USERNAME=...
set TIDB_PASSWORD=...
python run_crawler.py
```

### 5) 오늘의 할인 재료 JSON 출력 (Spring 연동 확인)
```bash
python run_crawler.py --output-json
```

### 6) DB 스키마 적용
```sql
-- TiDB / MySQL 클라이언트에서 실행
source src/main/resources/sql/discount_schema.sql
```
