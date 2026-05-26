#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
롯데마트 할인 상품 크롤러
=====================================
- URL: https://www.lottemart.com/display/page/event/weeklyspecial
- SALT UI 해시 클래스명 우회 → JS evaluate + 텍스트 파싱
- 상품명에서 가격 텍스트 제거, 단위 가격 오파싱 방지
"""
import logging
import re
from datetime import date

from playwright.async_api import Page, TimeoutError as PWTimeout
from utils import normalize_ingredient, extract_prices_from_text, is_ui_text, clean_product_name, dedup_items

logger = logging.getLogger(__name__)

LOTTEMART_URL = "https://www.lottemart.com/display/page/event/weeklyspecial"


async def crawl_lottemart(page: Page) -> list[dict]:
    """롯데마트 이번 주 특가 상품 크롤링."""
    results = []
    today = date.today().isoformat()

    try:
        logger.info("[롯데마트] 크롤링 시작: %s", LOTTEMART_URL)
        await page.goto(LOTTEMART_URL, wait_until="domcontentloaded", timeout=35000)
        await page.wait_for_timeout(3000)

        # 팝업 닫기
        for sel in [".btn-close", ".close-btn", "[aria-label*='닫']"]:
            try:
                for el in await page.locator(sel).all():
                    if await el.is_visible(timeout=800):
                        await el.click()
                        await page.wait_for_timeout(200)
            except Exception:
                pass

        # 더보기 클릭 (최대 2회)
        for _ in range(2):
            try:
                btn = page.locator("button:has-text('더보기')").first
                if await btn.is_visible(timeout=1500):
                    await btn.click()
                    await page.wait_for_timeout(1200)
                else:
                    break
            except Exception:
                break

        # JS로 상품 추출 (이미지+가격 있는 li)
        raw_items = await page.evaluate("""
            () => {
                const results = [];
                const seen = new Set();

                for (const li of document.querySelectorAll('li')) {
                    const text = (li.innerText || '').trim();
                    if (!text.includes('원') || text.length < 8 || text.length > 500) continue;

                    const img = li.querySelector('img[src]');
                    if (!img) continue;
                    const src = img.src || '';
                    // 아이콘/로고 제외
                    if (!src || src.includes('icon') || src.includes('logo') || src.length < 20) continue;

                    const key = text.substring(0, 50);
                    if (seen.has(key)) continue;
                    seen.add(key);

                    const link = li.querySelector('a[href]');
                    // 행사 기간 추출
                    const periodMatch = text.match(/(\\d+\\.\\d+~\\d+\\.\\d+|~\\d+\\/\\d+|\\d+\\/\\d+\\s*~\\s*\\d+\\/\\d+)/);

                    results.push({
                        text: text,
                        img_src: src,
                        href: link ? link.href : '',
                        period: periodMatch ? periodMatch[0] : null
                    });
                    if (results.length >= 60) break;
                }
                return results;
            }
        """)

        logger.info("[롯데마트] JS 추출 후보 수: %d", len(raw_items))

        for raw in raw_items:
            text = raw.get("text", "").strip()
            if not text:
                continue

            lines = [l.strip() for l in text.split('\n') if l.strip()]

            product_name = ""
            for line in lines:
                cleaned = clean_product_name(line)
                if (cleaned
                        and not is_ui_text(cleaned)
                        and len(cleaned) >= 3
                        and not re.match(r'^[\d,]+$', cleaned)
                        # 롯데마트 특이 케이스: 날짜/기간 텍스트 제외
                        and not re.match(r'^\d+\.\d+~', cleaned)):
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
                "market_name": "LOTTEMART",
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
        logger.info("[롯데마트] 크롤링 완료: %d건", len(results))

    except PWTimeout:
        logger.error("[롯데마트] 페이지 로드 타임아웃")
    except Exception as e:
        logger.error("[롯데마트] 크롤링 오류: %s", e)

    return results
