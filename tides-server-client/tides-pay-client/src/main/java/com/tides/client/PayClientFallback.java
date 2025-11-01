package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.NotifyDto;
import com.tides.dto.PayDto;
import com.tides.dto.RefundDto;
import com.tides.dto.TradeCheckDto;
import com.tides.enums.BaseCode;
import com.tides.vo.NotifyVo;
import com.tides.vo.TradeCheckVo;
import org.springframework.stereotype.Component;

/**
 * @description: 支付服务 feign 异常
 * @author: 19continue
 **/
@Component
public class PayClientFallback implements PayClient{
    
    @Override
    public ApiResponse<String> commonPay(final PayDto payDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<NotifyVo> notify(final NotifyDto notifyDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    

    @Override
    public ApiResponse<TradeCheckVo> tradeCheck(final TradeCheckDto tradeCheckDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<String> refund(final RefundDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
