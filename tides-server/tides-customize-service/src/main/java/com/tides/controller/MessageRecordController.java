package com.tides.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tides.common.ApiResponse;
import com.tides.dto.ExecuteExceptionMessageDto;
import com.tides.dto.MessageRecordDto;
import com.tides.service.MessageRecordService;
import com.tides.vo.MessageRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 消息消费记录 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/message/record")
@Tag(name = "/message/record", description = "消息记录")
public class MessageRecordController {

    @Autowired
    private MessageRecordService messageRecordService;
    
    @Operation(summary  = "分页查询消息记录")
    @PostMapping(value = "/page")
    public ApiResponse<IPage<MessageRecordVo>> page(@Valid @RequestBody MessageRecordDto messageRecordDto) {
        return ApiResponse.ok(messageRecordService.page(messageRecordDto));
    }
    
    @Operation(summary  = "执行对账任务")
    @PostMapping(value = "/execute/Reconciliation/task")
    public ApiResponse<Boolean> executeReconciliationTask() {
        return ApiResponse.ok(messageRecordService.executeReconciliationTask());
    }
    
    @Operation(summary  = "处理异常消息")
    @PostMapping(value = "/execute/exception/message")
    public ApiResponse<Boolean> executeExceptionMessage(@Valid @RequestBody ExecuteExceptionMessageDto executeExceptionMessageDto) {
        return ApiResponse.ok(messageRecordService.executeExceptionMessage(executeExceptionMessageDto));
    }
}
