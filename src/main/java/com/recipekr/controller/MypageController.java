package com.recipekr.controller;

import com.recipekr.domain.Recipe;
import com.recipekr.domain.User;
import com.recipekr.repository.RecipeRepository;
import com.recipekr.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MypageController {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String mypageForm(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user == null) {
            return "redirect:/auth/login";
        }

        // 사용자 레시피 정보 조회
        List<Recipe> myRecipes = recipeRepository.findByUsername(username);
        long totalRecipes = recipeRepository.countByUsername(username);
        List<String> myIngredients = recipeRepository.findIngredientsByUsername(username);

        // 가장 많이 찾은 식재료 통계 계산 (단순 쉼표 분리 후 빈도수 측정)
        Map<String, Long> ingredientCounts = myIngredients.stream()
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        List<Map.Entry<String, Long>> topIngredients = ingredientCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .toList();

        // 헬스 타입 분포 (다이어트, 벌크업 등)
        Map<String, Long> healthTypeCounts = myRecipes.stream()
                .map(Recipe::getHealthType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(h -> h, Collectors.counting()));

        model.addAttribute("user", user);
        model.addAttribute("myRecipes", myRecipes.stream().limit(5).toList()); // 최근 5개만 노출
        model.addAttribute("totalRecipes", totalRecipes);
        model.addAttribute("topIngredients", topIngredients);
        model.addAttribute("healthTypeCounts", healthTypeCounts);

        return "mypage";
    }

    @PostMapping("/update")
    public String updateProfile(
            Authentication authentication,
            @RequestParam("nickname") String nickname,
            @RequestParam("email") String email,
            @RequestParam(value = "password", required = false) String password,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setNickname(nickname);
            user.setEmail(email);
            if (password != null && !password.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }
            userRepository.update(user);
            redirectAttributes.addFlashAttribute("successMessage", "회원 정보가 성공적으로 수정되었습니다.");
        }
        return "redirect:/mypage";
    }

    @PostMapping("/delete")
    public String deleteAccount(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            userRepository.deleteByUsername(username);

            // 강제 로그아웃 처리
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
        return "redirect:/";
    }
}