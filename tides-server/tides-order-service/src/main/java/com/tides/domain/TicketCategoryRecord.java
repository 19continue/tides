package com.tides.domain;

import lombok.Data;

import java.util.List;

/**
 * @description: redis的操作记录(票档层)
 * @author: 19continue
 **/
@Data
public class TicketCategoryRecord {
    
    private Long ticketCategoryId;
    private Long beforeAmount;
    private Long afterAmount;
    private Long changeAmount;
    private List<SeatRecord> seatRecordList;
}
