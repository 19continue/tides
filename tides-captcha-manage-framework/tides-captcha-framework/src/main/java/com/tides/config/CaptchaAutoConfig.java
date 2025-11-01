package com.tides.config;

import com.tides.properties.AjCaptchaProperties;
import com.tides.captcha.service.CaptchaCacheService;
import com.tides.captcha.service.CaptchaService;
import com.tides.captcha.service.impl.CaptchaServiceFactory;
import com.tides.service.CaptchaCacheServiceRedisImpl;
import com.tides.service.CaptchaHandle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @description: 验证码配置
 * @author: 19continue
 **/
public class CaptchaAutoConfig {
    
    @Bean
    public CaptchaHandle captchaHandle(CaptchaService captchaService){
        return new CaptchaHandle(captchaService);
    }
    
    @Bean(name = "AjCaptchaCacheService")
    @Primary
    public CaptchaCacheService captchaCacheService(AjCaptchaProperties config, StringRedisTemplate redisTemplate){
        //缓存类型redis/local/....
        CaptchaCacheService ret = CaptchaServiceFactory.getCache(config.getCacheType().name());
        if(ret instanceof CaptchaCacheServiceRedisImpl){
            ((CaptchaCacheServiceRedisImpl)ret).setStringRedisTemplate(redisTemplate);
        }
        return ret;
    }
}
