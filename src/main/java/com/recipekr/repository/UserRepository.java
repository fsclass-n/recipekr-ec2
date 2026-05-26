package com.recipekr.repository;

import com.recipekr.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 데이터 접근 계층 (DAO)
 * JdbcTemplate을 사용하여 DB와 직접 통신
 */
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * ResultSet → User 객체 변환용 RowMapper
     */
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> User.builder()
            .id(rs.getLong("id"))
            .username(rs.getString("username"))
            .password(rs.getString("password"))
            .email(rs.getString("email"))
            .nickname(rs.getString("nickname"))
            .role(rs.getString("role"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    /**
     * 신규 사용자 저장 (INSERT)
     * @param user 저장할 User 도메인 객체
     * @return 생성된 DB Auto Increment ID
     */
    public Long save(User user) {
        String sql = """
                INSERT INTO users (username, password, email, nickname, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getNickname());
            ps.setString(5, user.getRole() != null ? user.getRole() : "USER");
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    /**
     * 아이디(username)로 사용자 조회
     * @param username 로그인 아이디
     * @return 해당 사용자 (Optional)
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, username);
        return users.stream().findFirst();
    }

    /**
     * 이메일로 사용자 조회
     * @param email 이메일 주소
     * @return 해당 사용자 (Optional)
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, email);
        return users.stream().findFirst();
    }

    /**
     * 아이디 중복 확인
     * @param username 확인할 아이디
     * @return 이미 존재하면 true
     */
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    /**
     * 이메일 중복 확인
     * @param email 확인할 이메일
     * @return 이미 존재하면 true
     */
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    /**
     * ID로 사용자 조회
     * @param id 사용자 ID
     * @return 해당 사용자 (Optional)
     */
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, id);
        return users.stream().findFirst();
    }

    /**
     * 총 회원 수 조회
     * @return 회원 수
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * 사용자 정보 업데이트 (닉네임, 비밀번호, 이메일)
     */
    public void update(User user) {
        String sql = "UPDATE users SET nickname = ?, password = ?, email = ?, updated_at = ? WHERE username = ?";
        jdbcTemplate.update(sql, user.getNickname(), user.getPassword(), user.getEmail(), 
                            Timestamp.valueOf(LocalDateTime.now()), user.getUsername());
    }

    /**
     * 사용자 삭제 (회원 탈퇴)
     */
    public void deleteByUsername(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        jdbcTemplate.update(sql, username);
    }
}
