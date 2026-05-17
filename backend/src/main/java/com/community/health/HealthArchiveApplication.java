package com.community.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HealthArchiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthArchiveApplication.class, args);
    }
}
