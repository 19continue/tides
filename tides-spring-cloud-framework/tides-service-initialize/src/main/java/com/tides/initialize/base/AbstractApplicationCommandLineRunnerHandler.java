package com.tides.initialize.base;

import org.springframework.boot.CommandLineRunner;

import static com.tides.initialize.constant.InitializeHandlerType.APPLICATION_COMMAND_LINE_RUNNER;
import static com.tides.initialize.constant.InitializeHandlerType.APPLICATION_POST_CONSTRUCT;

/**
 * @description: 用于处理 {@link CommandLineRunner} 类型 初始化执行 抽象
 * @author: 19continue
 **/
public abstract class AbstractApplicationCommandLineRunnerHandler implements InitializeHandler {
    
    @Override
    public String type() {
        return APPLICATION_COMMAND_LINE_RUNNER;
    }
}
