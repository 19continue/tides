package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "HallSaveDto", description = "Hall create or update")
public class HallSaveDto {

    @Schema(name = "id", type = "Long", description = "hall id")
    private Long id;

    @Schema(name = "cinemaId", type = "Long", description = "cinema id")
    @NotNull
    private Long cinemaId;

    @Schema(name = "hallName", type = "String", description = "hall name")
    @NotBlank
    private String hallName;

    @Schema(name = "hallType", type = "String", description = "hall type")
    private String hallType;

    @Schema(name = "screenType", type = "String", description = "screen type")
    private String screenType;

    @Schema(name = "soundType", type = "String", description = "sound type")
    private String soundType;

    @Schema(name = "screenSize", type = "String", description = "screen size")
    private String screenSize;

    @Schema(name = "rowCount", type = "Integer", description = "row count")
    private Integer rowCount;

    @Schema(name = "colCount", type = "Integer", description = "column count")
    private Integer colCount;

    @Schema(name = "seatCount", type = "Integer", description = "seat count")
    private Integer seatCount;

    @Schema(name = "hallTags", type = "String", description = "hall tags")
    private String hallTags;
}
