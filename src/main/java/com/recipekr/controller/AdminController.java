package com.recipekr.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipekr.domain.User;
import com.recipekr.repository.DiscountItemRepository;
import com.recipekr.repository.RecipeRepository;
import com.recipekr.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    private final DiscountItemRepository discountItemRepository;
    private final RecipeRepository recipeRepository;
    private final ObjectMapper objectMapper;
    private final String activeProfile;

    public AdminController(UserRepository userRepository, 
                           DiscountItemRepository discountItemRepository,
                           RecipeRepository recipeRepository,
                           @Value("${spring.profiles.active:rds}") String activeProfile) {
        this.userRepository = userRepository;
        this.discountItemRepository = discountItemRepository;
        this.recipeRepository = recipeRepository;
        this.objectMapper = new ObjectMapper();
        this.activeProfile = activeProfile;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        // ADMIN role is required to view the dashboard.
        if (authentication == null || !hasAdminRole(authentication)) {
            return "redirect:/";
        }

        // 사용자 목록은 항상 로드 (오류와 무관하게)
        try {
            model.addAttribute("users", userRepository.findAll());
        } catch (Exception ex) {
            log.warn("Failed to load user list: {}", ex.getMessage());
        }

        long userCount = 0;
        long discountCount = 0;
        long recipeCount = 0;
        List<String> ingredientsList = null;

        // DB 데이터 조회
        try {
            userCount = userRepository.count();
            discountCount = discountItemRepository.count();
            recipeCount = recipeRepository.count();
            ingredientsList = recipeRepository.findAllIngredients();
        } catch (Exception ex) {
            log.error("Failed to load DB stats", ex);
        }

        // 기본 수치 및 자바 백업 데이터 바인딩
        model.addAttribute("userCount", userCount);
        model.addAttribute("discountCount", discountCount);
        model.addAttribute("recipeCount", recipeCount);

        List<Map<String, Object>> topIngredients = analyzeIngredientsInJava(ingredientsList);
        model.addAttribute("topIngredients", topIngredients);
        model.addAttribute("chartBase64", ""); // 기본값은 빈 값

        // 데모 모드가 아니고 RDS/운영 환경일 경우, 파이썬 scikit-learn 분석 시도
        if (!"demo".equals(activeProfile)) {
            try {
                Map<String, Object> inputData = new HashMap<>();
                inputData.put("userCount", userCount);
                inputData.put("discountCount", discountCount);
                inputData.put("recipeCount", recipeCount);
                inputData.put("ingredientsList", ingredientsList);
                
                String jsonInput = objectMapper.writeValueAsString(inputData);

                String pythonScriptPath = "python-ai/admin_analytics.py";
                File scriptFile = new File(pythonScriptPath);
                
                if (scriptFile.exists()) {
                    String pythonExe = "python"; 
                    ProcessBuilder pb = new ProcessBuilder(pythonExe, pythonScriptPath);
                    pb.directory(new File("."));

                    Map<String, String> env = pb.environment();
                    env.put("PYTHONIOENCODING", "utf-8");

                    Process process = pb.start();

                    try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                        writer.write(jsonInput);
                        writer.flush();
                    }

                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line);
                        }
                    }

                    StringBuilder errorOutput = new StringBuilder();
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorOutput.append(line);
                        }
                    }

                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        String jsonResult = output.toString();
                        if (!jsonResult.trim().isEmpty()) {
                            Map<String, Object> data = objectMapper.readValue(jsonResult, new TypeReference<Map<String, Object>>(){});
                            if (!data.containsKey("error")) {
                                // 파이썬 Scikit-Learn 분석 성공 시 데이터 덮어쓰기
                                model.addAttribute("topIngredients", data.get("topIngredients"));
                                model.addAttribute("chartBase64", data.get("chartBase64"));
                            }
                        }
                    } else {
                        log.warn("Python analytics script exited with code {}. Using Java fallback.", exitCode);
                        log.warn("Python error output: {}", errorOutput);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to run python Scikit-Learn analytics. Using Java fallback.", e);
            }
        }

        return "admin/dashboard";
    }

    private List<Map<String, Object>> analyzeIngredientsInJava(List<String> ingredientsList) {
        Map<String, Integer> freqMap = new HashMap<>();
        if (ingredientsList != null) {
            for (String ingredients : ingredientsList) {
                if (ingredients == null) continue;
                String[] tokens = ingredients.split("[,\\s\\(\\)\\[\\]\\{\\}\\.\\-\\_\\+]+");
                for (String token : tokens) {
                    String clean = token.trim();
                    if (clean.length() >= 1) {
                        freqMap.put(clean, freqMap.getOrDefault(clean, 0) + 1);
                    }
                }
            }
        }
        return freqMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("ingredient", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /**
     * [GET] /admin/users - 사용자 목록 콘솔
     */
    @GetMapping("/users")
    public String userList(Authentication authentication, Model model) {
        if (authentication == null || !hasAdminRole(authentication)) {
            return "redirect:/";
        }
        try {
            List<User> users = userRepository.findAll();
            model.addAttribute("users", users);
        } catch (Exception e) {
            log.error("User list error", e);
            model.addAttribute("error", "사용자 목록 조회 중 오류: " + e.getMessage());
        }
        return "admin/dashboard";
    }

    /**
     * [POST] /admin/users/{id}/role - 사용자 role 변경
     */
    @PostMapping("/users/{id}/role")
    public String updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !hasAdminRole(authentication)) {
            return "redirect:/";
        }
        try {
            if (!role.equals("USER") && !role.equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("error", "유효하지 않은 권한입니다.");
                return "redirect:/admin/dashboard";
            }
            userRepository.updateRole(id, role);
            log.info("User role updated - id: {}, role: {}", id, role);
            redirectAttributes.addFlashAttribute("successMessage", "사용자 권한이 " + role + "로 변경되었습니다.");
        } catch (Exception e) {
            log.error("Role update error", e);
            redirectAttributes.addFlashAttribute("error", "권한 변경 실패: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
