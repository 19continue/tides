package com.tides.controller;

import com.tides.common.ApiResponse;
import com.tides.dto.NotifyDto;
import com.tides.dto.PayBillDto;
import com.tides.dto.PayDto;
import com.tides.dto.RefundDto;
import com.tides.dto.TradeCheckDto;
import com.tides.service.PayService;
import com.tides.vo.NotifyVo;
import com.tides.vo.PayBillVo;
import com.tides.vo.TradeCheckVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 支付 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/pay")
@Tag(name = "pay", description = "支付")
public class PayController {
    
    @Autowired
    private PayService payService;
    
    @Operation(summary  = "支付（feign微服务调用，不直接暴漏给前端）")
    @PostMapping(value = "/common/pay")
    public ApiResponse<String> commonPay(@Valid @RequestBody PayDto payDto) {
        return ApiResponse.ok(payService.commonPay(payDto));
    }
    
    @Operation(summary  = "支付后回调通知（feign微服务调用，不直接暴漏给前端）")
    @PostMapping(value = "/notify")
    public ApiResponse<NotifyVo> notify(@Valid @RequestBody NotifyDto notifyDto) {
        return ApiResponse.ok(payService.notify(notifyDto));
    }
    
    @Operation(summary  = "支付状态查询")
    @PostMapping(value = "/trade/check")
    public ApiResponse<TradeCheckVo> tradeCheck(@Valid @RequestBody TradeCheckDto tradeCheckDto) {
        return ApiResponse.ok(payService.tradeCheck(tradeCheckDto));
    }
    
    @Operation(summary  = "退款")
    @PostMapping(value = "/refund")
    public ApiResponse<String> refund(@Valid @RequestBody RefundDto refundDto) {
        return ApiResponse.ok(payService.refund(refundDto));
    }
    
    @Operation(summary  = "账单详情查询")
    @PostMapping(value = "/detail")
    public ApiResponse<PayBillVo> detail(@Valid @RequestBody PayBillDto payBillDto) {
        return ApiResponse.ok(payService.detail(payBillDto));
    }
}
