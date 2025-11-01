package com.tides.config;

import com.tides.handler.BloomFilterHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @description: 布隆过滤器 配置
 * @author: 19continue
 **/
@EnableConfigurationProperties(BloomFilterProperties.class)
public class BloomFilterAutoConfiguration {
    
    /**
     * 布隆过滤器
     */
    @Bean
    public BloomFilterHandler rBloomFilterUtil(RedissonClient redissonClient, BloomFilterProperties bloomFilterProperties) {
        return new BloomFilterHandler(redissonClient, bloomFilterProperties);
    }
}
