package com.tides.initialize.execute;

import com.tides.initialize.execute.base.AbstractApplicationExecute;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;

import static com.tides.initialize.constant.InitializeHandlerType.APPLICATION_INITIALIZING_BEAN;

/**
 * @description: 用于处理 {@link InitializingBean} 应用程序启动事件。
 * @author: 19continue
 **/

public class ApplicationInitializingBeanExecute extends AbstractApplicationExecute implements InitializingBean {
    
    public ApplicationInitializingBeanExecute(ConfigurableApplicationContext applicationContext){
        super(applicationContext);
    }
    
    @Override
    public void afterPropertiesSet() {
        execute();
    }
    
    @Override
    public String type() {
        return APPLICATION_INITIALIZING_BEAN;
    }
}
