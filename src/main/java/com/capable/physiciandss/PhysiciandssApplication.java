package com.capable.physiciandss;

import com.capable.physiciandss.model.Connect;
import com.capable.physiciandss.requests.RequestServiceWS;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@SpringBootApplication
public class PhysiciandssApplication {


    public static void main(String[] args) {
        SpringApplication.run(PhysiciandssApplication.class, args);
        RequestServiceWS RS = new RequestServiceWS();
        RS.getEnactments().subscribe(enactment -> {
            System.out.println(Arrays.toString(enactment));
            Mono<Connect> connect = RS.getConnection(enactment[0].getId());
            connect.subscribe(con -> {
                System.out.println(con.toString());
                RS.getData(con.getDresessionid()).subscribe(data -> System.out.println(Arrays.toString(data)));
            });
        });
        RS.getPathway().subscribe(pathways -> System.out.println(Arrays.toString(pathways)));
//        System.out.println(Arrays.toString(enactments));
//        System.out.println(Arrays.toString(new RequestServiceWS().getPathway()));
//        Connect connection = new RequestServiceWS().getConnection(enactments[0].getId());
//        System.out.println(connection);
//        System.out.println(Arrays.toString(new RequestServiceWS().getData(connection.getDresessionid())));
    }
}
