package com.tides.pay;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description: 支付结果
 * @author: 19continue
 **/
@Data
@AllArgsConstructor
public class PayResult {
    
    private final boolean success;
    
    private final String body;
}
