package com.tides.servicelock.info;

/**
 * @description: 分布式锁 处理失败抽象
 * @author: 19continue
 **/
public interface LockTimeOutHandler {
    
    /**
     * 处理
     * @param lockName 锁名
     * */
    void handler(String lockName);
}
