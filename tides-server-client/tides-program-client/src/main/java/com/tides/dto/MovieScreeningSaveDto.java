package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(title = "MovieScreeningSaveDto", description = "Movie screening create or update")
public class MovieScreeningSaveDto {

    @Schema(name = "id", type = "Long", description = "screening id")
    private Long id;

    @Schema(name = "movieId", type = "Long", description = "movie id")
    @NotNull
    private Long movieId;

    @Schema(name = "programId", type = "Long", description = "program id")
    @NotNull
    private Long programId;

    @Schema(name = "cinemaId", type = "Long", description = "cinema id")
    @NotNull
    private Long cinemaId;

    @Schema(name = "hallId", type = "Long", description = "hall id")
    @NotNull
    private Long hallId;

    @Schema(name = "showTime", type = "Date", description = "show start time")
    @NotNull
    private Date showTime;

    @Schema(name = "showDayTime", type = "Date", description = "show day")
    private Date showDayTime;

    @Schema(name = "showWeekTime", type = "String", description = "show week")
    private String showWeekTime;

    @Schema(name = "endTime", type = "Date", description = "show end time")
    private Date endTime;

    @Schema(name = "language", type = "String", description = "language")
    private String language;

    @Schema(name = "version", type = "String", description = "version")
    private String version;

    @Schema(name = "lowestPrice", type = "BigDecimal", description = "lowest price")
    private BigDecimal lowestPrice;

    @Schema(name = "stopSellTime", type = "Date", description = "stop sell time")
    private Date stopSellTime;

    @Schema(name = "screeningStatus", type = "Integer", description = "screening status")
    private Integer screeningStatus;
}
