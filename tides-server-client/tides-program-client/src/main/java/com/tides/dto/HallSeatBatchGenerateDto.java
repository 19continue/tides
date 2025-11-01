package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "HallSeatBatchGenerateDto", description = "Hall seat template batch generate")
public class HallSeatBatchGenerateDto {

    @NotNull
    private Long cinemaId;

    @NotNull
    private Long hallId;

    @NotNull
    private Integer rowCount;

    @NotNull
    private Integer colCount;

    private Integer seatType;

    private String zoneName;

    private String priceLevel;
}
