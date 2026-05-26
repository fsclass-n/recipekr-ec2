package com.recipekr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DiscountItem - 대형마트 할인 식재료 도메인 엔티티
 * --------------------------------------------------
 * market_discount 테이블과 1:1 매핑
 * 이마트(EMART) / 롯데마트(LOTTEMART) / 홈플러스(HOMEPLUS) 3사 데이터를 단일 도메인으로 관리
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountItem {

    private Long id;

    /** 마트명: EMART | LOTTEMART | HOMEPLUS */
    private String marketName;

    /** 실제 상품명 (크롤링 원본) */
    private String productName;

    /** 정규화된 식재료명 (AI 매칭용, 예: "삼겹살", "양파") */
    private String ingredientName;

    /** 정가 (원) */
    private Integer originalPrice;

    /** 할인가 (원) */
    private Integer discountPrice;

    /** 할인율 (%) */
    private BigDecimal discountRate;

    /** 할인 기간 (예: "~5/28") */
    private String discountPeriod;

    /** 상품 이미지 URL */
    private String imageUrl;

    /** 상품 상세 페이지 URL */
    private String productUrl;

    /** 크롤링 날짜 */
    private LocalDate crawledDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** AI 모델에 전달할 식재료 문자열 표현 */
    public String toIngredientString() {
        return ingredientName != null ? ingredientName : productName;
    }

    /** 마트명 한글 표시 */
    public String getMarketDisplayName() {
        return switch (marketName) {
            case "EMART"     -> "이마트";
            case "LOTTEMART" -> "롯데마트";
            case "HOMEPLUS"  -> "홈플러스";
            default          -> marketName;
        };
    }
}
