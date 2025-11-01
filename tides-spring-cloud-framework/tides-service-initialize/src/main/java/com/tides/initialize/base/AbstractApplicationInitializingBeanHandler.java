package com.tides.initialize.base;

import org.springframework.beans.factory.InitializingBean;

import static com.tides.initialize.constant.InitializeHandlerType.APPLICATION_INITIALIZING_BEAN;

/**
 * @description: 用于处理 {@link InitializingBean} 类型 初始化执行 抽象
 * @author: 19continue
 **/
public abstract class AbstractApplicationInitializingBeanHandler implements InitializeHandler {
    
    @Override
    public String type() {
        return APPLICATION_INITIALIZING_BEAN;
    }
}
