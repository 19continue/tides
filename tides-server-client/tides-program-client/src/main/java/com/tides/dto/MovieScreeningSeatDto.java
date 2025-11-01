package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description: 电影场次座位 dto
 */
@Data
@Schema(title = "MovieScreeningSeatDto", description = "电影场次座位")
public class MovieScreeningSeatDto {

    @Schema(name = "screeningId", type = "Long", description = "放映场次id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long screeningId;
}
