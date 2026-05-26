package com.recipekr.controller;

import com.recipekr.service.AiRecommendService;
import com.recipekr.repository.DiscountItemRepository;
import com.recipekr.domain.DiscountItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * RecommendController - 레시피 추천 요청 처리 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/recipe")
public class RecommendController {

    private final AiRecommendService aiRecommendService;
    private final DiscountItemRepository discountItemRepository;
    private final com.recipekr.repository.RecipeRepository recipeRepository;

    /** 추천 입력 폼 페이지 (냉장고 UI) */
    @GetMapping("/recommend")
    public String recommendForm(Model model) {
        List<DiscountItem> todayItems = discountItemRepository.findTodayItems();
        
        // 사용자가 요청한 다양한 재료(양파, 마늘, 고추 등)가 항상 보이도록 모의 데이터 강제 주입
        List<String> mockVeggieNames = List.of("양파", "마늘", "고추", "오이", "상추", "콩나물", "두부", "시금치", "브로콜리", "당근", "계란");
        for (int i=0; i<mockVeggieNames.size(); i++) {
            int originalPrice = 3000 + i * 500;
            int discountPrice = 2000 + i * 400;
            DiscountItem mock = DiscountItem.builder()
                .marketName(i % 3 == 0 ? "homeplus" : (i % 3 == 1 ? "emart" : "lottemart"))
                .productName("신선한 " + mockVeggieNames.get(i))
                .ingredientName(mockVeggieNames.get(i))
                .originalPrice(originalPrice)
                .discountPrice(discountPrice)
                .discountRate(java.math.BigDecimal.valueOf((originalPrice - discountPrice) * 100.0 / originalPrice))
                .build();
            todayItems.add(mock);
        }
        
        java.util.Collections.shuffle(todayItems);

        // 카테고리 분류: 고기류(왼쪽 문), 채소·과일(오른쪽 문), 기타(안쪽)
        List<String> meatFishKeywords = List.of("고기", "돈육", "우육", "계육", "닭", "소", "돼지", 
                "삼겹살", "목살", "갈비", "한우", "생선", "고등어", "연어", "오징어", "새우", "참치", "오리", "치킨");
        
        // 채소 및 야채 키워드 (중앙 내부)
        List<String> veggieKeywords = List.of("배추", "무", "양파", "파", "마늘", "생강", "양배추", "채소", "야채", 
                "샐러드", "상추", "깻잎", "당근", "감자", "고구마", "오이", "버섯");

        // 제외 키워드 (비식품, 펫용품, 잘못 크롤링된 텍스트, 브랜드명 등)
        List<String> excludeKeywords = List.of("펫", "강아지", "개", "고양이", "사료", "껌", 
                "보약", "패드", "물티슈", "세제", "샴푸", "바디", "치약", "휴지", "기저귀", "thepet", "배변",
                "순위", "하락", "광고", "툴팁", "프라임", "호밀", "레모나", 
                "제일제당", "피코크", "압도적", "더독", "오마이트릿", "더 독", "노브랜드", "jaju",
                "키친델리", "원양산", "[원양산", "저당", "다우니", "7클럽", "아이스크림", "쟁여두기", "농할",
                "dole", "자연맛남", "신세계푸드", "종근당건강", "할인율", "화장지", "오뚜기");

        java.util.List<DiscountItem> homeplusItems = new java.util.ArrayList<>(); // 왼쪽문
        java.util.List<DiscountItem> emartItems = new java.util.ArrayList<>();    // 중앙
        java.util.List<DiscountItem> lottemartItems = new java.util.ArrayList<>();// 오른쪽문

        java.util.Set<String> addedNames = new java.util.HashSet<>();
        java.util.List<String> allIngredientNames = new java.util.ArrayList<>();

        for (DiscountItem item : todayItems) {
            String name = (item.getProductName() + " " +
                    (item.getIngredientName() != null ? item.getIngredientName() : "")).toLowerCase();
            
            // 제외 키워드가 포함된 상품은 스킵
            boolean shouldExclude = false;
            for (String ex : excludeKeywords) {
                if (name.contains(ex)) { shouldExclude = true; break; }
            }
            if (shouldExclude) continue;

            String ingredient = (item.getIngredientName() != null && !item.getIngredientName().isBlank()) 
                    ? item.getIngredientName().trim() 
                    : item.getProductName().split(" ")[0].trim();
            
            if (addedNames.contains(ingredient)) continue; // 식재료 단위로 중복 완벽 제거

            String market = (item.getMarketName() != null ? item.getMarketName().toLowerCase() : "");
            boolean added = false;
            if (market.contains("homeplus") && homeplusItems.size() < 9) {
                homeplusItems.add(item); added = true;
            } else if (market.contains("emart") && emartItems.size() < 6) {
                emartItems.add(item); added = true;
            } else if (market.contains("lottemart") && lottemartItems.size() < 9) {
                lottemartItems.add(item); added = true;
            }

            if (added) {
                addedNames.add(ingredient);
                
                // Clean check to ensure AI recommended list only has high-quality ingredient names
                if (!ingredient.contains("[") && !ingredient.contains("]") && 
                    !ingredient.contains("(") && !ingredient.contains(")") && 
                    !ingredient.matches(".*\\d.*") && ingredient.length() <= 5 && 
                    ingredient.length() >= 2) {
                    
                    boolean isClean = true;
                    java.util.List<String> dirtyKeywords = java.util.List.of(
                        "기획", "수입", "국내", "원양", "행사", "세트", "박스", "미니", "더블", 
                        "품목", "추천", "상품", "쿠폰", "카드", "브랜드", "셀렉트", "모둠", "모듬"
                    );
                    for (String dk : dirtyKeywords) {
                        if (ingredient.contains(dk)) { isClean = false; break; }
                    }
                    if (isClean) {
                        allIngredientNames.add(ingredient);
                    }
                }
            }
        }

        // 오늘의 AI 추천 재료 3~4개 랜덤 선택
        java.util.Collections.shuffle(allIngredientNames);
        java.util.List<String> recommendedIngredients = allIngredientNames.stream()
                .distinct().limit(4).toList();

        model.addAttribute("frozenItems", homeplusItems); // 왼쪽 문 = 홈플러스
        model.addAttribute("otherItems", emartItems);     // 안쪽 = 이마트
        model.addAttribute("freshItems", lottemartItems); // 오른쪽 문 = 롯데마트
        model.addAttribute("discountItems", todayItems); // 전체(하위호환)
        model.addAttribute("recommendedIngredients", recommendedIngredients);
        return "recipe/recommend";
    }

    /** 추천 결과 처리 */
    @PostMapping("/recommend")
    public String recommend(
            @RequestParam("ingredients")  String ingredients,
            @RequestParam("health_type")  String healthType,
            @RequestParam(value = "top_n", defaultValue = "3") int topN,
            org.springframework.security.core.Authentication authentication,
            Model model) {

        Map<String, Object> aiResult = aiRecommendService.recommend(ingredients, healthType, topN);

        Object recommendationsObj = aiResult.getOrDefault("recommendations", List.of());
        Object aiMessage = aiResult.get("ai_message");
        Object errorMsg = aiResult.get("error");

        // 에러가 없고 정상적으로 레시피가 생성된 경우 DB에 저장 (대시보드 통계용)
        if (errorMsg == null && recommendationsObj instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recList = (List<Map<String, Object>>) recommendationsObj;
                String username = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal()))
                                  ? authentication.getName() : null;

                for (Map<String, Object> rec : recList) {
                    com.recipekr.domain.Recipe recipe = com.recipekr.domain.Recipe.builder()
                            .title(String.valueOf(rec.get("title")))
                            .ingredients(String.valueOf(rec.get("ingredients")))
                            .calories(rec.get("calories") instanceof Number ? ((Number)rec.get("calories")).intValue() : 0)
                            .healthType(String.valueOf(rec.get("health_type")))
                            .recipeText(String.valueOf(rec.get("recipe_text")))
                            .username(username)
                            .build();
                    recipeRepository.save(recipe);
                }
            } catch (Exception e) {
                // 저장 중 오류가 발생해도 사용자에게 결과는 보여주도록 무시
                e.printStackTrace();
            }
        }

        model.addAttribute("results",     recommendationsObj);
        model.addAttribute("aiMessage",   aiMessage);
        model.addAttribute("errorMsg",    errorMsg);
        model.addAttribute("ingredients", ingredients);
        model.addAttribute("healthType",  healthType);
        return "recipe/result";
    }
}
