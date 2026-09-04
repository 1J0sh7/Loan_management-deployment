package com.company.config;

import com.sendgrid.Client;
import com.sendgrid.SendGrid;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class SendGridConfig {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Bean
    public SendGrid sendGrid() {
        // Configure timeouts using Apache HttpClient (compatible with v4.x)
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)      // 5 seconds
                .setSocketTimeout(10000)      // 10 seconds
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        Client client = new Client(httpClient);
        return new SendGrid(sendGridApiKey, client);
    }
}