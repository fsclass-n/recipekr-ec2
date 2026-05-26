package com.recipekr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {
    private Long id;
    private String title;
    private String ingredients;
    private Integer calories;
    private String healthType;
    private String recipeText;
    private String username;
    private LocalDateTime createdAt;
}
