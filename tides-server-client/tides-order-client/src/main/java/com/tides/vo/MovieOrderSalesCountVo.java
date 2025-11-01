package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "MovieOrderSalesCountVo", description = "电影已支付订单销量统计响应")
public class MovieOrderSalesCountVo {

    @Schema(name = "programId", type = "Long", description = "节目id")
    private Long programId;

    @Schema(name = "screeningId", type = "Long", description = "场次id")
    private Long screeningId;

    @Schema(name = "paidOrderCount", type = "Long", description = "已支付订单数")
    private Long paidOrderCount;

    @Schema(name = "paidTicketCount", type = "Long", description = "已支付购票明细数")
    private Long paidTicketCount;
}
