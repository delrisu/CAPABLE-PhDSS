package com.capable.physiciandss;

import com.capable.physiciandss.requests.RequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PhysiciandssApplication {


    public static void main(String[] args) {
        SpringApplication.run(PhysiciandssApplication.class, args);
        System.out.println(new RequestService().getEnactments());
    }
}
