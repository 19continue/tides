package com.tides.context;

/**
 * @description: 上下文获取抽象
 * @author: 19continue
 **/
public interface ContextHandler {
    
    /***
     * 从request请求头获取值
     * @param name 值的名
     * @return 具体值
     * 
     */
    String getValueFromHeader(String name);
}