package com.recipekr.controller;

import com.recipekr.domain.User;
import com.recipekr.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 홈(메인) 페이지 컨트롤러
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;

    /**
     * [GET] / - 메인 페이지
     * 로그인된 사용자의 닉네임을 모델에 담아 렌더링
     */
    @GetMapping("/")
    public String home(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        if (userDetails != null) {
            User user = userService.findUserByUsername(userDetails.getUsername());
            model.addAttribute("user", user);
        }
        return "index";
    }
}
