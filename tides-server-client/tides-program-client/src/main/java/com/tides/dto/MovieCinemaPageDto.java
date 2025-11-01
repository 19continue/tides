package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MovieCinemaPageDto", description = "Movie cinema page query")
public class MovieCinemaPageDto extends BasePageDto {

    @Schema(name = "areaId", type = "Long", description = "city or area id")
    private Long areaId;

    @Schema(name = "cinemaName", type = "String", description = "cinema name")
    private String cinemaName;

    @Schema(name = "brandName", type = "String", description = "brand name")
    private String brandName;

    @Schema(name = "businessArea", type = "String", description = "business area")
    private String businessArea;

    @Schema(name = "programId", type = "Long", description = "program id")
    private Long programId;

    @Schema(name = "movieId", type = "Long", description = "movie id")
    private Long movieId;

    @Schema(name = "showDayTime", type = "Date", description = "show day")
    private Date showDayTime;
}
