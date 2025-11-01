package com.tides.controller;

import com.tides.common.ApiResponse;
import com.tides.service.ProgramDemoDataSnapshotService;
import com.tides.vo.DemoDataSnapshotVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 节目侧演示数据基准恢复 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/program/demo-data")
@Tag(name = "program/demo-data", description = "节目侧演示数据基准恢复")
public class ProgramDemoDataSnapshotController {

    @Autowired
    private ProgramDemoDataSnapshotService programDemoDataSnapshotService;

    @Operation(summary = "查询演示数据基准状态")
    @PostMapping(value = "/status")
    public ApiResponse<DemoDataSnapshotVo> status() {
        return ApiResponse.ok(programDemoDataSnapshotService.status());
    }

    @Operation(summary = "恢复节目数据到演示基准")
    @PostMapping(value = "/restore")
    public ApiResponse<DemoDataSnapshotVo> restore() {
        return ApiResponse.ok(programDemoDataSnapshotService.restoreSnapshot());
    }
}
