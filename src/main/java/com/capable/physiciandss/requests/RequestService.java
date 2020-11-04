package com.capable.physiciandss.requests;

import com.capable.physiciandss.model.Enactment;
import com.capable.physiciandss.utils.Constants;
import lombok.NoArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RequestService {

    RestTemplate restTemplate;

    public RequestService() {
        restTemplate = new RestTemplate();
    }

    public Enactment[] getEnactments() {

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", Constants.X_APIKEY);
        HttpEntity request = new HttpEntity(headers);

        ResponseEntity<Enactment[]> response = restTemplate.exchange(Constants.PRS_API_URL.concat("/Enactments"),
                HttpMethod.GET, request, Enactment[].class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            return null;
        }
    }


}
