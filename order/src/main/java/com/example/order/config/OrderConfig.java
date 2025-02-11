package com.example.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderConfig
{
    @Bean
    public ObjectMapper objectMapper()
    {
        return new ObjectMapper();
    }
}
