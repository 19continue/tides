package com.tides.initialize.config;

import com.tides.initialize.execute.ApplicationCommandLineRunnerExecute;
import com.tides.initialize.execute.ApplicationInitializingBeanExecute;
import com.tides.initialize.execute.ApplicationPostConstructExecute;
import com.tides.initialize.execute.ApplicationStartEventListenerExecute;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @description: 初始化执行 相关配置
 * @author: 19continue
 **/
public class InitializeAutoConfig {
    
    @Bean
    public ApplicationInitializingBeanExecute applicationInitializingBeanExecute(
            ConfigurableApplicationContext applicationContext){
        return new ApplicationInitializingBeanExecute(applicationContext);
    }
    
    @Bean
    public ApplicationPostConstructExecute applicationPostConstructExecute(
            ConfigurableApplicationContext applicationContext){
        return new ApplicationPostConstructExecute(applicationContext);
    }
    
    @Bean
    public ApplicationStartEventListenerExecute applicationStartEventListenerExecute(
            ConfigurableApplicationContext applicationContext){
        return new ApplicationStartEventListenerExecute(applicationContext);
    }
    
    @Bean
    public ApplicationCommandLineRunnerExecute applicationCommandLineRunnerExecute(
            ConfigurableApplicationContext applicationContext){
        return new ApplicationCommandLineRunnerExecute(applicationContext);
    }
}
