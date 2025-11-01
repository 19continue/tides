package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(title = "MovieOrderSalesCountDto", description = "电影已支付订单销量统计请求")
public class MovieOrderSalesCountDto {

    @Schema(name = "programIdList", type = "Long[]", description = "电影节目id列表")
    private List<Long> programIdList;

    @Schema(name = "screeningIdList", type = "Long[]", description = "电影场次id列表")
    private List<Long> screeningIdList;
}
