package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(title = "MovieScreeningPriceSaveDto", description = "Movie screening price create or update")
public class MovieScreeningPriceSaveDto {

    @Schema(name = "id", type = "Long", description = "screening price id")
    private Long id;

    @Schema(name = "screeningId", type = "Long", description = "screening id")
    @NotNull
    private Long screeningId;

    @Schema(name = "ticketCategoryId", type = "Long", description = "compatible ticket category id")
    @NotNull
    private Long ticketCategoryId;

    @Schema(name = "priceName", type = "String", description = "price name")
    private String priceName;

    @Schema(name = "price", type = "BigDecimal", description = "price")
    @NotNull
    private BigDecimal price;

    @Schema(name = "totalNumber", type = "Long", description = "total number")
    @NotNull
    private Long totalNumber;

    @Schema(name = "remainNumber", type = "Long", description = "remain number")
    @NotNull
    private Long remainNumber;
}
