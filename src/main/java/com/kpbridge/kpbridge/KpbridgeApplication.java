package com.kpbridge.kpbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KpbridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KpbridgeApplication.class, args);
    }

}