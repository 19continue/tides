package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @description: 节目订单创建 dto
 * @author: 19continue
 **/
@Data
@Schema(title="ProgramOrderCreateDto", description ="节目订单创建")
public class ProgramOrderCreateDto {
    
    @Schema(name ="programId", type ="Long", description ="节目id",requiredMode= RequiredMode.REQUIRED)
    @NotNull
    private Long programId;

    @Schema(name ="screeningId", type ="Long", description ="电影放映场次id，电影购票时必填")
    private Long screeningId;

    @Schema(name ="clientRequestId", type ="String", description ="客户端请求幂等号，同一笔创建订单重试时保持不变")
    private String clientRequestId;
    
    @Schema(name ="userId", type ="Long", description ="用户id",requiredMode= RequiredMode.REQUIRED)
    @NotNull
    private Long userId;
    
    @Schema(name ="ticketUserIdList", type ="List<Long>", description ="购票人id集合",requiredMode= RequiredMode.REQUIRED)
    @NotNull
    private List<Long> ticketUserIdList;
    
    @Schema(name ="seatDtoList", type ="List<SeatDto>", description = "座位")
    private List<SeatDto> seatDtoList;
    
    @Schema(name ="ticketCategoryId", type ="Long", description = "节目票档id(如果不选座位，那么票档id必填)")
    private Long ticketCategoryId;
    
    @Schema(name ="ticketCount", type ="Integer", description = "购买票数量(如果不选座位，那么购买票数量必填)")
    private Integer ticketCount;
}
