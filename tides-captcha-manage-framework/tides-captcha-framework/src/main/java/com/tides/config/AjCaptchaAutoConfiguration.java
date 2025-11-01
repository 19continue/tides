package com.tides.config;

import com.tides.properties.AjCaptchaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @description: AjCaptchaAutoConfiguration
 * @author: 19continue
 **/

@Configuration
@EnableConfigurationProperties(AjCaptchaProperties.class)
@ComponentScan("com.tides")
@Import({AjCaptchaServiceAutoConfiguration.class, AjCaptchaStorageAutoConfiguration.class})
public class AjCaptchaAutoConfiguration {
}
