package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description: Movie inventory reconcile dto
 */
@Data
@Schema(title = "MovieInventoryReconcileDto", description = "Movie inventory reconcile")
public class MovieInventoryReconcileDto {

    @Schema(name = "screeningId", type = "Long", description = "screening id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long screeningId;
}
