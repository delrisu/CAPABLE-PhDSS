package com.capable.physiciandss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PhysiciandssApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhysiciandssApplication.class, args);
    }

}
