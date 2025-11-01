package com.tides.context.config;

import com.tides.context.ContextHandler;
import com.tides.context.filter.GatewayWorkClearFilter;
import com.tides.context.filter.GatewayWorkRouteFilter;
import com.tides.context.impl.GatewayContextHandler;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;

/**
 * @description: Gateway配置
 * @author: 19continue
 **/
public class GatewayContextAutoConfiguration {
    
    @Bean
    public GlobalFilter gatewayWorkRouteFilter() {
        return new GatewayWorkRouteFilter();
    }
    
    @Bean
    public GlobalFilter gatewayWorkClearFilter() {
        return new GatewayWorkClearFilter();
    }
    
    @Bean
    public ContextHandler webMvcContext(){
        return new GatewayContextHandler();
    }
}
