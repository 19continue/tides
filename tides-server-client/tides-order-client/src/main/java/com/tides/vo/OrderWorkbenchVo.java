package com.tides.vo;

import com.tides.page.PageVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Order operation workbench result.
 */
@Data
@Schema(title = "OrderWorkbenchVo", description = "订单运营台结果")
public class OrderWorkbenchVo {

    @Schema(name = "orderPage", description = "订单分页")
    private PageVo<OrderManageVo> orderPage;

    @Schema(name = "totalCount", type = "Long", description = "当前查询范围订单数")
    private Long totalCount;

    @Schema(name = "noPayCount", type = "Long", description = "未支付订单数")
    private Long noPayCount;

    @Schema(name = "paidCount", type = "Long", description = "已支付订单数")
    private Long paidCount;

    @Schema(name = "cancelCount", type = "Long", description = "已取消订单数")
    private Long cancelCount;

    @Schema(name = "refundCount", type = "Long", description = "已退单订单数")
    private Long refundCount;

    @Schema(name = "abnormalCount", type = "Long", description = "需要运营关注订单数")
    private Long abnormalCount;

    @Schema(name = "timeRangeStart", type = "String", description = "实际查询开始时间")
    private String timeRangeStart;

    @Schema(name = "timeRangeEnd", type = "String", description = "实际查询结束时间")
    private String timeRangeEnd;

    @Schema(name = "scopeTip", type = "String", description = "查询范围提示")
    private String scopeTip;
}
