package com.tides.service.init;

import com.tides.BusinessThreadPool;
import com.tides.initialize.base.AbstractApplicationPostConstructHandler;
import com.tides.service.ProgramCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
/**
 * @description: 节目种类缓存
 * @author: 19continue
 **/
@Component
public class ProgramCategoryInitData extends AbstractApplicationPostConstructHandler {
    
    @Autowired
    private ProgramCategoryService programCategoryService;
    
    
    @Override
    public Integer executeOrder() {
        return 1;
    }
    
    @Override
    public void executeInit(final ConfigurableApplicationContext context) {
        BusinessThreadPool.execute(() -> {
            programCategoryService.programCategoryRedisDataInit();
        });
    }
}
