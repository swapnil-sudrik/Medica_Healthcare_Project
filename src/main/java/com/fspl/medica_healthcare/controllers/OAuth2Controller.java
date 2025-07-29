package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/OAuth2")
public class OAuth2Controller {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUrlEndpoint;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoURL;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectURL;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserService userService;


    @PostMapping("/google")
    public synchronized ResponseEntity<?> getUserInfo(@RequestParam String code) {
        try{
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret",clientSecret);
            params.add("redirect_uri",redirectURL);
            params.add("grant_type","authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            System.out.println(request);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrlEndpoint, request, Map.class);
            String accessToken = (String) tokenResponse.getBody().get("id_token");
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token="+accessToken;
            ResponseEntity<Map> userInfoResponse = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> userInfo;
            if (userInfoResponse.getStatusCode() == HttpStatus.OK){
                 userInfo = userInfoResponse.getBody();
                System.out.println(userInfo);
                User user = new User();
                assert userInfo != null;
                User isAlreadyExist = userService.findByUsername(userInfo.get("email").toString());
                if (isAlreadyExist !=null){
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ExceptionMessages.USERNAME_ALREADY_EXIST));
                }
                user.setUsername(userInfo.get("email").toString());
                user.setName(userInfo.get("name").toString());
                user.setRoles("USER");
                user.setStatus(1);
                boolean isSaved = userService.saveUserByOAuth(user);
                if (isSaved){
                    User savedUser = userService.findByUsername(user.getUsername());
                    return ResponseEntity.ok().body(savedUser);
                }else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "user not saved please try again"));
                }
            }else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ExceptionMessages.SOMETHING_WENT_WRONG));
            }

//            String peopleApiUrl = "https://people.googleapis.com/v1/people/me?personFields=names,emailAddresses,phoneNumbers,birthdays,genders,addresses";
//
//            // Set up the Authorization header
//            HttpHeaders headers1 = new HttpHeaders();
//            headers1.setBearerAuth(accessToken1);
//            headers1.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//
//            HttpEntity<String> entity = new HttpEntity<>(headers1);
//
//
//                ResponseEntity<String> response = restTemplate.exchange(
//                        peopleApiUrl,
//                        HttpMethod.GET,
//                        entity,
//                        String.class
//                );
//
//                return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ExceptionMessages.SERVER_DOWN));
        }
    }
}
