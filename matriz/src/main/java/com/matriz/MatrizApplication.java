package com.matriz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MatrizApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatrizApplication.class, args);
    }
}
