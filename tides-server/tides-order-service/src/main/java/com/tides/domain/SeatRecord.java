package com.tides.domain;

import lombok.Data;

/**
 * @description: redis的操作记录(座位层)
 * @author: 19continue
 **/
@Data
public class SeatRecord {
    
    private Long ticketCategoryId;
    private Long seatId;
    private Long ticketUserId;
    private Integer beforeStatus;
    private Integer afterStatus;
}
