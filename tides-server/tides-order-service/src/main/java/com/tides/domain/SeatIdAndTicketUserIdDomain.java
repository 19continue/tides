package com.tides.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 座位id和购票人id
 * @author: 19continue
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatIdAndTicketUserIdDomain {

    private Long seatId;
    
    private Long ticketUserId;
}
