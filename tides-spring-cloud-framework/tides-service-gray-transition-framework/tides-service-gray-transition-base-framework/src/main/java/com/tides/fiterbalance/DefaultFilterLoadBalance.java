package com.tides.fiterbalance;


import com.tides.balance.FilterLoadBalance;
import com.tides.filter.AbstractServerFilter;
import lombok.AllArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @description: 负载均衡服务过滤接口的实现
 * @author: 19continue
 **/
@AllArgsConstructor
public class DefaultFilterLoadBalance implements FilterLoadBalance {

    protected final List<AbstractServerFilter> strategyFilterList;

    @Override
    public void selectServer(List<ServiceInstance> servers) {
        for (AbstractServerFilter strategyEnabledFilter : strategyFilterList) {
            strategyEnabledFilter.filter(servers);
        }
    }
}