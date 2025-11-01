package com.tides.pro.limit;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

/**
 * @description: 线上限流工具属性
 * @author: 19continue
 **/
@Data
public class RateLimiterProperty {
    
    @Value("${rate.switch:false}")
    private Boolean rateSwitch;

    @Value("${rate.permits:200}")
    private Integer ratePermits;
}
