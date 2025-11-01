package com.tides.controller;

import com.tides.common.ApiResponse;
import com.tides.dto.TestDto;
import com.tides.dto.TestSendDto;
import com.tides.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 节目订单 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private TestService testService;

    @Operation(summary  = "重置消息计数器")
    @PostMapping(value = "/reset")
    public ApiResponse<Boolean> reset(@Valid @RequestBody TestSendDto testSendDto) {
        return ApiResponse.ok(testService.reset(testSendDto));
    }
    
    @PostMapping(value = "/test")
    public ApiResponse<Void> test(@Valid @RequestBody TestDto testDto){
        return ApiResponse.ok();
    }
}
