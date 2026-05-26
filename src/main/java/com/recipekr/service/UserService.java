package com.recipekr.service;

import com.recipekr.domain.User;
import com.recipekr.dto.SignupDto;
import com.recipekr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 사용자 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 처리
     * - 아이디/이메일 중복 체크
     * - 비밀번호 BCrypt 단방향 암호화
     * - DB 저장
     *
     * @param signupDto 회원가입 폼 데이터
     * @throws IllegalArgumentException 중복 아이디/이메일이거나 비밀번호 불일치 시
     */
    public void signup(SignupDto signupDto) {
        // 1. 비밀번호 일치 확인
        if (!signupDto.getPassword().equals(signupDto.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 2. 아이디 중복 확인
        if (userRepository.existsByUsername(signupDto.getUsername())) {
            throw new IllegalArgumentException("이미 사용중인 아이디입니다.");
        }

        // 3. 이메일 중복 확인
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }

        // 4. 비밀번호 BCrypt 암호화 (단방향 해시)
        String encodedPassword = passwordEncoder.encode(signupDto.getPassword());

        // 5. User 엔티티 생성 및 저장
        User newUser = User.builder()
                .username(signupDto.getUsername())
                .password(encodedPassword)
                .email(signupDto.getEmail())
                .nickname(signupDto.getNickname())
                .role("USER")
                .build();

        Long savedId = userRepository.save(newUser);
        log.info("새 회원 가입 완료 - username: {}, id: {}", signupDto.getUsername(), savedId);
    }

    /**
     * 로그인 처리 (세션 방식 - Spring Security 위임)
     * Spring Security의 UserDetailsService를 사용하므로 별도 구현 불필요.
     * 여기서는 추가적인 사용자 정보 조회 유틸 메서드만 제공.
     *
     * @param username 로그인 아이디
     * @return 사용자 도메인 객체
     */
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}
