package com.capable.physiciandss;

import com.capable.physiciandss.model.Connect;
import com.capable.physiciandss.model.Enactment;
import com.capable.physiciandss.requests.RequestServiceWS;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class PhysiciandssApplication {


    public static void main(String[] args) {
        SpringApplication.run(PhysiciandssApplication.class, args);
        Enactment[] enactments = new RequestServiceWS().getEnactments();
        System.out.println(Arrays.toString(enactments));
        System.out.println(Arrays.toString(new RequestServiceWS().getPathway()));
        Connect connection = new RequestServiceWS().getConnection(enactments[0].getId());
        System.out.println(connection);
        System.out.println(Arrays.toString(new RequestServiceWS().getData(connection.getDresessionid())));
    }
}
