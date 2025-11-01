package com.tides.initialize.execute;

import com.tides.initialize.execute.base.AbstractApplicationExecute;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import static com.tides.initialize.constant.InitializeHandlerType.APPLICATION_EVENT_LISTENER;

/**
 * @description: 用于处理 {@link ApplicationStartedEvent} 应用程序启动事件。
 * @author: 19continue
 **/
public class ApplicationStartEventListenerExecute extends AbstractApplicationExecute implements ApplicationListener<ApplicationStartedEvent> {
    
    public ApplicationStartEventListenerExecute(ConfigurableApplicationContext applicationContext){
        super(applicationContext);
    }
    
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        execute();
    }
    
    @Override
    public String type() {
        return APPLICATION_EVENT_LISTENER;
    }
}
