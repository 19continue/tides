package com.tides.service.domain;

import com.tides.domain.PurchaseSeat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @description: 创建订单临时需要的数据
 * @author: 19continue
 **/
@Data
@AllArgsConstructor
public class CreateOrderTemporaryData {

    /**
     * 记录id
     */
    private Long identifierId;
    
    /**
     * 购买的座位
     * */
    private List<PurchaseSeat> purchaseSeatList;
   
}
