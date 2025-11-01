package com.tides.initialize.impl.composite.init;

import com.tides.initialize.base.AbstractApplicationStartEventListenerHandler;
import com.tides.initialize.impl.composite.CompositeContainer;
import lombok.AllArgsConstructor;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @description: 组合模式初始化操作执行
 * @author: 19continue
 **/
@AllArgsConstructor
public class CompositeInit extends AbstractApplicationStartEventListenerHandler {
    
    private final CompositeContainer compositeContainer;
    
    @Override
    public Integer executeOrder() {
        return 1;
    }
    
    @Override
    public void executeInit(ConfigurableApplicationContext context) {
        compositeContainer.init(context);
    }
}
