package com.rjs.fsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RjsFsmApplication {
    public static void main(String[] args) {
        SpringApplication.run(RjsFsmApplication.class, args);
    }
}
