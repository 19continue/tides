package com.tides.conf;

import com.tides.common.ApiResponse;
import lombok.Data;

import java.util.Map;

/**
 * @description: 临时信息
 * @author: 19continue
 **/
@Data
public class RequestTemporaryWrapper {
    
    private Map<String,String> map;
    
    private ApiResponse<?> apiResponse;
}
