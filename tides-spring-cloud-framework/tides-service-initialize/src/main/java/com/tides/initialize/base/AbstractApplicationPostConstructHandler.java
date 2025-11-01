package com.tides.initialize.base;

import jakarta.annotation.PostConstruct;

import static com.tides.initialize.constant.InitializeHandlerType.APPLICATION_POST_CONSTRUCT;

/**
 * @description: 用于处理 {@link PostConstruct} 类型 初始化执行 抽象
 * @author: 19continue
 **/
public abstract class AbstractApplicationPostConstructHandler implements InitializeHandler {
    
    @Override
    public String type() {
        return APPLICATION_POST_CONSTRUCT;
    }
}
