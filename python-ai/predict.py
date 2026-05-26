import time
"""
predict.py - Spring Boot에서 호출하는 AI 레시피 창작 스크립트 (Gemini Generative API)
---------------------------------------------------------
[사용 방법]
  export GEMINI_API_KEY="your_api_key_here"
  python predict.py --ingredients "계란,양파,감자" --health_type "다이어트" --top_n 3

[출력]
  JSON 형태로 표준 출력(stdout)으로 반환합니다.
  {
    "recommendations": [ ... 창작된 3가지 레시피 JSON ... ],
    "ai_message": "..."
  }
"""

import sys
import json
import argparse
import os
import google.generativeai as genai
from dotenv import load_dotenv, find_dotenv
import warnings

# 경고 메시지 억제 (Spring Boot에서 stdout으로 JSON 파싱할 때 방해되지 않도록)
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
warnings.filterwarnings('ignore')

# 루트 또는 현재 폴더의 .env 자동 탐색 및 로드
load_dotenv(find_dotenv())

def generate_recipes(ingredients: str, health_type: str, top_n: int) -> dict:
    """
    제미나이 2.5 Flash 모델을 사용하여, 사용자가 선택한 재료와 건강 목표에 맞는
    완전히 새로운 맞춤형 레시피를 창작하여 JSON으로 반환합니다.
    """
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise ValueError("GEMINI_API_KEY 환경 변수가 없습니다.")
        
    genai.configure(api_key=api_key)
    
    # JSON 스키마를 강제하기 위해 generation_config 사용
    prompt = f"""
당신은 최고의 창의적인 요리사입니다. 사용자가 선택한 다음 재료를 **주재료로 반드시 활용하여** {top_n}가지의 새롭고 맛있는 레시피를 창작해 주세요.

- 필수 주재료: {ingredients}
- 건강 목적: {health_type}

다음 JSON 스키마를 정확히 지켜서 출력하세요:
{{
  "recommendations": [
    {{
      "rank": 1부터 순위 번호 (정수),
      "id": 0,
      "title": "창의적인 요리 이름",
      "ingredients": "사용한 전체 재료 목록 (필수 재료 포함)",
      "calories": 예상 칼로리 (정수),
      "health_type": "{health_type}",
      "recipe_text": "간결한 요리 순서 (1. ... 2. ...)",
      "score": 요리의 매칭도(0.90~0.99 사이의 소수)
    }}
  ],
  "ai_message": "전체 요리에 대한 셰프의 친절하고 센스있는 한 줄 조언 평 (마크다운 없이 평문)"
}}
"""
    # JSON 스키마를 강제하기 위해 generation_config 사용
    models_to_try = ["gemini-2.5-flash", "gemini-1.5-flash", "gemini-pro"]
    last_error = None
    
    for model_name in models_to_try:
        try:
            model = genai.GenerativeModel(model_name)
            max_retries = 3
            for attempt in range(max_retries):
                try:
                    response = model.generate_content(
                        prompt,
                        generation_config=genai.GenerationConfig(
                            response_mime_type="application/json"
                        )
                    )
                    # JSON 파싱
                    result_json = json.loads(response.text)
                    return result_json
                except Exception as inner_e:
                    error_msg = str(inner_e)
                    if "429" in error_msg or "Quota" in error_msg:
                        if attempt < max_retries - 1:
                            time.sleep(15)  # 15초 대기 후 재시도
                            continue
                    raise inner_e
        except Exception as e:
            last_error = e
            print(f"Model {model_name} failed: {e}", file=sys.stderr)
            continue
            
    raise RuntimeError(f"모든 AI 모델 호출에 실패했습니다. 마지막 오류: {str(last_error)}")

def main():
    parser = argparse.ArgumentParser(description="제미나이 기반 맞춤형 레시피 창작 엔진")
    parser.add_argument("--ingredients",  type=str, required=True,
                        help="쉼표로 구분된 재료 (예: 계란,양파,감자)")
    parser.add_argument("--health_type",  type=str, default="일반",
                        help="건강 유형 (다이어트|당뇨|저염식|일반)")
    parser.add_argument("--top_n",        type=int, default=3,
                        help="추천 결과 개수 (기본값: 3)")
    args = parser.parse_args()

    results = generate_recipes(
        ingredients=args.ingredients,
        health_type=args.health_type,
        top_n=args.top_n,
    )
    
    # ✅ Spring Boot가 읽을 수 있도록 JSON 형태로 stdout 출력
    print(json.dumps(results, ensure_ascii=False))

if __name__ == "__main__":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    try:
        main()
    except Exception as e:
        # ❌ 에러 발생 시에도 JSON 형태로 출력
        print(json.dumps({"error": str(e)}, ensure_ascii=False))
        sys.exit(1)
