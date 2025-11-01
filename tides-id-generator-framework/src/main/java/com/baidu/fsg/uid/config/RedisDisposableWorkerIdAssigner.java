package com.baidu.fsg.uid.config;

import com.baidu.fsg.uid.worker.WorkerIdAssigner;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

/**
 * @description: redis配置生成work_id
 * @author: 19continue
 **/
public class RedisDisposableWorkerIdAssigner implements WorkerIdAssigner {
    
    private RedisTemplate redisTemplate;
    
    public RedisDisposableWorkerIdAssigner (RedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public long assignWorkerId() {
        String key = "uid_work_id";
        Long increment = redisTemplate.opsForValue().increment(key);
        return Optional.ofNullable(increment).orElseThrow(() -> new TidesFrameException(BaseCode.UID_WORK_ID_ERROR));
    }
}
