#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
공통 유틸리티 - 식재료 키워드 정규화 및 가격 파싱
"""
import re
from typing import Optional

# 한국 주요 식재료 키워드 (길이 역순 → 긴 키워드 먼저 매칭)
INGREDIENT_KEYWORDS = sorted([
    "삼겹살", "목살", "항정살", "갈비", "등심", "안심", "채끝", "앞다리살",
    "소고기", "닭고기", "돼지고기", "오리고기", "양고기", "닭가슴살", "닭다리",
    "연어", "고등어", "갈치", "오징어", "새우", "꽃게", "조개", "전복", "광어", "참치",
    "두부", "계란", "달걀", "우유", "치즈", "버터", "요거트",
    "양파", "감자", "당근", "마늘", "대파", "쪽파", "생강", "고추", "청양고추",
    "배추", "무", "브로콜리", "시금치", "깻잎", "상추", "양배추", "고구마",
    "사과", "배", "딸기", "포도", "바나나", "오렌지", "귤", "수박", "참외", "복숭아",
    "버섯", "느타리버섯", "표고버섯", "팽이버섯", "새송이버섯",
    "쌀", "현미", "보리", "잡곡",
    "된장", "간장", "고추장", "참기름", "들기름", "식용유", "소금", "설탕",
    "라면", "파스타", "국수", "당면",
    "김치", "어묵", "햄", "소시지",
    "아몬드", "캐슈너트", "호두",
], key=len, reverse=True)

# 상품명이 아닌 UI/버튼/배송 관련 텍스트 (필터링용)
UI_BLACKLIST = {
    "장바구니", "장바구니 담기", "담기", "찜", "좋아요", "관심상품",
    "매직배송", "매직나우", "당일배송", "새벽배송", "무료배송",
    "주간배송", "일반배송", "빠른배송", "익일배송", "택배배송",
    "더보기", "닫기", "이전", "다음", "로그인", "회원가입",
    "베스트", "신상품", "할인", "이벤트", "혜택", "쿠폰",
    "품절", "일시품절", "재입고알림",
    "오늘출발", "내일도착", "주문제작",
    # 프로모션 태그 (상품명 아님)
    "1+1", "2+1", "1 + 1", "2 + 1",
}


def is_ui_text(text: str) -> bool:
    """버튼/UI/배송 텍스트 여부 확인."""
    t = text.strip()
    return (
        t in UI_BLACKLIST
        or len(t) < 2
        or re.match(r'^[\d,]+원', t)      # 가격으로 시작
        or re.match(r'^\d+%', t)           # 퍼센트로 시작
        or re.match(r'^\d{4}-\d{2}', t)   # 날짜로 시작
        or re.search(r'배송$', t)           # ~배송으로 끝나는 단어
        or re.match(r'^\d+\.\d+~', t)     # 날짜 범위로 시작
        or re.search(r'\d+:\d+까지', t)   # 배송 시간 "14:00까지"
        or re.match(r'^오늘\(', t)          # "오늘(내)" 패턴
        or re.match(r'^내일\(', t)          # "내일(내)" 패턴
        or re.match(r'^\d+그럽', t)        # "7그럽" 같은 알 수 없는 패턴
        or len(t) > 80                     # 너무 긴 텍스트는 상품명 아님
    )


def normalize_ingredient(product_name: str) -> str:
    """상품명에서 대표 식재료명 추출. 미매칭 시 첫 2어절 반환."""
    # 먼저 가격/수량 패턴 제거
    clean = re.sub(r'\d+[Gg]당.*', '', product_name)
    clean = re.sub(r'\d+[Gg]$', '', clean)
    clean = re.split(r'[/(]', clean)[0].strip()

    for keyword in INGREDIENT_KEYWORDS:
        if keyword in clean:
            return keyword
    parts = clean.split()
    return " ".join(parts[:2]) if len(parts) >= 2 else clean or product_name


def parse_price(text: Optional[str]) -> Optional[int]:
    """'12,900원' 또는 '12,900' → 12900 정수. 범위 밖이면 None."""
    if not text:
        return None
    digits = re.sub(r"[^\d]", "", text)
    if not digits:
        return None
    val = int(digits)
    # 비현실적 가격 필터
    if val < 100 or val > 2_000_000:
        return None
    return val


def clean_product_name(name: str) -> str:
    """
    상품명에서 불필요한 가격/할인 텍스트 제거.
    예1: "아메리카노 2.1L 3,490원:"   → "아메리카노 2.1L"
    예2: "국내산 삼겹살 30% 할인"     → "국내산 삼겹살"
    예3: "4.7 / 37,316"              → ""  (별점/리뷰만 있으면 빈 문자열)
    """
    n = name.strip()
    # SSG ONLY 브랜드명 제거
    n = re.sub(r'(?i)\bSSG\s*ONLY\b', '', n).strip()
    n = re.sub(r'(?i)\bSSG\s*온리\b', '', n).strip()
    
    # 화면낭독기용 텍스트 및 원산지/브랜드 괄호 제거
    n = n.replace("할인율,", "")
    n = re.sub(r'\[.*?\]', '', n)
    n = n.replace("[원양산", "")
    n = n.replace("원양산", "")
    
    # 끝에 콜론/세미콜론 제거
    n = n.rstrip(':；;')
    # 끝에 붙은 가격 패턴 제거: " 3,490원", " 30%"
    n = re.sub(r'\s+[\d,]+원:?\s*$', '', n).strip()
    n = re.sub(r'\s+\d+%:?\s*$', '', n).strip()
    # 중간에 붙은 가격 제거: "아메리카노 2.1L 3,490원" 형태
    # (상품명 뒤에 공백+숫자,숫자원 패턴이 있으면 잘라냄)
    n = re.sub(r'\s+[\d,]{3,}원.*$', '', n).strip()
    # 별점/리뷰 패턴 제거: "4.7 / 37,316" 등
    n = re.sub(r'\s*[\d.]+\s*/\s*[\d,]+\s*$', '', n).strip()
    return n


def extract_prices_from_text(text: str):
    """
    텍스트에서 (원래가격, 할인가격, 할인율) 추출.

    제외 패턴:
    - "100G당 xxx원", "1팩당 xxx원" 등 단위 가격
    - 별점/리뷰 숫자

    반환: (original_price, discount_price, discount_rate) - 모두 None 가능
    """
    # 단위 가격 패턴 제거 (파싱 전 제거)
    clean_text = re.sub(
        r'\d+[gGmMkK][gGlL]?당\s*[\d,]+원',  # "100g당 1,110원"
        '', text
    )
    clean_text = re.sub(
        r'1[팩개박스봉]\s*당?\s*[\d,]+원',    # "1팩당 3,490원"
        '', clean_text
    )
    # 별점/리뷰 패턴 제거: "4.7/37,316" 같은 것에서 숫자가 가격으로 오파싱되는 문제 방지
    clean_text = re.sub(r'\d+\.\d+/[\d,]+', '', clean_text)

    # 모든 "xxx원" 패턴 추출
    price_matches = re.findall(r'([\d,]+)원', clean_text)
    prices = []
    for p in price_matches:
        val = parse_price(p)
        if val:
            prices.append(val)

    # 할인율 패턴: "30%" 또는 "30% 할인"
    discount_match = re.search(r'(\d+)%', clean_text)
    discount_rate = float(discount_match.group(1)) if discount_match else None

    # 할인율이 비현실적이면 무시 (90% 초과는 파싱 오류로 간주)
    if discount_rate and discount_rate > 90:
        discount_rate = None

    original_price = None
    discount_price = None

    if len(prices) == 0:
        return None, None, None

    if len(prices) == 1:
        if discount_rate:
            # 할인율만 있고 가격 1개 → 이 가격이 할인가
            discount_price = prices[0]
            raw_original = round(discount_price / (1 - discount_rate / 100))
            # 100원 단위 반올림
            original_price = round(raw_original / 100) * 100
        else:
            discount_price = prices[0]
    else:
        # 가격이 여러 개 → 최댓값이 원가, 최솟값이 할인가
        prices_sorted = sorted(prices, reverse=True)
        original_price = prices_sorted[0]
        discount_price = prices_sorted[-1]

        # 원가와 할인가가 같으면 할인 없음으로 처리
        if original_price == discount_price:
            original_price = None

        # 할인율 재계산 (텍스트에 없는 경우)
        if not discount_rate and original_price and discount_price and original_price > discount_price:
            discount_rate = round((original_price - discount_price) / original_price * 100, 2)

        # 할인율이 비현실적이면 교정:
        # SSG포인트 등 ~7% 값이 할인가로 잘못 파싱된 경우
        # original이 실제 가격, discount가 포인트 값
        if discount_rate and discount_rate > 90:
            discount_price = original_price  # 실제 할인가 = 원래 실제가 (더 큰 값)
            original_price = None
            discount_rate = None

    return original_price, discount_price, discount_rate


def dedup_items(items: list[dict]) -> list[dict]:
    """같은 상품명 + 할인가 조합의 중복 제거."""
    seen = set()
    result = []
    for item in items:
        key = (item.get("product_name", ""), item.get("discount_price"))
        if key not in seen:
            seen.add(key)
            result.append(item)
    return result
