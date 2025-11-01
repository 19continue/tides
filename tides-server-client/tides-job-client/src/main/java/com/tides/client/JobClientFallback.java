package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.JobCallBackDto;
import com.tides.enums.BaseCode;
import org.springframework.stereotype.Component;

/**
 * @description: job服务 feign 异常
 * @author: 19continue
 **/
@Component
public class JobClientFallback implements JobClient {
    
    @Override
    public ApiResponse<Boolean> callBack(final JobCallBackDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
