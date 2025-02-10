package com.example.order.db.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.locks.ReentrantReadWriteLock;

@Configuration
public class DBConfig
{
    @Bean(name = "datIdxLock")
    public ReentrantReadWriteLock datIdxLock()
    {
        return new ReentrantReadWriteLock();
    }

    @Bean(name = "txnLock")
    public ReentrantReadWriteLock txnLock()
    {
        return new ReentrantReadWriteLock();
    }
}
