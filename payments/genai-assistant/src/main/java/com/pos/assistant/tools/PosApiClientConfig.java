package com.pos.assistant.tools;

import com.pos.assistant.AssistantProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class PosApiClientConfig {

    /** Calls go through the API gateway; short timeouts keep a slow backend from stalling the chat turn. */
    @Bean
    RestClient posApiClient(RestClient.Builder builder, AssistantProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.baseUrl(properties.posApiUrl()).requestFactory(factory).build();
    }
}
