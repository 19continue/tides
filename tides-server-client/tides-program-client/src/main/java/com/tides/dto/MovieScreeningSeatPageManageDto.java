package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: Movie screening seat manage page dto
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MovieScreeningSeatPageManageDto", description = "Movie screening seat manage page")
public class MovieScreeningSeatPageManageDto extends BasePageDto {

    @Schema(name = "screeningId", type = "Long", description = "screening id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long screeningId;

    @Schema(name = "ticketCategoryId", type = "Long", description = "ticket category id")
    private Long ticketCategoryId;
}
