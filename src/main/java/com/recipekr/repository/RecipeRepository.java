package com.recipekr.repository;

import com.recipekr.domain.Recipe;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RecipeRepository {
    private final JdbcTemplate jdbcTemplate;

    public RecipeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Recipe> recipeRowMapper = (rs, rowNum) -> Recipe.builder()
            .id(rs.getLong("id"))
            .title(rs.getString("title"))
            .ingredients(rs.getString("ingredients"))
            .calories(rs.getInt("calories"))
            .healthType(rs.getString("health_type"))
            .recipeText(rs.getString("recipe_text"))
            .username(rs.getString("username"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public void save(Recipe recipe) {
        String sql = "INSERT INTO recipes (title, ingredients, calories, health_type, recipe_text, username) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, recipe.getTitle(), recipe.getIngredients(), recipe.getCalories(), recipe.getHealthType(), recipe.getRecipeText(), recipe.getUsername());
    }

    public List<Recipe> findAll() {
        String sql = "SELECT * FROM recipes ORDER BY id DESC";
        return jdbcTemplate.query(sql, recipeRowMapper);
    }

    /**
     * 총 레시피 수 조회
     * @return 레시피 수
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM recipes";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * 모든 레시피의 재료 목록만 추출
     * @return 재료 목록 리스트
     */
    public List<String> findAllIngredients() {
        String sql = "SELECT ingredients FROM recipes WHERE ingredients IS NOT NULL";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * 특정 사용자가 생성한 레시피 목록 조회
     * @param username 사용자 아이디
     * @return 레시피 목록
     */
    public List<Recipe> findByUsername(String username) {
        String sql = "SELECT * FROM recipes WHERE username = ? ORDER BY id DESC";
        return jdbcTemplate.query(sql, recipeRowMapper, username);
    }

    /**
     * 특정 사용자의 총 레시피 수 조회
     * @param username 사용자 아이디
     * @return 레시피 수
     */
    public long countByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM recipes WHERE username = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, username);
        return count != null ? count : 0L;
    }

    /**
     * 특정 사용자의 모든 레시피 재료 목록 추출
     * @param username 사용자 아이디
     * @return 재료 목록 리스트
     */
    public List<String> findIngredientsByUsername(String username) {
        String sql = "SELECT ingredients FROM recipes WHERE username = ? AND ingredients IS NOT NULL";
        return jdbcTemplate.queryForList(sql, String.class, username);
    }
}
