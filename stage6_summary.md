# Stage 6: 제미나이(Gemini) AI 연동 및 RAG 기반 추천 고도화 요약

## 1. 작업 개요
기존의 단순 단어 빈도 기반(TF-IDF) 레시피 추천 엔진을, 구글의 최신 제미나이(Gemini) 무료 티어 API를 활용한 **의미 기반(Semantic) 임베딩 검색 및 생성형 AI 챗봇(RAG) 기반**으로 전면 업그레이드했습니다.

## 2. 주요 변경 사항
- **패키지 추가**: `google-generativeai` 라이브러리를 `requirements.txt`에 추가.
- **`train.py` (임베딩 학습)**:
  - 기존 `TfidfVectorizer`를 제거하고, `models/text-embedding-004` 모델을 호출해 레시피를 768차원 벡터 행렬로 변환.
  - 무료 티어의 분당 요청 제한(Rate Limit)을 고려하여 일괄 처리 및 대기 로직 추가.
- **`predict.py` (RAG 챗봇 엔진)**:
  - 사용자 입력 검색어(재료, 건강유형)를 임베딩하여 가장 문맥이 잘 맞는 레시피 Top 3을 코사인 유사도로 추출.
  - 추출된 Top 3 레시피 정보를 컨텍스트로 삼아 제미나이 `gemini-1.5-flash` 모델에 전달.
  - 제미나이가 사용자의 "보유 재료"와 "건강 목적"을 반영하여 **창의적이고 친절한 레시피 조합 텍스트를 즉석 생성**하도록 구현.
- **GitHub Actions 연동 (`.github/workflows/ai-train.yml`)**:
  - `GEMINI_API_KEY` 시크릿(Secret) 환경변수를 주입받아 매일 자정 또는 코드 푸시 시 임베딩이 자동 학습되도록 CI/CD 파이프라인 구성.

---

## 3. 테스트 방법 (로컬 환경)

### 사전 준비 (API 키 발급)
1. [Google AI Studio](https://aistudio.google.com/)에 접속하여 **Gemini API Key**를 발급받습니다.
2. 윈도우 터미널(PowerShell 또는 Bash)에서 발급받은 키를 환경변수로 등록합니다.
   - **PowerShell**: `$env:GEMINI_API_KEY="본인의_API_키"`
   - **Git Bash / Linux**: `export GEMINI_API_KEY="본인의_API_키"`

### Step 1. 모델 임베딩 생성 (학습)
먼저 전체 레시피 데이터(`recipes.csv`)를 제미나이 임베딩으로 변환해야 합니다. 터미널에서 다음을 실행합니다.
```bash
cd python-ai
python train.py
```
- **기대 결과**: "제미나이 임베딩 생성 시작..." 로그가 뜨고 정상적으로 `models/embeddings_matrix.pkl` 파일이 생성됩니다.

### Step 2. AI 추천 및 챗봇 조언 확인 (추론)
임베딩 파일이 만들어졌다면, 가상의 재료를 입력하여 RAG 기반 챗봇이 어떻게 동작하는지 테스트합니다.
```bash
python predict.py --ingredients "닭가슴살, 양배추, 마늘" --health_type "다이어트" --top_n 3
```
- **기대 결과**: 터미널에 JSON 형태로 결과가 출력됩니다.
  - `"recommendations"`: 관련성이 높은 기존 레시피 목록 (검색 결과)
  - `"ai_message"`: 제미나이 1.5 Flash가 직접 생성한 맞춤형 조리 팁과 위트 있는 응답 (생성 결과)

---

## 4. GitHub 자동 연동 설정 방법
이제 개발자 PC가 아닌 GitHub 서버에서도 무료로 AI가 학습할 수 있도록 세팅합니다.
1. 현재 이 프로젝트가 올라가 있는 본인의 **GitHub Repository** 페이지에 들어갑니다.
2. `Settings` 탭 -> 좌측 메뉴의 `Secrets and variables` -> `Actions` 클릭
3. `New repository secret` 버튼 클릭
4. **Name**: `GEMINI_API_KEY` 입력
5. **Secret**: 구글에서 발급받은 API 키 문자열 복사 붙여넣기
6. 코드를 커밋하고 푸시(`git push`)하면 GitHub Actions의 `AI Model Training` 워크플로가 자동으로 동작하며 정상 학습되는 것을 확인할 수 있습니다.
