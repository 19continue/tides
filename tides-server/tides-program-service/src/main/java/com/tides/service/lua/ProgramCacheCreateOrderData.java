package com.tides.service.lua;

import com.tides.domain.PurchaseSeat;
import lombok.Data;

import java.util.List;

/**
 * @description: 节目缓存更新 实体
 * @author: 19continue
 **/
@Data
public class ProgramCacheCreateOrderData {

    private Integer code;
    
    private List<PurchaseSeat> purchaseSeatList;
}
