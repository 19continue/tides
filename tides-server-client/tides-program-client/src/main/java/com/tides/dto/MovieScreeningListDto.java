package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@Schema(title = "MovieScreeningListDto", description = "Movie screening list query")
public class MovieScreeningListDto {

    @Schema(name = "programId", type = "Long", description = "program id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long programId;

    @Schema(name = "cinemaId", type = "Long", description = "cinema id")
    private Long cinemaId;

    @Schema(name = "areaId", type = "Long", description = "city or district id")
    private Long areaId;

    @Schema(name = "showDayTime", type = "Date", description = "show day")
    private Date showDayTime;
}
