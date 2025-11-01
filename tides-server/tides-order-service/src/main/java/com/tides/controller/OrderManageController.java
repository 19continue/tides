package com.tides.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tides.common.ApiResponse;
import com.tides.dto.OrderPageManageDto;
import com.tides.dto.OrderWorkbenchPageDto;
import com.tides.dto.RecordManageDto;
import com.tides.service.OrderManageService;
import com.tides.service.OrderWorkbenchService;
import com.tides.vo.DiscardOrderManageVo;
import com.tides.vo.OrderManageVo;
import com.tides.vo.OrderWorkbenchVo;
import com.tides.vo.RecordOrderManageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 订单后台管理 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/order/manage")
@Tag(name = "order/manage", description = "订单")
public class OrderManageController {
    
    @Autowired
    private OrderManageService orderManageService;

    @Autowired
    private OrderWorkbenchService orderWorkbenchService;

    

    @Operation(summary  = "操作记录分页列表")
    @PostMapping(value = "/record/page")
    public ApiResponse<IPage<RecordOrderManageVo>> recordPage(@Valid @RequestBody RecordManageDto recordManageDto) {
        return ApiResponse.ok(orderManageService.recordPage(recordManageDto));
    }
    
    @Operation(summary  = "查看订单分页列表")
    @PostMapping(value = "/order/page")
    public ApiResponse<IPage<OrderManageVo>> orderPage(@Valid @RequestBody OrderPageManageDto orderPageManageDto) {
        return ApiResponse.ok(orderManageService.orderPage(orderPageManageDto));
    }

    @Operation(summary  = "订单运营台分页")
    @PostMapping(value = "/order/workbench/page")
    public ApiResponse<OrderWorkbenchVo> orderWorkbenchPage(@Valid @RequestBody OrderWorkbenchPageDto orderWorkbenchPageDto) {
        return ApiResponse.ok(orderWorkbenchService.page(orderWorkbenchPageDto));
    }
    
    @Operation(summary  = "查看废弃订单分页列表")
    @PostMapping(value = "/discard/order/page")
    public ApiResponse<IPage<DiscardOrderManageVo>> discardOrderPage(@Valid @RequestBody OrderPageManageDto orderPageManageDto) {
        return ApiResponse.ok(orderManageService.discardOrderPage(orderPageManageDto));
    }
}
