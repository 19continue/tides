package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "MovieScreeningPriceStatusUpdateDto", description = "Movie screening price status update")
public class MovieScreeningPriceStatusUpdateDto {

    @Schema(name = "screeningId", type = "Long", description = "screening id")
    @NotNull
    private Long screeningId;

    @Schema(name = "priceId", type = "Long", description = "price id")
    @NotNull
    private Long priceId;

    @Schema(name = "status", type = "Integer", description = "status")
    @NotNull
    private Integer status;
}
