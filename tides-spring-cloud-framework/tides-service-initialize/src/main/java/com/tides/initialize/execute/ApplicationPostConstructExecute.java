package com.tides.initialize.execute;

import com.tides.initialize.execute.base.AbstractApplicationExecute;
import org.springframework.context.ConfigurableApplicationContext;

import jakarta.annotation.PostConstruct;

import static com.tides.initialize.constant.InitializeHandlerType.APPLICATION_POST_CONSTRUCT;

/**
 * @description: 用于处理 {@link PostConstruct} 应用程序启动事件。
 * @author: 19continue
 **/
public class ApplicationPostConstructExecute extends AbstractApplicationExecute {
    
    public ApplicationPostConstructExecute(ConfigurableApplicationContext applicationContext){
        super(applicationContext);
    }
    
    @PostConstruct
    public void postConstructExecute() {
        execute();
    }
    
    @Override
    public String type() {
        return APPLICATION_POST_CONSTRUCT;
    }
}
