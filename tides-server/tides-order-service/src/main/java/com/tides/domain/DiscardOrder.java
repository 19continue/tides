package com.tides.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 丢弃的订单
 * @author: 19continue
 **/
@Data
@NoArgsConstructor
public class DiscardOrder {
    /**
     * 参数信息
     * */
    private OrderCreateMq orderCreateMq;
    
    /**
     * 原因
     * */
    private Integer discardOrderReason;
    
    /**
     * 错误信息
     * */
    private String errorMsg;
    
    public DiscardOrder(OrderCreateMq orderCreateMq, Integer discardOrderReason) {
        this.orderCreateMq = orderCreateMq;
        this.discardOrderReason = discardOrderReason;
    }
    
    public DiscardOrder(OrderCreateMq orderCreateMq, Integer discardOrderReason, String errorMsg) {
        this.orderCreateMq = orderCreateMq;
        this.discardOrderReason = discardOrderReason;
        this.errorMsg = errorMsg;
    }
}
