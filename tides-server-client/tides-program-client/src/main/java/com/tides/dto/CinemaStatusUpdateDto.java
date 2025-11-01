package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "CinemaStatusUpdateDto", description = "Cinema status update")
public class CinemaStatusUpdateDto {

    @Schema(name = "cinemaId", type = "Long", description = "cinema id")
    @NotNull
    private Long cinemaId;

    @Schema(name = "status", type = "Integer", description = "status")
    @NotNull
    private Integer status;
}
