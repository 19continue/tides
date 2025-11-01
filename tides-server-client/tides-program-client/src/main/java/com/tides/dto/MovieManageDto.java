package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MovieManageDto", description = "Movie manage query")
public class MovieManageDto extends BasePageDto {

    @Schema(name = "areaId", type = "Long", description = "area id")
    private Long areaId;

    @Schema(name = "areaIds", type = "Long[]", description = "area id list")
    private List<Long> areaIds;

    @Schema(name = "movieId", type = "Long", description = "movie id")
    private Long movieId;

    @Schema(name = "programId", type = "Long", description = "program id")
    private Long programId;

    @Schema(name = "movieName", type = "String", description = "movie name")
    private String movieName;

    @Schema(name = "status", type = "Integer", description = "status")
    private Integer status;
}
