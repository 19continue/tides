package com.tides.service.strategy;

import com.tides.dto.ProgramOrderCreateDto;

/**
 * @description: 节目订单策略
 * @author: 19continue
 **/
public interface ProgramOrderStrategy {
    
    /**
     * 创建订单
     * @param programOrderCreateDto 订单参数
     * @return 订单编号
     * */
    String createOrder(ProgramOrderCreateDto programOrderCreateDto);
    
    /**
     * 获取版本号
     * @return 版本号
     * */
    String version();
}
