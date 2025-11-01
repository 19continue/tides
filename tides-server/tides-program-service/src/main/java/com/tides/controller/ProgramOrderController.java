package com.tides.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.tides.common.ApiResponse;
import com.tides.dto.ProgramOrderCreateDto;
import com.tides.dto.SeatDto;
import com.tides.enums.BaseCode;
import com.tides.enums.ProgramOrderVersion;
import com.tides.exception.TidesFrameException;
import com.tides.service.ProgramOrderService;
import com.tides.service.strategy.ProgramOrderContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @description: 节目订单 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/program/order")
@Tag(name = "program-order", description = "节目订单")
public class ProgramOrderController {
    
    @Autowired
    private ProgramOrderContext programOrderContext;
    
    @Autowired
    private ProgramOrderService programOrderService;
    
    @Value("${ticket-rush-simulator.fast-order-enabled:false}")
    private Boolean fastOrderEnabled;
    
    @Value("${ticket-rush-simulator.user-token:}")
    private String simulatorToken;
    
    @Operation(summary  = "购票V1")
    @PostMapping(value = "/create/v1")
    public ApiResponse<String> createV1(@Valid @RequestBody ProgramOrderCreateDto programOrderCreateDto) {
        return ApiResponse.ok(programOrderContext.get(ProgramOrderVersion.V1_VERSION.getVersion())
                .createOrder(programOrderCreateDto));
    }
    
    @Operation(summary  = "购票V2")
    @PostMapping(value = "/create/v2")
    public ApiResponse<String> createV2(@Valid @RequestBody ProgramOrderCreateDto programOrderCreateDto) {
        return ApiResponse.ok(programOrderContext.get(ProgramOrderVersion.V2_VERSION.getVersion())
                .createOrder(programOrderCreateDto));
    }
    
    @Operation(summary  = "购票V21")
    @PostMapping(value = "/create/v21")
    public ApiResponse<String> createV21(@Valid @RequestBody ProgramOrderCreateDto programOrderCreateDto) {
        return ApiResponse.ok(programOrderContext.get(ProgramOrderVersion.V21_VERSION.getVersion())
                .createOrder(programOrderCreateDto));
    }
    
    @Operation(summary  = "购票V3")
    @PostMapping(value = "/create/v3")
    public ApiResponse<String> createV3(@Valid @RequestBody ProgramOrderCreateDto programOrderCreateDto) {
        return ApiResponse.ok(programOrderContext.get(ProgramOrderVersion.V3_VERSION.getVersion())
                .createOrder(programOrderCreateDto));
    }
    
    @Operation(summary  = "购票V31")
    @PostMapping(value = "/create/v31")
    public ApiResponse<String> createV31(@Valid @RequestBody ProgramOrderCreateDto programOrderCreateDto) {
        return ApiResponse.ok(programOrderContext.get(ProgramOrderVersion.V31_VERSION.getVersion())
                .createOrder(programOrderCreateDto));
    }
    
    @Operation(summary  = "购票V4")
    @PostMapping(value = "/create/v4")
    public ApiResponse<String> createV4(@Valid @RequestBody ProgramOrderCreateDto programOrderCreateDto) {
        return ApiResponse.ok(programOrderContext.get(ProgramOrderVersion.V4_VERSION.getVersion())
                .createOrder(programOrderCreateDto));
    }
    
    @Operation(summary  = "压测快速购票V4")
    @PostMapping(value = "/create/v4/fast")
    public ApiResponse<String> createV4Fast(@Valid @RequestBody ProgramOrderCreateDto programOrderCreateDto,
                                            @RequestHeader(value = "X-Tides-Simulator-Token", required = false) String token) {
        if (!Boolean.TRUE.equals(fastOrderEnabled) || !Objects.equals(simulatorToken, token)) {
            throw new TidesFrameException(BaseCode.API_CALL_PASSWORD_ERROR);
        }
        validateFastOrderParam(programOrderCreateDto);
        return ApiResponse.ok(programOrderService.createNewAsyncFast(programOrderCreateDto,
                ProgramOrderVersion.V4_VERSION.getValue()));
    }
    
    @Operation(summary  = "购票V4")
    @PostMapping(value = "/create/v41")
    public ApiResponse<String> createV41(@Valid @RequestBody ProgramOrderCreateDto programOrderCreateDto) {
        return ApiResponse.ok(programOrderContext.get(ProgramOrderVersion.V41_VERSION.getVersion())
                .createOrder(programOrderCreateDto));
    }
    
    private void validateFastOrderParam(ProgramOrderCreateDto programOrderCreateDto) {
        List<Long> ticketUserIdList = programOrderCreateDto.getTicketUserIdList();
        if (CollectionUtil.isEmpty(ticketUserIdList)) {
            throw new TidesFrameException(BaseCode.TICKET_USER_EMPTY);
        }
        Set<Long> ticketUserIdSet = new HashSet<>();
        for (Long ticketUserId : ticketUserIdList) {
            if (!ticketUserIdSet.add(ticketUserId)) {
                throw new TidesFrameException(BaseCode.TICKET_USER_ID_REPEAT);
            }
        }
        List<SeatDto> seatDtoList = programOrderCreateDto.getSeatDtoList();
        if (CollectionUtil.isNotEmpty(seatDtoList)) {
            validateFastSeatParam(programOrderCreateDto, seatDtoList);
            return;
        }
        if (Objects.isNull(programOrderCreateDto.getTicketCategoryId())) {
            throw new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST);
        }
        if (Objects.isNull(programOrderCreateDto.getTicketCount())) {
            throw new TidesFrameException(BaseCode.TICKET_COUNT_NOT_EXIST);
        }
        if (programOrderCreateDto.getTicketCount() <= 0) {
            throw new TidesFrameException(BaseCode.TICKET_COUNT_ERROR);
        }
        if (programOrderCreateDto.getTicketCount() != ticketUserIdList.size()) {
            throw new TidesFrameException(BaseCode.TICKET_USER_COUNT_UNEQUAL_SEAT_COUNT);
        }
    }
    
    private void validateFastSeatParam(ProgramOrderCreateDto programOrderCreateDto, List<SeatDto> seatDtoList) {
        if (seatDtoList.size() != programOrderCreateDto.getTicketUserIdList().size()) {
            throw new TidesFrameException(BaseCode.TICKET_USER_COUNT_UNEQUAL_SEAT_COUNT);
        }
        Set<Long> seatIdSet = new HashSet<>();
        for (SeatDto seatDto : seatDtoList) {
            if (Objects.isNull(seatDto.getId())) {
                throw new TidesFrameException(BaseCode.SEAT_ID_EMPTY);
            }
            if (!seatIdSet.add(seatDto.getId())) {
                throw new TidesFrameException(BaseCode.SEAT_ID_REPEAT);
            }
            if (Objects.isNull(seatDto.getTicketCategoryId())) {
                throw new TidesFrameException(BaseCode.SEAT_TICKET_CATEGORY_ID_EMPTY);
            }
            if (Objects.isNull(seatDto.getRowCode())) {
                throw new TidesFrameException(BaseCode.SEAT_ROW_CODE_EMPTY);
            }
            if (Objects.isNull(seatDto.getColCode())) {
                throw new TidesFrameException(BaseCode.SEAT_COL_CODE_EMPTY);
            }
            if (Objects.isNull(seatDto.getPrice())) {
                throw new TidesFrameException(BaseCode.SEAT_PRICE_EMPTY);
            }
        }
    }
}
