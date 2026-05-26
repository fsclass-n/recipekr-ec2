# 4단계: Python AI 추천 알고리즘 구현 완료 요약

## 1. 구현 개요
TF-IDF + 코사인 유사도 기반의 AI 레시피 추천 엔진을 완성하고,
Spring Boot와 Python 스크립트를 ProcessBuilder로 연동했습니다.

## 2. 생성된 파일 목록

### Python AI 모듈 (`python-ai/`)
| 파일 | 역할 |
| :--- | :--- |
| `requirements.txt` | `scikit-learn`, `pandas`, `numpy` 패키지 정의 |
| `data/recipes.csv` | 초기 레시피 학습 데이터 (10개 샘플) |
| `train.py` | TF-IDF 학습 및 `.pkl` 모델 저장 (MLOps 재학습 스크립트) |
| `predict.py` | Spring Boot가 호출하는 추천 추론 스크립트 (JSON 반환) |

### Java 백엔드
| 파일 | 역할 |
| :--- | :--- |
| `service/AiRecommendService.java` | ProcessBuilder로 Python 실행 + UTF-8 JSON 파싱 서비스 |
| `controller/RecommendController.java` | `/recipe/recommend` GET/POST 요청 처리 컨트롤러 |

### 화면 (Thymeleaf + Bootstrap)
| 파일 | 역할 |
| :--- | :--- |
| `templates/recipe/recommend.html` | 재료 입력 폼 페이지 (다크 테마) |
| `templates/recipe/result.html` | AI 추천 결과 시각화 페이지 (카드 + 매칭도 바) |

## 3. 핵심 기술 설명

### TF-IDF + 코사인 유사도
- 사용자가 입력한 재료와 건강유형을 문자열로 합쳐 TF-IDF 벡터로 변환합니다.
- 건강유형에 **3배 가중치**를 부여하여 건강 상태가 추천 결과에 강하게 반영되도록 설계했습니다.
- 전체 레시피 벡터와의 코사인 유사도를 계산하여 가장 유사한 레시피 순으로 정렬합니다.

### ProcessBuilder 한글 깨짐 방지
- `InputStreamReader`에 `StandardCharsets.UTF_8`을 명시하여 Windows 환경에서의 한글 깨짐 문제를 원천 차단했습니다.

## 4. 로컬 테스트 방법

### 1) Python 패키지 설치
```bash
cd python-ai
pip install -r requirements.txt
```

### 2) AI 모델 학습 (최초 1회 실행)
```bash
python train.py
```
> `python-ai/models/` 폴더에 `vectorizer.pkl`, `cosine_matrix.pkl`, `recipe_ids.pkl` 3개 파일이 생성됩니다.

### 3) 추천 테스트 (단독 실행)
```bash
python predict.py --ingredients "계란,양파,감자" --health_type "다이어트" --top_n 3
```

### 4) Spring Boot 서버 실행 후 브라우저 접속
```
http://localhost:8080/recipe/recommend
```
