package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "HallStatusUpdateDto", description = "Hall status update")
public class HallStatusUpdateDto {

    @Schema(name = "hallId", type = "Long", description = "hall id")
    @NotNull
    private Long hallId;

    @Schema(name = "cinemaId", type = "Long", description = "cinema id")
    @NotNull
    private Long cinemaId;

    @Schema(name = "status", type = "Integer", description = "status")
    @NotNull
    private Integer status;
}
