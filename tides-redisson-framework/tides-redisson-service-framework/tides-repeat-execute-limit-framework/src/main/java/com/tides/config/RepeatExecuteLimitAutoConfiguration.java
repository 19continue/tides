package com.tides.config;

import com.tides.constant.LockInfoType;
import com.tides.handle.RedissonDataHandle;
import com.tides.locallock.LocalLockCache;
import com.tides.lockinfo.LockInfoHandle;
import com.tides.lockinfo.factory.LockInfoHandleFactory;
import com.tides.lockinfo.impl.RepeatExecuteLimitLockInfoHandle;
import com.tides.repeatexecutelimit.aspect.RepeatExecuteLimitAspect;
import com.tides.servicelock.factory.ServiceLockFactory;
import org.springframework.context.annotation.Bean;

/**
 * @description: 防重复幂等配置
 * @author: 19continue
 **/
public class RepeatExecuteLimitAutoConfiguration {
    
    @Bean(LockInfoType.REPEAT_EXECUTE_LIMIT)
    public LockInfoHandle repeatExecuteLimitHandle(){
        return new RepeatExecuteLimitLockInfoHandle();
    }
    
    @Bean
    public RepeatExecuteLimitAspect repeatExecuteLimitAspect(LocalLockCache localLockCache,
                                                             LockInfoHandleFactory lockInfoHandleFactory,
                                                             ServiceLockFactory serviceLockFactory,
                                                             RedissonDataHandle redissonDataHandle){
        return new RepeatExecuteLimitAspect(localLockCache, lockInfoHandleFactory,serviceLockFactory,redissonDataHandle);
    }
}
    