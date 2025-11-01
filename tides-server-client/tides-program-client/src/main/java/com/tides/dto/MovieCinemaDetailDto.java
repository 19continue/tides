package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "MovieCinemaDetailDto", description = "Movie cinema detail query")
public class MovieCinemaDetailDto {

    @Schema(name = "cinemaId", type = "Long", description = "cinema id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long cinemaId;
}
