package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.NotifyDto;
import com.tides.dto.PayDto;
import com.tides.dto.RefundDto;
import com.tides.dto.TradeCheckDto;
import com.tides.vo.NotifyVo;
import com.tides.vo.TradeCheckVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import static com.tides.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

/**
 * @description: 支付服务 feign
 * @author: 19continue
 **/
@Component
@FeignClient(value = SPRING_INJECT_PREFIX_DISTINCTION_NAME+"-"+"pay-service",fallback = PayClientFallback.class)
public interface PayClient {
    /**
     * 支付
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping(value = "/pay/common/pay")
    ApiResponse<String> commonPay(PayDto dto);
    /**
     * 回调
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping(value = "/pay/notify")
    ApiResponse<NotifyVo> notify(NotifyDto dto);
    /**
     * 查询支付状态
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping(value = "/pay/trade/check")
    ApiResponse<TradeCheckVo> tradeCheck(TradeCheckDto dto);
    /**
     * 退款
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping(value = "/pay/refund")
    ApiResponse<String> refund(RefundDto dto);
}
