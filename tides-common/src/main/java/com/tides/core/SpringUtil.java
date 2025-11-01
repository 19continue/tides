package com.tides.core;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static com.tides.constant.Constant.DEFAULT_PREFIX_DISTINCTION_NAME;
import static com.tides.constant.Constant.PREFIX_DISTINCTION_NAME;

/**
 * @description: spring工具
 * @author: 19continue
 **/

public class SpringUtil implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    private static ConfigurableApplicationContext configurableApplicationContext;
    
    
    public static String getPrefixDistinctionName(){
        return configurableApplicationContext.getEnvironment().getProperty(PREFIX_DISTINCTION_NAME,
                DEFAULT_PREFIX_DISTINCTION_NAME);
    }
    
    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
        configurableApplicationContext = applicationContext;
    }
    
    public static <T> T getBean(Class<T> requiredType){
        return configurableApplicationContext.getBean(requiredType);
    }
    public static <T> T getBean(String name, Class<T> requiredType){
        return configurableApplicationContext.getBean(name,requiredType);
    }
}
