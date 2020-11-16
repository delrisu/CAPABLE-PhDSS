package com.capable.physiciandss.requests;

import com.capable.physiciandss.model.get.Connect;
import com.capable.physiciandss.model.get.Enactment;
import com.capable.physiciandss.model.get.ItemData;
import com.capable.physiciandss.model.get.Pathway;
import com.capable.physiciandss.utils.Constants;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    public Pathway[] getPathway() {

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", Constants.X_APIKEY);
        HttpEntity request = new HttpEntity(headers);

        ResponseEntity<Pathway[]> response = restTemplate.exchange(Constants.PRS_API_URL.concat("/Pathways"),
                HttpMethod.GET, request, Pathway[].class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            return null;
        }
    }

    public ItemData[] getData(String sessionId) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", Constants.X_APIKEY);
        headers.set("x-dresessionid", sessionId);
        HttpEntity request = new HttpEntity(headers);

        ResponseEntity<ItemData[]> response = restTemplate.exchange(Constants.DRE_API_URL.concat("/Data"),
                HttpMethod.GET, request, ItemData[].class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            return null;
        }
    }

    public Connect getConnection(String enactmentId) {


        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", Constants.X_APIKEY);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(Constants.DRE_API_URL.concat("/Connect"))
                .queryParam("enactmentid", enactmentId);
        HttpEntity request = new HttpEntity(headers);
        ResponseEntity<Connect> response = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, request, Connect.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            return null;
        }
    }


}


//        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<String, String>();
//        requestBody.add("enactmentId",enactmentId);

