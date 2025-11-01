package com.tides.service.init;

import cn.hutool.core.collection.CollectionUtil;
import com.tides.BusinessThreadPool;
import com.tides.handler.BloomFilterHandler;
import com.tides.initialize.base.AbstractApplicationPostConstructHandler;
import com.tides.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 节目种类缓存
 * @author: 19continue
 **/
@Component
public class UserBloomFilterInitData extends AbstractApplicationPostConstructHandler {
    
    @Autowired
    private BloomFilterHandler bloomFilterHandler;
    
    @Autowired
    private UserService userService;
    
    
    @Override
    public Integer executeOrder() {
        return 1;
    }
    
    @Override
    public void executeInit(final ConfigurableApplicationContext context) {
        BusinessThreadPool.execute(() -> {
            List<String> allMobile = userService.getAllMobile();
            if (CollectionUtil.isNotEmpty(allMobile)) {
                allMobile.forEach(mobile -> bloomFilterHandler.add(mobile));
            }
        });
    }
}
