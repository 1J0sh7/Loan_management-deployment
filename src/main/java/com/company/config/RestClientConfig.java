//package com.company.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.SimpleClientHttpRequestFactory;
//import org.springframework.web.client.RestClient;
//
//@Configuration
//public class RestClientConfig {
//
//    @Value("${email.mailgun.url}")   // ← CHANGED THIS
//    private String emailApiUrl;
//
//    @Bean
//    public RestClient restClient() {
//        return RestClient.builder()
//                .baseUrl(emailApiUrl)
//                .requestFactory(new SimpleClientHttpRequestFactory() {{
//                    setConnectTimeout(5000);
//                    setReadTimeout(10000);
//                }})
//                .build();
//    }
//}