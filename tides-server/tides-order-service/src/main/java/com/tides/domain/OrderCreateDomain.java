package com.tides.domain;

import com.tides.dto.OrderTicketUserCreateDto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @description: 订单创建 需要的数据
 * @author: 19continue
 **/
@Data
public class OrderCreateDomain {
    
    private Long identifierId;
    
    private Long orderNumber;
 
    private Long programId;

    private Long screeningId;
   
    private String programItemPicture;
    
    private Long userId;
    
    private String programTitle;
    
    private String programPlace;
    
    private Date programShowTime;
    
    private Integer programPermitChooseSeat;
    
    private String distributionMode;
    
    private String takeTicketMode;
    
    private BigDecimal orderPrice;
    
    private Date createOrderTime;
    
    private List<OrderTicketUserCreateDto> orderTicketUserCreateDtoList;
    
    private Integer orderVersion;
    
}
