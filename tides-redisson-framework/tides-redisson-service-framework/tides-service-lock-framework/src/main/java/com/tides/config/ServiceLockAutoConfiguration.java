package com.tides.config;

import com.tides.constant.LockInfoType;
import com.tides.core.ManageLocker;
import com.tides.lockinfo.LockInfoHandle;
import com.tides.lockinfo.factory.LockInfoHandleFactory;
import com.tides.lockinfo.impl.ServiceLockInfoHandle;
import com.tides.servicelock.aspect.ServiceLockAspect;
import com.tides.servicelock.factory.ServiceLockFactory;
import com.tides.util.ServiceLockTool;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @description: 分布式锁 配置
 * @author: 19continue
 **/
public class ServiceLockAutoConfiguration {
    
    @Bean(LockInfoType.SERVICE_LOCK)
    public LockInfoHandle serviceLockInfoHandle(){
        return new ServiceLockInfoHandle();
    }
    
    @Bean
    public ManageLocker manageLocker(RedissonClient redissonClient, StringRedisTemplate stringRedisTemplate){
        return new ManageLocker(redissonClient, stringRedisTemplate);
    }
    
    @Bean
    public ServiceLockFactory serviceLockFactory(ManageLocker manageLocker){
        return new ServiceLockFactory(manageLocker);
    }
    
    @Bean
    public ServiceLockAspect serviceLockAspect(LockInfoHandleFactory lockInfoHandleFactory,ServiceLockFactory serviceLockFactory){
        return new ServiceLockAspect(lockInfoHandleFactory,serviceLockFactory);
    }
    
    @Bean
    public ServiceLockTool serviceLockUtil(LockInfoHandleFactory lockInfoHandleFactory,ServiceLockFactory serviceLockFactory){
        return new ServiceLockTool(lockInfoHandleFactory,serviceLockFactory);
    }
}
