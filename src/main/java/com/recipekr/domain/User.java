package com.recipekr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자(회원) 도메인 엔티티
 * JdbcTemplate 기반으로 사용하므로 JPA 어노테이션 없이 순수 Java 클래스로 구성
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    /** 로그인 아이디 (중복 불가) */
    private String username;

    /** BCrypt로 암호화된 비밀번호 */
    private String password;

    /** 이메일 주소 */
    private String email;

    /** 화면 표시용 닉네임 */
    private String nickname;

    /** 권한: USER, ADMIN */
    private String role;

    /** 가입 일시 */
    private LocalDateTime createdAt;

    /** 수정 일시 */
    private LocalDateTime updatedAt;
}
