package com.recipekr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class RecipekrApplication {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void runMigration() {
        try {
            jdbcTemplate.execute("ALTER TABLE recipes ADD COLUMN username VARCHAR(50)");
            System.out.println("Migration: Added username column to recipes table.");
        } catch (Exception e) {
            System.out.println("Migration: Column username likely already exists.");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(RecipekrApplication.class, args);
    }
}
