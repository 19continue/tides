package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "HallSeatSaveDto", description = "Hall seat template save")
public class HallSeatSaveDto {

    private Long id;

    @NotNull
    private Long cinemaId;

    @NotNull
    private Long hallId;

    @NotNull
    private Integer rowCode;

    @NotNull
    private Integer colCode;

    private String seatNo;

    private String zoneName;

    private String priceLevel;

    private Integer seatType;

    private Integer seatStatus;

    private Integer aisleFlag;

    private Integer coupleFlag;

    private Integer accessibleFlag;

    private Integer vipFlag;

    private Integer repairFlag;

    private Integer sellableFlag;

    private Integer xCoordinate;

    private Integer yCoordinate;
}
