package com.tides.balance;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @description: 负载均衡服务过滤接口
 * @author: 19continue
 **/
public interface FilterLoadBalance {
    
    /**
     * 服务过滤操作
     * @param servers 服务列表
     * */
    void selectServer(List<ServiceInstance> servers);
}
