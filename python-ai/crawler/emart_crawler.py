#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
이마트 할인 상품 크롤러
================================
- URL: https://emart.ssg.com/ (메인 페이지 특가 섹션)
- JS evaluate로 이미지+가격이 있는 요소 추출
- 상품명 추출 시 UI 버튼 텍스트 필터링
"""
import logging
import re
from datetime import date

from playwright.async_api import Page, TimeoutError as PWTimeout
from utils import normalize_ingredient, extract_prices_from_text, is_ui_text, clean_product_name, dedup_items

logger = logging.getLogger(__name__)

EMART_URLS = [
    "https://emart.ssg.com/plan/listPrd.ssg?mId=54&mCategoryCd=MU&dispCtgId=6000036831",
    "https://emart.ssg.com/",
]


async def crawl_emart(page: Page) -> list[dict]:
    """이마트 특가/할인 상품 크롤링."""
    results = []
    today = date.today().isoformat()

    for url in EMART_URLS:
        logger.info("[이마트] 시도 URL: %s", url)
        try:
            await page.goto(url, wait_until="domcontentloaded", timeout=35000)
            await page.wait_for_timeout(3000)

            # 팝업 닫기
            for sel in [".btn_close", ".close", "[class*='close']", "button[aria-label*='닫']"]:
                try:
                    el = page.locator(sel).first
                    if await el.is_visible(timeout=1000):
                        await el.click()
                        await page.wait_for_timeout(300)
                except Exception:
                    pass

            # JS로 상품 요소 추출 (이미지 + 가격 있는 li/article)
            raw_items = await page.evaluate("""
                () => {
                    const results = [];
                    const seen = new Set();
                    const candidates = document.querySelectorAll(
                        'li, article, [class*="cunit"], [class*="item_info"]'
                    );
                    for (const el of candidates) {
                        const text = (el.innerText || '').trim();
                        if (!text.includes('원')) continue;
                        if (text.length < 10 || text.length > 400) continue;
                        const img = el.querySelector('img[src]');
                        if (!img) continue;

                        // 상품 이미지인지 확인 (아이콘/로고 제외)
                        const src = img.src || '';
                        if (!src || src.includes('icon') || src.includes('logo') || src.includes('banner')) continue;

                        const key = text.substring(0, 60);
                        if (seen.has(key)) continue;
                        seen.add(key);

                        const link = el.querySelector('a[href*="item"]');
                        results.push({
                            text: text,
                            img_src: src,
                            href: link ? link.href : (el.querySelector('a') ? el.querySelector('a').href : '')
                        });
                        if (results.length >= 80) break;
                    }
                    return results;
                }
            """)

            logger.info("[이마트] JS 추출 후보 수: %d", len(raw_items))

            for raw in raw_items:
                text = raw.get("text", "").strip()
                if not text:
                    continue
                
                # SSG ONLY 또는 SSG온리 상품 제외 (공백 무시 매칭)
                normalized_text = re.sub(r'\s+', '', text.upper())
                if "SSGONLY" in normalized_text or "SSG온리" in normalized_text:
                    continue

                # 줄 단위로 분리 후 상품명 추출
                lines = [l.strip() for l in text.split('\n') if l.strip()]

                product_name = ""
                for line in lines:
                    cleaned = clean_product_name(line)
                    if (cleaned
                            and not is_ui_text(cleaned)
                            and len(cleaned) >= 3
                            and not re.match(r'^[\d,]+$', cleaned)):
                        product_name = cleaned
                        break

                if not product_name:
                    continue

                original_price, discount_price, discount_rate = extract_prices_from_text(text)

                if not discount_price:
                    continue

                image_url = raw.get("img_src", "")
                if image_url and image_url.startswith("//"):
                    image_url = "https:" + image_url

                product_url = raw.get("href", "")

                results.append({
                    "market_name": "EMART",
                    "product_name": product_name[:255],
                    "ingredient_name": normalize_ingredient(product_name),
                    "original_price": original_price,
                    "discount_price": discount_price,
                    "discount_rate": discount_rate,
                    "discount_period": None,
                    "image_url": image_url[:500] if image_url else None,
                    "product_url": product_url[:500] if product_url else None,
                    "crawled_date": today,
                })

            # 중복 제거
            results = dedup_items(results)

            if results:
                logger.info("[이마트] 크롤링 완료: %d건 (URL: %s)", len(results), url)
                break

        except PWTimeout:
            logger.error("[이마트] 타임아웃: %s", url)
        except Exception as e:
            logger.error("[이마트] 오류 (%s): %s", url, e)

    if not results:
        logger.warning("[이마트] 모든 URL에서 상품을 찾지 못했습니다.")

    return results
