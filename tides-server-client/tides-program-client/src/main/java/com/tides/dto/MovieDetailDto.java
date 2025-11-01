package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "MovieDetailDto", description = "Movie detail query")
public class MovieDetailDto {

    @Schema(name = "programId", type = "Long", description = "program id")
    private Long programId;

    @Schema(name = "movieId", type = "Long", description = "movie id")
    private Long movieId;

    @Schema(name = "areaId", type = "Long", description = "city or district id")
    private Long areaId;
}
