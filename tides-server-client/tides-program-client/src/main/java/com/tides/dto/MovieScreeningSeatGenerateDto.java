package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(title = "MovieScreeningSeatGenerateDto", description = "Generate movie screening seats from hall template")
public class MovieScreeningSeatGenerateDto {

    @Schema(name = "screeningId", type = "Long", description = "screening id")
    @NotNull
    private Long screeningId;

    @Schema(name = "ticketCategoryId", type = "Long", description = "ticket category id")
    @NotNull
    private Long ticketCategoryId;

    @Schema(name = "priceName", type = "String", description = "price name")
    private String priceName;

    @Schema(name = "price", type = "BigDecimal", description = "seat price")
    @NotNull
    private BigDecimal price;
}
