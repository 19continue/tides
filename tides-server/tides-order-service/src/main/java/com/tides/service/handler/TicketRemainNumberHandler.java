package com.tides.service.handler;

import com.tides.core.RedisKeyManage;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.servicelock.LockType;
import com.tides.servicelock.annotion.ServiceLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tides.core.DistributedLockConstants.REMAIN_NUMBER_LOCK;

/**
 * @description: 余票处理
 * @author: 19continue
 **/
@Slf4j
@Component
public class TicketRemainNumberHandler {
    
    @Autowired
    private RedisCache redisCache;

    /**
     * 从redis中删除余票数据
     * */
    @ServiceLock(lockType= LockType.Write,name = REMAIN_NUMBER_LOCK,keys = {"#programId","#ticketCategoryId"})
    public void delRedisSeatData(Long programId,Long ticketCategoryId){
        redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_TICKET_REMAIN_NUMBER_HASH_RESOLUTION,programId,ticketCategoryId));
    }
}
