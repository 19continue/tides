package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Order operation workbench query.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "OrderWorkbenchPageDto", description = "订单运营台分页查询")
public class OrderWorkbenchPageDto extends BasePageDto {

    @Schema(name = "programId", type = "Long", description = "节目/项目ID")
    private Long programId;

    @Schema(name = "screeningId", type = "Long", description = "电影放映场次ID")
    private Long screeningId;

    @Schema(name = "orderNumber", type = "Long", description = "订单编号")
    private Long orderNumber;

    @Schema(name = "userId", type = "Long", description = "用户ID")
    private Long userId;

    @Schema(name = "orderStatus", type = "Integer", description = "订单状态 1:未支付 2:已取消 3:已支付 4:已退单")
    private Integer orderStatus;

    @Schema(name = "createTimeStart", type = "String", description = "下单开始时间 yyyy-MM-dd HH:mm:ss")
    private String createTimeStart;

    @Schema(name = "createTimeEnd", type = "String", description = "下单结束时间 yyyy-MM-dd HH:mm:ss")
    private String createTimeEnd;
}
