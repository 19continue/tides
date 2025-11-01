package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description: Movie screening price manage dto
 */
@Data
@Schema(title = "MovieScreeningPriceManageDto", description = "Movie screening price manage")
public class MovieScreeningPriceManageDto {

    @Schema(name = "screeningId", type = "Long", description = "screening id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long screeningId;
}
