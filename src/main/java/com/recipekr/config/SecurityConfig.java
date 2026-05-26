package com.recipekr.config;

import com.recipekr.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security 보안 설정
 * - 세션 기반 인증 (Form Login)
 * - BCrypt 비밀번호 암호화
 * - 접근 권한 제어
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    /**
     * BCrypt 비밀번호 인코더 빈 등록
     * strength=10: 기본값, 보안과 성능의 균형점
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * AuthenticationManager 빈 등록
     * DaoAuthenticationProvider를 통해 DB 기반 인증 수행
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    /**
     * 보안 필터 체인 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. URL 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 경로
                .requestMatchers(
                    new AntPathRequestMatcher("/"),
                    new AntPathRequestMatcher("/auth/login"),
                    new AntPathRequestMatcher("/auth/signup"),
                    new AntPathRequestMatcher("/auth/logout"),
                    new AntPathRequestMatcher("/css/**"),
                    new AntPathRequestMatcher("/js/**"),
                    new AntPathRequestMatcher("/images/**"),
                    new AntPathRequestMatcher("/favicon.svg"),
                    new AntPathRequestMatcher("/favicon.png"),
                    new AntPathRequestMatcher("/favicon.ico"),
                    new AntPathRequestMatcher("/error")
                ).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasRole("ADMIN")
                // 그 외 모든 요청은 로그인 필요 (레시피 추천 포함)
                .anyRequest().authenticated()
            )

            // 2. 폼 로그인 설정
            .formLogin(form -> form
                .loginPage("/auth/login")              // 커스텀 로그인 페이지
                .loginProcessingUrl("/auth/login")     // 로그인 처리 URL (POST)
                .usernameParameter("username")          // 폼의 username 필드명
                .passwordParameter("password")          // 폼의 password 필드명
                .defaultSuccessUrl("/", true)           // 로그인 성공 후 이동
                .failureUrl("/auth/login?error=true")   // 로그인 실패 후 이동
                .permitAll()
            )

            // 3. 로그아웃 설정
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout", "GET"))
                .logoutSuccessUrl("/auth/login?logout=true")  // 로그아웃 성공 후 이동
                .invalidateHttpSession(true)                   // 세션 무효화
                .deleteCookies("JSESSIONID")                   // 세션 쿠키 삭제
                .permitAll()
            )

            // 4. 세션 관리 설정
            .sessionManagement(session -> session
                .maximumSessions(1)                    // 동일 계정 중복 로그인 1개만 허용
                .maxSessionsPreventsLogin(false)       // 새 로그인 시 기존 세션 만료
            )

            // 5. CSRF 설정 (개발 편의상 특정 경로 제외 가능, 현재는 기본값 활성화)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(new AntPathRequestMatcher("/api/**"))    // REST API 경로는 CSRF 제외
            );

        return http.build();
    }

    /**
     * 정적 리소스에 대해 보안 필터를 거치지 않도록 설정
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
            new AntPathRequestMatcher("/css/**"),
            new AntPathRequestMatcher("/js/**"),
            new AntPathRequestMatcher("/images/**"),
            new AntPathRequestMatcher("/favicon.svg"),
            new AntPathRequestMatcher("/favicon.png"),
            new AntPathRequestMatcher("/favicon.ico")
        );
    }
}
