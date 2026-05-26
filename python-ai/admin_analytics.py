import sys
import json
import base64
import io
import warnings
import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import CountVectorizer
import matplotlib.pyplot as plt
import matplotlib.font_manager as fm

# Suppress warnings
warnings.filterwarnings('ignore')

def main():
    try:
        # Spring Boot에서 표준 입력(stdin)으로 JSON 데이터를 전달합니다.
        input_data = sys.stdin.read()
        if not input_data:
            print(json.dumps({"error": "No input data provided."}))
            sys.exit(1)
            
        data = json.loads(input_data)
        
        user_count = data.get("userCount", 0)
        discount_count = data.get("discountCount", 0)
        recipe_count = data.get("recipeCount", 0)
        ingredients_list = data.get("ingredientsList", [])

        # 재료 데이터 텍스트 마이닝 (Scikit-Learn CountVectorizer)
        top_ingredients = []
        chart_base64 = ""
        
        if len(ingredients_list) > 0:
            ingredients_series = pd.Series(ingredients_list).dropna()
            
            if not ingredients_series.empty:
                # 텍스트 전처리 및 벡터화
                vectorizer = CountVectorizer(token_pattern=r'(?u)\b\w+\b') # 한글 단어 추출
                X = vectorizer.fit_transform(ingredients_series)
                
                # 빈도수 합산
                word_counts = np.array(X.sum(axis=0)).flatten()
                words = vectorizer.get_feature_names_out()
                
                # 데이터 프레임 생성 후 상위 10개 추출
                word_freq = pd.DataFrame({'ingredient': words, 'count': word_counts})
                word_freq = word_freq.sort_values(by='count', ascending=False).head(10)
                
                top_ingredients = word_freq.to_dict(orient='records')
                
                # Matplotlib 시각화 설정 (다크 테마)
                plt.style.use('dark_background')
                import platform
                if platform.system() == 'Windows':
                    plt.rcParams['font.family'] = 'Malgun Gothic' # 윈도우 한글 폰트
                else:
                    plt.rcParams['font.family'] = 'NanumGothic' # 리눅스(Render) 한글 폰트

                plt.rcParams['axes.unicode_minus'] = False
                
                fig, ax = plt.subplots(figsize=(10, 5))
                
                # 역순 정렬 (가장 큰 값이 위에 오도록)
                plot_data = word_freq.sort_values(by='count', ascending=True)
                
                bars = ax.barh(plot_data['ingredient'], plot_data['count'], color='#ff6b6b', alpha=0.8)
                ax.set_title('Top 10 레시피 요청 재료', fontsize=16, pad=20, color='white')
                ax.set_xlabel('요청 횟수', fontsize=12, color='white')
                
                # 테두리 제거 및 그리드 설정
                ax.spines['top'].set_visible(False)
                ax.spines['right'].set_visible(False)
                ax.spines['left'].set_color('#555555')
                ax.spines['bottom'].set_color('#555555')
                ax.grid(axis='x', linestyle='--', alpha=0.3)
                
                # 수치 텍스트 추가
                for bar in bars:
                    width = bar.get_width()
                    ax.text(width + 0.1, bar.get_y() + bar.get_height()/2, f'{int(width)}', 
                            ha='left', va='center', color='white', fontsize=10)
                
                plt.tight_layout()
                
                # 이미지를 BytesIO에 저장하고 Base64로 인코딩
                buf = io.BytesIO()
                fig.savefig(buf, format='png', transparent=True)
                buf.seek(0)
                chart_base64 = base64.b64encode(buf.read()).decode('utf-8')
                plt.close(fig)

        # 결과 JSON 생성
        result = {
            "userCount": user_count,
            "discountCount": discount_count,
            "recipeCount": recipe_count,
            "topIngredients": top_ingredients,
            "chartBase64": chart_base64
        }
        
        # 표준 출력으로 JSON 전송 (Spring Boot에서 읽음)
        print(json.dumps(result))

    except Exception as e:
        print(json.dumps({"error": f"데이터 분석 중 오류 발생: {str(e)}"}))

if __name__ == "__main__":
    main()
