package com.tides.pro.limit;

import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @description: 线上限流工具
 * @author: 19continue
 **/
public class RateLimiter {
    
    private final Semaphore semaphore;
    private final TimeUnit timeUnit;
    
    public RateLimiter(int maxPermitsPerSecond) {
        this.timeUnit = TimeUnit.SECONDS;
        this.semaphore = new Semaphore(maxPermitsPerSecond);
    }
    
    public void acquire() throws InterruptedException {
        if (!semaphore.tryAcquire(1, timeUnit)) {
            throw new TidesFrameException(BaseCode.OPERATION_IS_TOO_FREQUENT_PLEASE_TRY_AGAIN_LATER);
        }
    }
    
    public void release() {
        semaphore.release();
    }
}
