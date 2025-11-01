package com.tides.controller;

import com.tides.common.ApiResponse;
import com.tides.service.CustomizeDemoDataSnapshotService;
import com.tides.vo.DemoDataSnapshotVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 定制服务演示数据基准恢复 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/customize/demo-data")
@Tag(name = "customize/demo-data", description = "定制服务演示数据基准恢复")
public class CustomizeDemoDataSnapshotController {

    @Autowired
    private CustomizeDemoDataSnapshotService customizeDemoDataSnapshotService;

    @Operation(summary = "查询演示数据基准状态")
    @PostMapping(value = "/status")
    public ApiResponse<DemoDataSnapshotVo> status() {
        return ApiResponse.ok(customizeDemoDataSnapshotService.status());
    }

    @Operation(summary = "恢复消息数据到演示基准")
    @PostMapping(value = "/restore")
    public ApiResponse<DemoDataSnapshotVo> restore() {
        return ApiResponse.ok(customizeDemoDataSnapshotService.restoreSnapshot());
    }
}
