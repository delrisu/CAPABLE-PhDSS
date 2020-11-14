package com.capable.physiciandss;

import com.capable.physiciandss.requests.RequestServiceWS;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class PhysiciandssApplication {


    public static void main(String[] args) {
        SpringApplication.run(PhysiciandssApplication.class, args);
        System.out.println(Arrays.toString(new RequestServiceWS().getEnactments()));
    }
}
