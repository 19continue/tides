package com.tides.service.init;

import com.tides.core.SpringUtil;
import com.tides.initialize.base.AbstractApplicationPostConstructHandler;
import com.tides.service.ProgramService;
import com.tides.service.ProgramShowTimeService;
import com.tides.util.BusinessEsHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @description: 节目演出时间更新
 * @author: 19continue
 **/
@Component
public class ProgramShowTimeRenewal extends AbstractApplicationPostConstructHandler {
    
    @Autowired
    private ProgramShowTimeService programShowTimeService;
    
    @Autowired
    private ProgramService programService;
    
    @Autowired
    private BusinessEsHandle businessEsHandle;
    
    @Override
    public Integer executeOrder() {
        return 2;
    }
    
    @Override
    public void executeInit(final ConfigurableApplicationContext context) {
        renewal();
    }
    
    public Set<Long> renewal() {
        //判断节目演出时间是否过期，如果过期了，则更新时间，并返回已经更新演出时间的节目id
        Set<Long> programIdSet = programShowTimeService.renewal();
        if (!programIdSet.isEmpty()) {
            //如果更新了，将elasticsearch的整个索引和数据都删除
            businessEsHandle.deleteIndex(SpringUtil.getPrefixDistinctionName() + "-" +
                    ProgramDocumentParamName.INDEX_NAME);
            for (Long programId : programIdSet) {
                //将redis中的数据也删除
                programService.delRedisData(programId);
                //将本地缓存数据也删除
                programService.delLocalCache(programId);
            }
        }
        return programIdSet;
    }
}
