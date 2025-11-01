package com.tides.lock;

/**
 * @description: 锁的任务
 * @author: 19continue
 **/
@FunctionalInterface
public interface LockTask<V> {
    /**
     * 执行锁的任务
     * @return 结果
     */
    V execute();
}