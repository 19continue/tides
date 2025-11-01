package com.tides.mq.callback;

/**
 * @description: 用于执行失败的情况
 * @author: 19continue
 **/
@FunctionalInterface
public interface FailureCallback {
    
    /**
     * 执行逻辑
     * @param ex 执行失败的异常当做参数传递
     * */
    void onFailure(Throwable ex);

}