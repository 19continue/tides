package com.tides.pay;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description: 支付状态查询 实体
 * @author: 19continue
 **/
@Data
public class TradeResult {
    
    private boolean success;
    
    private Integer payBillStatus;
    
    private String outTradeNo;
    
    private BigDecimal totalAmount;
}
