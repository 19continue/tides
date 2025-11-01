package com.tides.controller;

import com.tides.common.ApiResponse;
import com.tides.service.OrderDemoDataSnapshotService;
import com.tides.vo.DemoDataSnapshotVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 订单侧演示数据基准恢复 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/order/demo-data")
@Tag(name = "order/demo-data", description = "订单侧演示数据基准恢复")
public class OrderDemoDataSnapshotController {

    @Autowired
    private OrderDemoDataSnapshotService orderDemoDataSnapshotService;

    @Operation(summary = "查询演示数据基准状态")
    @PostMapping(value = "/status")
    public ApiResponse<DemoDataSnapshotVo> status() {
        return ApiResponse.ok(orderDemoDataSnapshotService.status());
    }

    @Operation(summary = "恢复订单数据到演示基准")
    @PostMapping(value = "/restore")
    public ApiResponse<DemoDataSnapshotVo> restore() {
        return ApiResponse.ok(orderDemoDataSnapshotService.restoreSnapshot());
    }
}
