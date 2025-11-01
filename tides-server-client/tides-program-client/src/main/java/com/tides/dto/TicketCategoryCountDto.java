package com.tides.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 票据数据操作 dto
 * @author: 19continue
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketCategoryCountDto {
    
    /**
     * 票档id
     * */
    private Long ticketCategoryId;
    
    /**
     * 数量
     * */
    private Long count;
}
