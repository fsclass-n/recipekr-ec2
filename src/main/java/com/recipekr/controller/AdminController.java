package com.recipekr.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipekr.domain.User;
import com.recipekr.repository.DiscountItemRepository;
import com.recipekr.repository.RecipeRepository;
import com.recipekr.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    private final DiscountItemRepository discountItemRepository;
    private final RecipeRepository recipeRepository;
    private final ObjectMapper objectMapper;

    public AdminController(UserRepository userRepository, 
                           DiscountItemRepository discountItemRepository,
                           RecipeRepository recipeRepository) {
        this.userRepository = userRepository;
        this.discountItemRepository = discountItemRepository;
        this.recipeRepository = recipeRepository;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        // ADMIN role is required to view the dashboard.
        if (authentication == null || !hasAdminRole(authentication)) {
            return "redirect:/";
        }

        try {
            // 사용자 목록은 항상 로드 (파이썬 오류와 무관하게)
            try {
                model.addAttribute("users", userRepository.findAll());
            } catch (Exception ex) {
                log.warn("Failed to load user list: {}", ex.getMessage());
            }

            // DB 데이터 조회
            long userCount = userRepository.count();
            long discountCount = discountItemRepository.count();
            long recipeCount = recipeRepository.count();
            List<String> ingredientsList = recipeRepository.findAllIngredients();

            // 파이썬에 넘길 입력 데이터 준비
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("userCount", userCount);
            inputData.put("discountCount", discountCount);
            inputData.put("recipeCount", recipeCount);
            inputData.put("ingredientsList", ingredientsList);
            
            String jsonInput = objectMapper.writeValueAsString(inputData);

            // 파이썬 스크립트 실행
            String pythonScriptPath = "python-ai/admin_analytics.py";
            File scriptFile = new File(pythonScriptPath);
            
            if (!scriptFile.exists()) {
                log.error("Python script not found at: {}", scriptFile.getAbsolutePath());
                model.addAttribute("error", "분석 스크립트를 찾을 수 없습니다.");
                return "admin/dashboard";
            }

            // 시스템 파이썬 사용 (의존성 패키지는 시스템 전역으로 설치되어야 함)
            String pythonExe = "python"; 

            ProcessBuilder pb = new ProcessBuilder(pythonExe, pythonScriptPath);
            pb.directory(new File(".")); // 프로젝트 루트

            // 환경 변수 셋업: UTF-8 강제
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();

            // 표준 입력(stdin)으로 JSON 쓰기
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonInput);
                writer.flush();
            }

            // 파이썬 출력 읽기
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            // 에러 출력 읽기
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python script failed with exit code: {}", exitCode);
                log.error("Python error output: {}", errorOutput);
                model.addAttribute("error", "데이터 분석 중 오류가 발생했습니다. (Exit Code: " + exitCode + ")");
                return "admin/dashboard";
            }

            String jsonResult = output.toString();
            log.debug("Python result: {}", jsonResult);

            if (jsonResult.trim().isEmpty()) {
                throw new RuntimeException("Python script returned empty output");
            }

            // JSON 파싱 후 Model에 담기
            Map<String, Object> data = objectMapper.readValue(jsonResult, new TypeReference<Map<String, Object>>(){});
            
            if (data.containsKey("error")) {
                model.addAttribute("error", data.get("error"));
            } else {
                model.addAttribute("userCount", data.get("userCount"));
                model.addAttribute("discountCount", data.get("discountCount"));
                model.addAttribute("recipeCount", data.get("recipeCount"));
                model.addAttribute("topIngredients", data.get("topIngredients"));
                model.addAttribute("chartBase64", data.get("chartBase64"));
            }

        } catch (Exception e) {
            log.error("Admin dashboard error", e);
            model.addAttribute("error", "서버 내부 오류가 발생했습니다: " + e.getMessage());
        }

        return "admin/dashboard";
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
