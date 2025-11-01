package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.AccountOrderCountDto;
import com.tides.dto.MovieOrderSalesCountDto;
import com.tides.dto.OrderCreateDto;
import com.tides.enums.BaseCode;
import com.tides.vo.AccountOrderCountVo;
import com.tides.vo.MovieOrderSalesCountVo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 订单服务 feign 异常
 * @author: 19continue
 **/
@Component
public class OrderClientFallback implements OrderClient {
    
    @Override
    public ApiResponse<String> create(final OrderCreateDto orderCreateDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<AccountOrderCountVo> accountOrderCount(final AccountOrderCountDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }

    @Override
    public ApiResponse<List<MovieOrderSalesCountVo>> movieSalesCount(final MovieOrderSalesCountDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<Void> reloadRouteMappingCache() {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
