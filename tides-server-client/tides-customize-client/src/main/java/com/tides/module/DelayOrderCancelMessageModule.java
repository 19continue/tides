package com.tides.module;

import lombok.Data;

/**
 * @description: DelayOrderCancelMessageModule
 * @author: 19continue
 **/
@Data
public class DelayOrderCancelMessageModule {

    private Long messageTraceId;
    
    private Long messageId;
    
    private Long programId;
    
    private Long orderNumber;
}
