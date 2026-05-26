#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
import io
# Windows stdout UTF-8 강제 설정
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')
"""
페이지 구조 진단 스크립트
- 각 마트 페이지의 HTML 스냅샷 + 스크린샷을 저장합니다.
- 이 결과를 보고 올바른 CSS 셀렉터를 찾습니다.

사용법:
  python debug_page.py
"""

import asyncio
import os
from pathlib import Path
from playwright.async_api import async_playwright

OUTPUT_DIR = Path(__file__).parent / "debug_output"
OUTPUT_DIR.mkdir(exist_ok=True)

SITES = {
    "emart": "https://emart.ssg.com/sale/salePlanList.ssg",
    "lottemart": "https://www.lottemart.com/display/page/event/weeklyspecial",
    "homeplus": "https://homeplus.co.kr/Page/EventWeekly",
}


async def dump_site(page, name: str, url: str):
    print(f"\n{'='*60}")
    print(f"[{name}] URL: {url}")
    print(f"{'='*60}")

    await page.goto(url, wait_until="domcontentloaded", timeout=40000)
    await page.wait_for_timeout(3000)

    # 스크린샷
    screenshot_path = OUTPUT_DIR / f"{name}_screenshot.png"
    await page.screenshot(path=str(screenshot_path), full_page=True)
    print(f"  [OK] screenshot: {screenshot_path}")

    # HTML 저장
    html = await page.content()
    html_path = OUTPUT_DIR / f"{name}_page.html"
    html_path.write_text(html, encoding="utf-8")
    print(f"  [OK] html saved: {html_path} ({len(html):,} bytes)")

    # 페이지 내 모든 텍스트 샘플 출력 (상품명 후보 탐색)
    print(f"\n  [텍스트 샘플 - 상품명 후보]")

    # 시도할 셀렉터 목록
    candidate_selectors = [
        "li", "article", ".item", ".product", ".goods", ".card",
        "[class*='item']", "[class*='product']", "[class*='goods']",
        "[class*='prd']", "[class*='card']", "[class*='list']",
    ]

    for sel in candidate_selectors:
        try:
            els = await page.locator(sel).all()
            if 5 <= len(els) <= 200:
                texts = []
                for el in els[:5]:
                    try:
                        t = (await el.inner_text()).strip()
                        if t and len(t) > 3:
                            texts.append(t[:80].replace('\n', ' / '))
                    except Exception:
                        pass
                if texts:
                    print(f"  셀렉터: {sel!r:35s} → {len(els)}개 발견")
                    for t in texts[:3]:
                        print(f"    텍스트 예시: {t}")
                    break
        except Exception:
            pass

    # body 전체 텍스트에서 가격 패턴이 있는 행 추출
    print(f"\n  [가격 패턴이 있는 텍스트 라인 (최대 10개)]")
    try:
        body_text = await page.locator("body").inner_text()
        lines = [l.strip() for l in body_text.split('\n') if l.strip()]
        price_lines = [l for l in lines if any(c.isdigit() for c in l) and '원' in l]
        for line in price_lines[:10]:
            print(f"    {line[:100]}")
    except Exception as e:
        print(f"    오류: {e}")


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(
            headless=True,
            args=["--no-sandbox", "--disable-setuid-sandbox", "--lang=ko-KR"],
        )
        context = await browser.new_context(
            locale="ko-KR",
            viewport={"width": 1280, "height": 900},
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/124.0.0.0 Safari/537.36"
            ),
        )
        page = await context.new_page()

        for name, url in SITES.items():
            try:
                await dump_site(page, name, url)
            except Exception as e:
                print(f"  [ERR] [{name}] error: {e}")

        await browser.close()

    print(f"\n\n완료! debug_output/ 폴더에서 HTML/스크린샷을 확인하세요.")


if __name__ == "__main__":
    asyncio.run(main())
