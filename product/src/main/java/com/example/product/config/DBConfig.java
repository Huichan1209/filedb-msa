package com.example.product.config;

import com.example.product.db.GarbageCollector;
import com.example.product.db.IdGenerator;
import com.example.product.db.TransactionManager;
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

    @Bean
    public IdGenerator idGenerator()
    {
        return new IdGenerator();
    }

    @Bean
    public GarbageCollector garbageCollector()
    {
        return new GarbageCollector();
    }
}
