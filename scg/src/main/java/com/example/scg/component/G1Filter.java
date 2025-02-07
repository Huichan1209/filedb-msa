package com.example.scg.component;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class G1Filter implements GlobalFilter, Ordered
{
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        // 마이크로서비스로 요청이 넘어가기전에 처리할 로직(pre)
        System.out.println("pre global filter order -1");

        return chain.filter(exchange).then(Mono.fromRunnable(()->
        {
            // 마이크로서비스를 거치고 돌아온 시점에 처리할 로직(Post)
            System.out.println("post global filter order -1");
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
