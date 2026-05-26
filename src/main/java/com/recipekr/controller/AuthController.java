package com.recipekr.controller;

import com.recipekr.dto.SignupDto;
import com.recipekr.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 회원가입 / 로그인 / 로그아웃 컨트롤러
 * 로그아웃은 Spring Security가 자동 처리하므로 GET /auth/logout 매핑만 선언
 */
@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // ─────────────────────────────────────────────
    // 로그인
    // ─────────────────────────────────────────────

    /**
     * [GET] /auth/login - 로그인 페이지
     * 이미 로그인된 사용자는 메인으로 리다이렉트
     */
    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        // 이미 로그인된 경우 홈으로 리다이렉트
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "auth/login";
    }

    // ─────────────────────────────────────────────
    // 회원가입
    // ─────────────────────────────────────────────

    /**
     * [GET] /auth/signup - 회원가입 페이지
     */
    @GetMapping("/signup")
    public String signupPage(Authentication authentication, Model model) {
        // 이미 로그인된 경우 홈으로 리다이렉트
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        model.addAttribute("signupDto", new SignupDto());
        return "auth/signup";
    }

    /**
     * [POST] /auth/signup - 회원가입 처리
     */
    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute("signupDto") SignupDto signupDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        // 1. Bean Validation 오류 확인
        if (bindingResult.hasErrors()) {
            log.debug("회원가입 유효성 검사 실패: {}", bindingResult.getAllErrors());
            return "auth/signup";
        }

        // 2. 비즈니스 로직 처리 (중복 체크 등)
        try {
            userService.signup(signupDto);
            redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다! 로그인해주세요.");
            return "redirect:/auth/login";

        } catch (IllegalArgumentException e) {
            log.warn("회원가입 실패: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/signup";
        }
    }

    // ─────────────────────────────────────────────
    // 메인 홈 (임시)
    // ─────────────────────────────────────────────

    /**
     * [GET] / - 임시 홈 화면
     * 추후 7단계(프론트엔드)에서 전용 HomeController로 분리 예정
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/";
    }
}
