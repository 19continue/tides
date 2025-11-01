package com.tides.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description: 票档统计 实体
 * @author: 19continue
 **/
@Data
public class TicketCategoryAggregate implements Serializable {
    
    /**
     * 节目表id
     */
    private Long programId;
    
    /**
     * 最低价格
     */
    private BigDecimal minPrice;
    
    /**
     * 最高价格
     */
    private BigDecimal maxPrice;
}
