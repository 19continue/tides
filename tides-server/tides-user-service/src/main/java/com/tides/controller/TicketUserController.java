package com.tides.controller;

import com.tides.common.ApiResponse;
import com.tides.dto.TicketUserDto;
import com.tides.dto.TicketUserIdDto;
import com.tides.dto.TicketUserListDto;
import com.tides.service.TicketUserService;
import com.tides.vo.TicketUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description: 购票人 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/ticket/user")
@Tag(name = "ticket-user", description = "购票人")
public class TicketUserController {
    
    @Autowired
    private TicketUserService ticketUserService;
    
    @Operation(summary  = "查询购票人列表")
    @PostMapping(value = "/list")
    public ApiResponse<List<TicketUserVo>> list(@Valid @RequestBody TicketUserListDto ticketUserListDto){
        return ApiResponse.ok(ticketUserService.list(ticketUserListDto));
    }
    
    @Operation(summary  = "添加购票人")
    @PostMapping(value = "/add")
    public ApiResponse<Void> add(@Valid @RequestBody TicketUserDto ticketUserDto){
        ticketUserService.add(ticketUserDto);
        return ApiResponse.ok();
    }
    
    @Operation(summary  = "删除购票人")
    @PostMapping(value = "/delete")
    public ApiResponse<Void> delete(@Valid @RequestBody TicketUserIdDto ticketUserIdDto){
        ticketUserService.delete(ticketUserIdDto);
        return ApiResponse.ok();
    }
}
