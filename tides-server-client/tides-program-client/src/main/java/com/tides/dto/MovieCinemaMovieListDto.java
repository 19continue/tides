package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@Schema(title = "MovieCinemaMovieListDto", description = "Cinema movie list query")
public class MovieCinemaMovieListDto {

    @Schema(name = "cinemaId", type = "Long", description = "cinema id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long cinemaId;

    @Schema(name = "showDayTime", type = "Date", description = "show day")
    private Date showDayTime;
}
