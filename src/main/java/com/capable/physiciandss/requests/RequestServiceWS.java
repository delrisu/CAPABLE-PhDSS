package com.capable.physiciandss.requests;


import com.capable.physiciandss.configuration.WebClientConfig;
import com.capable.physiciandss.model.Enactment;
import com.capable.physiciandss.utils.Constants;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class RequestServiceWS {

    WebClient webClient;

    public RequestServiceWS() {
        ApplicationContext context = new AnnotationConfigApplicationContext(WebClientConfig.class);
        webClient = context.getBean("webClient", WebClient.class);
    }

    public Enactment[] getEnactments() {
        return webClient.get()
                .uri(Constants.PRS_API_URL + "/Enactments")
                .header("x-apikey", Constants.X_APIKEY)
                .retrieve()
                .bodyToMono(Enactment[].class).log().block();
    }
}
