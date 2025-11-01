package com.tides.core;

import com.tides.servicelock.LockType;
import com.tides.servicelock.ServiceLocker;
import com.tides.servicelock.impl.RedissonFairLocker;
import com.tides.servicelock.impl.RedissonReadLocker;
import com.tides.servicelock.impl.RedissonReentrantLocker;
import com.tides.servicelock.impl.RedissonWriteLocker;
import com.tides.servicelock.impl.RedisServiceLocker;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.tides.servicelock.LockType.Fair;
import static com.tides.servicelock.LockType.Read;
import static com.tides.servicelock.LockType.Reentrant;
import static com.tides.servicelock.LockType.Write;

/**
 * @description: 分布式锁 锁缓存
 * @author: 19continue
 **/
public class ManageLocker {

    private final Map<LockType, ServiceLocker> cacheLocker = new HashMap<>();
    
    public ManageLocker(RedissonClient redissonClient, StringRedisTemplate stringRedisTemplate){
        ServiceLocker redisServiceLocker = new RedisServiceLocker(stringRedisTemplate);
        cacheLocker.put(Reentrant, redisServiceLocker);
        cacheLocker.put(Fair, redisServiceLocker);
        cacheLocker.put(Write, redisServiceLocker);
        cacheLocker.put(Read, redisServiceLocker);
    }
    
    public ServiceLocker getReentrantLocker(){
        return cacheLocker.get(Reentrant);
    }
    
    public ServiceLocker getFairLocker(){
        return cacheLocker.get(Fair);
    }
    
    public ServiceLocker getWriteLocker(){
        return cacheLocker.get(Write);
    }
    
    public ServiceLocker getReadLocker(){
        return cacheLocker.get(Read);
    }
}
