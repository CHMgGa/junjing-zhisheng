package com.morel.greenhouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GreenhouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenhouseApplication.class, args);
    }
}
