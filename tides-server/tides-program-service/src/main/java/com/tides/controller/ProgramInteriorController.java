package com.tides.controller;

import com.tides.common.ApiResponse;
import com.tides.dto.ProgramOperateDataDto;
import com.tides.dto.ReduceRemainNumberDto;
import com.tides.service.ProgramService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 节目 控制层(内部rpc调用)
 * @author: 19continue
 **/
@RestController
@RequestMapping("/program/interior")
public class ProgramInteriorController {
    
    @Autowired
    private ProgramService programService;
    
    
    @Operation(summary  = "扣减库存相关操作")
    @PostMapping(value = "/reduce/remain/number")
    public ApiResponse<Boolean> operateSeatLockAndTicketCategoryRemainNumber(@Valid @RequestBody ReduceRemainNumberDto reduceRemainNumberDto) {
        return ApiResponse.ok(programService.operateSeatLockAndTicketCategoryRemainNumber(reduceRemainNumberDto));
    }
    
    @Operation(summary  = "订单支付成功或者取消订单后对节目服务库的相关操作")
    @PostMapping(value = "/operate/program/data")
    public ApiResponse<Boolean> operateProgramData(@Valid @RequestBody ProgramOperateDataDto programOperateDataDto){
        return ApiResponse.ok(programService.operateProgramData(programOperateDataDto));
    }
}
