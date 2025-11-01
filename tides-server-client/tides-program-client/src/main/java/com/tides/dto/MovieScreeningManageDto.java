package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @description: Movie screening manage query dto
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MovieScreeningManageDto", description = "Movie screening manage query")
public class MovieScreeningManageDto extends BasePageDto {

    @Schema(name = "areaId", type = "Long", description = "area id")
    private Long areaId;

    @Schema(name = "areaIds", type = "Long[]", description = "area id list")
    private List<Long> areaIds;

    @Schema(name = "screeningId", type = "Long", description = "screening id")
    private Long screeningId;

    @Schema(name = "movieId", type = "Long", description = "movie id")
    private Long movieId;

    @Schema(name = "programId", type = "Long", description = "program id")
    private Long programId;

    @Schema(name = "cinemaId", type = "Long", description = "cinema id")
    private Long cinemaId;

    @Schema(name = "screeningStatus", type = "Integer", description = "screening status")
    private Integer screeningStatus;
}
