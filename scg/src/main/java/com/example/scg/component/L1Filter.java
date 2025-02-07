package com.example.scg.component;

import lombok.*;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class L1Filter extends AbstractGatewayFilterFactory<L1Filter.Config>
{
    public L1Filter()
    {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(L1Filter.Config config)
    {
        return (exchange, chain)->
        {
            System.out.println(config.getMyarg());

            if(config.isPre())
            {
                System.out.println("pre local filter 1");
            }

            return chain.filter(exchange).then(Mono.fromRunnable(() ->
            {
                if(config.isPost())
                {
                    System.out.println("post local filter 1");
                }
            }));
        };
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Config
    {
        private boolean pre;
        private boolean post;
        private String myarg;
    }
}
