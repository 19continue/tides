package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@Schema(title = "MovieCinemaScreeningListDto", description = "Cinema screening list query")
public class MovieCinemaScreeningListDto {

    @Schema(name = "cinemaId", type = "Long", description = "cinema id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long cinemaId;

    @Schema(name = "programId", type = "Long", description = "program id")
    private Long programId;

    @Schema(name = "movieId", type = "Long", description = "movie id")
    private Long movieId;

    @Schema(name = "showDayTime", type = "Date", description = "show day")
    private Date showDayTime;
}
