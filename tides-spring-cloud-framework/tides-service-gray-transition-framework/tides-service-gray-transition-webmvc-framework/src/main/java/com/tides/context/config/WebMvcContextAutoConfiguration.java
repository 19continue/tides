package com.tides.context.config;

import com.tides.context.ContextHandler;
import com.tides.context.impl.WebMvcContextHandler;
import org.springframework.context.annotation.Bean;

/**
 * @description: WebMvc配置
 * @author: 19continue
 **/
public class WebMvcContextAutoConfiguration {
    
    @Bean
    public ContextHandler webMvcContext(){
        return new WebMvcContextHandler();
    }
}
