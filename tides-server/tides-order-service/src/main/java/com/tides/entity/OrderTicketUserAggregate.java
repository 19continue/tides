package com.tides.entity;

import lombok.Data;

/**
 * @description: 购票人订单聚合统计 实体
 * @author: 19continue
 **/
@Data
public class OrderTicketUserAggregate {
    
    private Long orderNumber;
    
    private Integer orderTicketUserCount;
}
