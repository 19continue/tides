package com.tides.reconciliation;

/**
 * @description: 对账任务接口
 * @author: 19continue
 **/
@FunctionalInterface
public interface ReconciliationTask {
    
    /***
     * 执行任务
     */
    void run();
}
