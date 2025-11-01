package com.tides.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import static com.tides.constant.Constant.SERVER_GRAY;


/**
 * @description: feign扩展插件配置类
 * @author: 19continue
 **/

public class ExtraFeignAutoConfiguration {
    
    @Value(SERVER_GRAY)
    public String serverGray;
    
    @Bean
    public FeignRequestInterceptor feignRequestInterceptor(){
        return new FeignRequestInterceptor(serverGray);
    }
}
