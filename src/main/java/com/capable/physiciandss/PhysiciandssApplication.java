package com.capable.physiciandss;

import com.capable.physiciandss.services.HapiRequestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PhysiciandssApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhysiciandssApplication.class, args);
        HapiRequestService hrs = new HapiRequestService();
        System.out.println(hrs.getPatientbyId("1").getName().get(0).getGiven().get(0));
    }

}
