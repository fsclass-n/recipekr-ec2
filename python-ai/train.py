"""
train.py - 레시피 추천 AI 모델 학습 스크립트 (Gemini Embeddings)
-------------------------------------------------
[MLOps 역할]
  - 레시피 데이터(data/recipes.csv)를 읽어 제미나이 텍스트 임베딩을 생성합니다.
  - TF-IDF 대신 의미(Semantic) 기반의 768차원 벡터 행렬을 생성합니다.
  - 학습된 임베딩 행렬(.pkl)을 models/ 폴더에 저장합니다.
  - GitHub Actions 크론잡을 통해 주기적으로 자동 실행됩니다. (GEMINI_API_KEY 필요)

[실행 방법]
  export GEMINI_API_KEY="your_api_key_here"
  python train.py
"""

import os
import sys
import time
import pickle
import numpy as np
import pandas as pd
from pathlib import Path
import google.generativeai as genai
from dotenv import load_dotenv, find_dotenv

# ----------------------------------------------------------------
# 경로 설정
# ----------------------------------------------------------------
BASE_DIR  = Path(__file__).resolve().parent
DATA_PATH = BASE_DIR / "data" / "recipes.csv"
MODEL_DIR = BASE_DIR / "models"
MODEL_DIR.mkdir(exist_ok=True)

# 루트 또는 현재 폴더의 .env 자동 탐색 및 로드
load_dotenv(find_dotenv())

# 저장할 파일 경로
EMBEDDINGS_MATRIX_PATH = MODEL_DIR / "embeddings_matrix.pkl"
RECIPE_IDS_PATH        = MODEL_DIR / "recipe_ids.pkl"

def load_data() -> pd.DataFrame:
    """레시피 CSV 데이터를 읽어 반환합니다."""
    if not DATA_PATH.exists():
        raise FileNotFoundError(f"레시피 데이터 파일이 없습니다: {DATA_PATH}")
    df = pd.read_csv(DATA_PATH, encoding="utf-8")
    
    # 필수 컬럼 확인
    required = {"id", "title", "ingredients", "health_type"}
    if not required.issubset(df.columns):
        raise ValueError(f"CSV에 필수 컬럼이 없습니다. 필요: {required}")
    return df

def build_feature(row: pd.Series) -> str:
    """
    제미나이 임베딩에 들어갈 문맥 텍스트를 생성합니다.
    자연스럽게 요리 이름, 재료, 건강 유형을 문장 형태로 만듭니다.
    """
    title = str(row["title"]).strip()
    ingredients  = str(row["ingredients"]).strip()
    health_type  = str(row["health_type"]).strip() if pd.notna(row["health_type"]) else "일반"
    
    # 의미(Semantic) 검색을 위해 문장형으로 구성
    return f"요리명: {title}\n재료: {ingredients}\n건강 유형: {health_type}식"

def train():
    print("[train.py] Gemini 임베딩 생성 시작...")
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("❌ 에러: GEMINI_API_KEY 환경변수가 설정되지 않았습니다.")
        sys.exit(1)
        
    genai.configure(api_key=api_key)

    df = load_data()
    print(f"[train.py] 레시피 데이터 로드 완료: {len(df)}개")

    # 1) 피처 텍스트 생성
    texts = df.apply(build_feature, axis=1).tolist()

    # 2) 제미나이 API 호출하여 임베딩 생성 (무료 티어 제약 고려하여 chunk 단위 처리)
    # 2026년 기준 최신 임베딩 모델 사용
    model_name = "models/gemini-embedding-2"
    embeddings = []
    
    # 한 번에 요청할 수 있는 최대 크기에 따라 분할 처리 (예: 100개 단위)
    chunk_size = 50
    print(f"[train.py] 임베딩 API 호출 중... (총 {len(texts)}개)")
    
    for i in range(0, len(texts), chunk_size):
        chunk = texts[i:i+chunk_size]
        try:
            result = genai.embed_content(
                model=model_name,
                content=chunk,
                task_type="retrieval_document"
            )
            embeddings.extend(result['embedding'])
            print(f"  - {min(i+chunk_size, len(texts))}/{len(texts)} 처리 완료")
            time.sleep(1) # 무료 티어 Rate Limit(1500 RPM) 안정성 확보를 위해 짧은 대기
        except Exception as e:
            print(f"❌ API 호출 실패: {e}")
            sys.exit(1)

    # N x 768 차원의 NumPy 행렬로 변환
    embeddings_matrix = np.array(embeddings)
    print(f"[train.py] 임베딩 행렬 생성 완료: shape={embeddings_matrix.shape}")

    # 3) 모델 저장 (.pkl)
    with open(EMBEDDINGS_MATRIX_PATH, "wb") as f:
        pickle.dump(embeddings_matrix, f)
        
    with open(RECIPE_IDS_PATH, "wb") as f:
        pickle.dump(df[["id", "title", "ingredients", "calories",
                         "health_type", "recipe_text"]].to_dict(orient="records"), f)

    print(f"[train.py] 제미나이 임베딩 파일 저장 완료: {MODEL_DIR}")

if __name__ == "__main__":
    train()
