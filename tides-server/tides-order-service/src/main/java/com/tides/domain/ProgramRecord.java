package com.tides.domain;

import lombok.Data;

import java.util.List;

/**
 * @description: redis的操作记录
 * @author: 19continue
 **/
@Data
public class ProgramRecord {
    
    private Long timestamp;
    
    private String recordType;
    
    private List<TicketCategoryRecord> ticketCategoryRecordList;
}
