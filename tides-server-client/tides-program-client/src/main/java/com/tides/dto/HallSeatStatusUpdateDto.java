package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(title = "HallSeatStatusUpdateDto", description = "Hall seat template status update")
public class HallSeatStatusUpdateDto {

    @NotNull
    private Long cinemaId;

    @NotNull
    private Long hallId;

    @NotEmpty
    private List<Long> seatIdList;

    @NotNull
    private Integer seatStatus;
}
