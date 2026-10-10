package com.night_club.bar_restaurant.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class DarajaConfig {

    // Sandbox: https://sandbox.safaricom.co.ke  |  Production: https://api.safaricom.co.ke
    @Value("${mpesa.base-url}")
    private String baseUrl;

    @Bean
    public RestClient darajaRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}