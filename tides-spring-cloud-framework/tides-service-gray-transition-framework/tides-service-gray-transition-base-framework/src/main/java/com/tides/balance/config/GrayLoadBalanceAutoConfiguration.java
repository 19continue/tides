package com.tides.balance.config;

import com.tides.context.ContextHandler;
import com.tides.enhance.config.EnhanceLoadBalancerClientConfiguration;
import com.tides.enhance.config.EnhanceLoadBalancerClientConfiguration.BlockingSupportConfiguration;
import com.tides.enhance.config.EnhanceLoadBalancerClientConfiguration.ReactiveSupportConfiguration;
import com.tides.filter.AbstractServerFilter;
import com.tides.filter.impl.ServerGrayFilter;
import com.tides.fiterbalance.DefaultFilterLoadBalance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @description: 灰度版本选择相关配置
 * @author: 19continue
 **/
@LoadBalancerClients(defaultConfiguration = {EnhanceLoadBalancerClientConfiguration.class, ReactiveSupportConfiguration.class, BlockingSupportConfiguration.class})
public class GrayLoadBalanceAutoConfiguration {
    
    @Bean
    public DefaultFilterLoadBalance defaultFilterLoadBalance(List<AbstractServerFilter> strategyEnabledFilterList){
        return new DefaultFilterLoadBalance(strategyEnabledFilterList);
    }
    
    @Bean
    public AbstractServerFilter serverGrayFilter(ContextHandler contextHandler) {
        return new ServerGrayFilter(contextHandler);
    }
}
