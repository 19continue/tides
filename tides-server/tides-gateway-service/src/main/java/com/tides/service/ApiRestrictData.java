package com.tides.service;

import lombok.Data;

/**
 * @description: 接口请求记录 实体对象
 * @author: 19continue
 **/
@Data
public class ApiRestrictData {

    private Long triggerResult;
    
    private Long triggerCallStat;
    
    private Long apiCount;
    
    private Long threshold;
    
    private Long messageIndex;
}
