#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
홈플러스 할인 상품 크롤러 (재작성)
=====================================
- URL: https://homeplus.co.kr/Page/EventWeekly
- 방식: article 42개 발견 확인됨. inner_text() 파싱으로 전환.
  텍스트 형식: "상품명 / 분류 / ... / 정가원 / N%할인가원 / ..."
"""
import logging
import re
from datetime import date

from playwright.async_api import Page, TimeoutError as PWTimeout
from utils import normalize_ingredient, extract_prices_from_text, is_ui_text, clean_product_name, dedup_items

logger = logging.getLogger(__name__)

HOMEPLUS_URLS = [
    "https://homeplus.co.kr/Page/EventWeekly",
    "https://homeplus.co.kr/exhibit?promoNo=20311",  # 신상품/특가 대안
]


async def crawl_homeplus(page: Page) -> list[dict]:
    """
    홈플러스 이번 주 특가 상품 크롤링.
    article 요소의 inner_text()를 '/' 기준으로 파싱합니다.
    실제 확인: article 요소 42개, 텍스트에 상품명/가격/할인율 포함.
    """
    results = []
    today = date.today().isoformat()

    for url in HOMEPLUS_URLS:
        logger.info("[홈플러스] 시도 URL: %s", url)
        try:
            await page.goto(url, wait_until="domcontentloaded", timeout=40000)
            await page.wait_for_timeout(3000)

            # 팝업 닫기
            for sel in [".close", ".btn-close", "[aria-label*='닫']", "[class*='popup'] button"]:
                try:
                    for el in await page.locator(sel).all():
                        if await el.is_visible(timeout=1000):
                            await el.click()
                            await page.wait_for_timeout(200)
                except Exception:
                    pass

            # 스크롤로 지연 로딩 유발
            await page.evaluate("window.scrollTo(0, document.body.scrollHeight / 2)")
            await page.wait_for_timeout(1500)
            await page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
            await page.wait_for_timeout(1500)

            # JS로 article 요소 + 가격 있는 요소 추출
            raw_items = await page.evaluate("""
                () => {
                    const results = [];
                    const seen = new Set();

                    // article 우선 탐색 (홈플러스 상품 컨테이너)
                    let containers = Array.from(document.querySelectorAll('article'));

                    // article이 없으면 li로 폴백
                    if (containers.length === 0) {
                        containers = Array.from(document.querySelectorAll('li'));
                    }

                    for (const el of containers) {
                        const text = (el.innerText || '').trim();
                        if (!text.includes('원') || text.length < 5 || text.length > 800) continue;

                        const key = text.substring(0, 60);
                        if (seen.has(key)) continue;
                        seen.add(key);

                        const img = el.querySelector('img');
                        const link = el.querySelector('a[href]');
                        const imgSrc = img ? (img.src || img.dataset.src || img.dataset.lazy || '') : '';

                        // 행사 기간 추출
                        const periodMatch = text.match(/~?\\d+\\/\\d+|\\d+\\.\\d+~\\d+\\.\\d+/);

                        results.push({
                            text: text,
                            img_src: imgSrc,
                            href: link ? link.href : '',
                            period: periodMatch ? periodMatch[0] : null
                        });

                        if (results.length >= 80) break;
                    }
                    return results;
                }
            """)

            logger.info("[홈플러스] JS 추출 후보 수: %d", len(raw_items))

            for raw in raw_items:
                text = raw.get("text", "").strip()
                if not text:
                    continue

                # 홈플러스 텍스트 형식:
                # "상품명 / 분류 / 마일리지 / ... / 9,990원 / 30%6,990원 / 100G당 2,796원"
                # '/' 로 분리 후 첫 번째 유효 파트를 상품명으로
                parts = [p.strip() for p in text.split('/') if p.strip()]

                product_name = ""
                for part in parts:
                    candidate = clean_product_name(part.split('\n')[0].strip())
                    if candidate and not is_ui_text(candidate) and len(candidate) >= 3:
                        product_name = candidate
                        break

                if not product_name:
                    # 줄 기반 폴백
                    for line in text.split('\n'):
                        candidate = clean_product_name(line.strip())
                        if candidate and not is_ui_text(candidate) and len(candidate) >= 3:
                            product_name = candidate
                            break

                if not product_name or len(product_name) < 2:
                    continue

                original_price, discount_price, discount_rate = extract_prices_from_text(text)

                if not discount_price:
                    continue

                image_url = raw.get("img_src", "")
                if image_url and image_url.startswith("//"):
                    image_url = "https:" + image_url

                product_url = raw.get("href", "")

                results.append({
                    "market_name": "HOMEPLUS",
                    "product_name": product_name[:255],
                    "ingredient_name": normalize_ingredient(product_name),
                    "original_price": original_price,
                    "discount_price": discount_price,
                    "discount_rate": discount_rate,
                    "discount_period": raw.get("period"),
                    "image_url": image_url[:500] if image_url else None,
                    "product_url": product_url[:500] if product_url else None,
                    "crawled_date": today,
                })

            results = dedup_items(results)
            if results:
                logger.info("[홈플러스] 크롤링 완료: %d건 (URL: %s)", len(results), url)
                break

        except PWTimeout:
            logger.error("[홈플러스] 타임아웃: %s", url)
        except Exception as e:
            logger.error("[홈플러스] 오류 (%s): %s", url, e)

    if not results:
        logger.warning("[홈플러스] 모든 URL에서 상품을 찾지 못했습니다.")

    return results
