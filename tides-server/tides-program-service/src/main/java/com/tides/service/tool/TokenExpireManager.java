package com.tides.service.tool;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @description: token失效时间管理
 * @author: 19continue
 **/
@Data
@Component
public class TokenExpireManager {
    
    @Value("${token.expire.time:40}")
    private Long tokenExpireTime;
}
