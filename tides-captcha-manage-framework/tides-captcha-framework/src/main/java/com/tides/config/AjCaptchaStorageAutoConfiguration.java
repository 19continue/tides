package com.tides.config;

import com.tides.properties.AjCaptchaProperties;
import com.tides.captcha.service.CaptchaCacheService;
import com.tides.captcha.service.impl.CaptchaServiceFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @description: 存储策略自动配置
 * @author: 19continue
 **/
@Configuration
public class AjCaptchaStorageAutoConfiguration {

    @Bean(name = "AjCaptchaCacheService")
    @ConditionalOnMissingBean
    public CaptchaCacheService captchaCacheService(AjCaptchaProperties config){
        //缓存类型redis/local/....
        return CaptchaServiceFactory.getCache(config.getCacheType().name());
    }
}
