#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
냉장고 파먹기 - 대형마트 3사 RPA 크롤러 통합 실행
=================================================
이마트(EMART) / 롯데마트(LOTTEMART) / 홈플러스(HOMEPLUS)
할인 식재료를 크롤링하여 MySQL(TiDB) market_discount 테이블에 UPSERT 저장합니다.

사용법:
  # 3사 전체 크롤링 (기본)
  python run_crawler.py

  # 특정 마트만
  python run_crawler.py --market emart
  python run_crawler.py --market lottemart
  python run_crawler.py --market homeplus

  # DB 저장 없이 콘솔 확인 (테스트)
  python run_crawler.py --dry-run
  python run_crawler.py --dry-run --market homeplus

  # Spring Boot 연동: 오늘의 할인 재료 JSON 출력
  python run_crawler.py --output-json
"""
import sys
import io
# Windows stdout UTF-8 강제
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

import argparse
import asyncio
import json
import logging
import os
from datetime import date

import mysql.connector
from playwright.async_api import async_playwright

from emart_crawler import crawl_emart
from lottemart_crawler import crawl_lottemart
from homeplus_crawler import crawl_homeplus

# ── 로깅 설정 ──────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stderr)],
)
logger = logging.getLogger("run_crawler")


# ── DB 연결 ─────────────────────────────────────────────────
def get_db_config() -> dict:
    import re
    from dotenv import load_dotenv
    load_dotenv(os.path.join(os.path.dirname(__file__), '../../.env'))
    
    # 기본적으로 RDS_URL을 우선 읽어오고 없으면 예외 처리 또는 로컬호스트
    db_url = os.environ.get("RDS_URL", os.environ.get("TIDB_URL", ""))
    
    host, port, database = "localhost", 3306, "recipekrrds"
    if db_url:
        m = re.search(r"(?:jdbc:)?mysql://([^:/]+):?(\d+)?/([^?]+)", db_url)
        if m:
            host = m.group(1)
            port = int(m.group(2)) if m.group(2) else 3306
            database = m.group(3)
            
    # JDBC URL 파라미터 분리 (예: ?useSSL=false 등)
    ssl_disabled = False
    if "useSSL=false" in db_url.lower():
        ssl_disabled = True
    elif "usessl=true" in db_url.lower():
        ssl_disabled = False

    database = database.split("?")[0]
            
    return {
        "host": host,
        "port": port,
        "database": database,
        "user": os.environ.get("RDS_USERNAME", os.environ.get("TIDB_USERNAME", "root")),
        "password": os.environ.get("RDS_PASSWORD", os.environ.get("TIDB_PASSWORD", "")),
        "ssl_disabled": ssl_disabled,
        "charset": "utf8mb4",
        "use_pure": True,
        "connection_timeout": 15,
    }


# ── DB UPSERT ────────────────────────────────────────────────
UPSERT_SQL = """
INSERT INTO market_discount (
    market_name, product_name, ingredient_name,
    original_price, discount_price, discount_rate,
    discount_period, image_url, product_url, crawled_date
) VALUES (
    %(market_name)s, %(product_name)s, %(ingredient_name)s,
    %(original_price)s, %(discount_price)s, %(discount_rate)s,
    %(discount_period)s, %(image_url)s, %(product_url)s, %(crawled_date)s
)
ON DUPLICATE KEY UPDATE
    ingredient_name  = VALUES(ingredient_name),
    original_price   = VALUES(original_price),
    discount_price   = VALUES(discount_price),
    discount_rate    = VALUES(discount_rate),
    discount_period  = VALUES(discount_period),
    image_url        = VALUES(image_url),
    product_url      = VALUES(product_url),
    updated_at       = NOW()
"""


def save_to_db(items: list[dict], market_name: str) -> int:
    if not items:
        return 0
    config = get_db_config()
    saved = 0
    try:
        conn = mysql.connector.connect(**config)
        cursor = conn.cursor()
        for item in items:
            try:
                cursor.execute(UPSERT_SQL, item)
                saved += 1
            except mysql.connector.Error as e:
                logger.warning("[DB] 저장 실패 [%s] %s: %s", market_name, item.get("product_name", ""), e)
        conn.commit()
        cursor.close()
        conn.close()
        logger.info("[DB] [%s] %d건 저장 완료", market_name, saved)
    except mysql.connector.Error as e:
        logger.error("[DB] 연결 실패: %s", e)
    return saved


def get_today_discount_ingredients() -> list[dict]:
    config = get_db_config()
    today = date.today().isoformat()
    try:
        conn = mysql.connector.connect(**config)
        cursor = conn.cursor(dictionary=True)
        cursor.execute(
            """
            SELECT market_name, product_name, ingredient_name,
                   discount_price, discount_rate, discount_period, image_url
            FROM market_discount
            WHERE crawled_date = %s
            ORDER BY market_name, discount_rate DESC
            """,
            (today,),
        )
        rows = cursor.fetchall()
        cursor.close()
        conn.close()
        return rows
    except mysql.connector.Error as e:
        logger.error("[DB] 조회 실패: %s", e)
        return []


# ── 크롤링 실행 ──────────────────────────────────────────────
MARKET_MAP = {
    "emart":     ("이마트",   crawl_emart),
    "lottemart": ("롯데마트", crawl_lottemart),
    "homeplus":  ("홈플러스", crawl_homeplus),
}


async def run_crawlers(market: str = "all", dry_run: bool = False) -> list[dict]:
    all_results = []

    targets = (
        list(MARKET_MAP.items())
        if market == "all"
        else [(market, MARKET_MAP[market])]
    )

    print(f"\n{'='*55}", flush=True)
    print(f"  대형마트 3사 RPA 크롤러 시작", flush=True)
    print(f"  대상: {', '.join(k for k, _ in targets)}", flush=True)
    print(f"  모드: {'dry-run (DB 저장 없음)' if dry_run else 'DB 저장 모드'}", flush=True)
    print(f"{'='*55}\n", flush=True)

    async with async_playwright() as pw:
        browser = await pw.chromium.launch(
            headless=True,
            args=[
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-dev-shm-usage",
                "--disable-blink-features=AutomationControlled",
                "--lang=ko-KR",
                "--disable-gpu",
                "--disable-software-rasterizer",
                "--disable-extensions",
                "--mute-audio",
                "--js-flags=--max-old-space-size=128"
            ],
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

        for key, (display_name, crawl_fn) in targets:
            print(f"[{display_name}] 크롤링 시작...", flush=True)
            try:
                market_results = await crawl_fn(page)

                if market_results:
                    print(f"[{display_name}] 수집 완료: {len(market_results)}건", flush=True)
                    # 샘플 출력 (처음 3개)
                    for item in market_results[:3]:
                        rate_str = f" ({item['discount_rate']:.0f}% 할인)" if item.get('discount_rate') else ""
                        price_str = f"{item['discount_price']:,}원" if item.get('discount_price') else "가격미상"
                        print(f"  - {item['product_name']}: {price_str}{rate_str}", flush=True)
                    if len(market_results) > 3:
                        print(f"  ... 외 {len(market_results) - 3}건", flush=True)

                    if not dry_run:
                        saved = save_to_db(market_results, display_name)
                        print(f"[{display_name}] DB 저장: {saved}건", flush=True)

                    all_results.extend(market_results)
                else:
                    print(f"[{display_name}] 수집된 상품 없음", flush=True)

            except Exception as e:
                logger.error("[%s] 실행 오류: %s", display_name, e)
                print(f"[{display_name}] 오류 발생: {e}", flush=True)

            print(flush=True)

        await browser.close()

    print(f"{'='*55}", flush=True)
    print(f"  전체 완료: 총 {len(all_results)}건 수집", flush=True)
    print(f"{'='*55}\n", flush=True)

    return all_results


def main():
    parser = argparse.ArgumentParser(description="대형마트 3사 할인 식재료 RPA 크롤러")
    parser.add_argument(
        "--market",
        choices=["all", "emart", "lottemart", "homeplus"],
        default="all",
        help="크롤링할 마트 (기본: all = 3사 전체)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="DB 저장 없이 콘솔 출력만 (테스트용)",
    )
    parser.add_argument(
        "--output-json",
        action="store_true",
        help="오늘의 DB 할인 재료 JSON 출력 (Spring Boot 연동용)",
    )
    args = parser.parse_args()

    if args.output_json:
        discount_list = get_today_discount_ingredients()
        print(json.dumps(discount_list, ensure_ascii=False, default=str))
        return

    results = asyncio.run(run_crawlers(market=args.market, dry_run=args.dry_run))

    if args.dry_run:
        print(json.dumps(results, ensure_ascii=False, indent=2, default=str))


if __name__ == "__main__":
    main()
