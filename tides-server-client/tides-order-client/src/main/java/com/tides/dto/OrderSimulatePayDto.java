package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 演示压测用的模拟支付成功入参。
 * 只用于自有环境压测，不接入第三方支付渠道。
 */
@Data
@Schema(title = "OrderSimulatePayDto", description = "演示压测模拟支付成功")
public class OrderSimulatePayDto implements Serializable {

    @Schema(name = "orderNumber", type = "Long", description = "订单编号")
    @NotNull
    private Long orderNumber;
}
