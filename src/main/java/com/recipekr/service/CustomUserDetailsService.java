package com.recipekr.service;

import com.recipekr.domain.User;
import com.recipekr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security 연동을 위한 UserDetailsService 구현체
 * 로그인 시 Spring Security가 이 서비스를 호출하여 사용자 정보를 조회하고 인증을 처리함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security 로그인 처리 시 호출됨
     * username으로 DB에서 사용자를 조회하여 UserDetails 반환
     *
     * @param username 로그인 폼에서 입력한 아이디
     * @return Spring Security UserDetails 객체
     * @throws UsernameNotFoundException 사용자가 없을 경우
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("로그인 시도 - username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자 로그인 시도 - username: {}", username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });

        // CustomUserDetails 객체 반환
        return new CustomUserDetails(user);
    }
}
