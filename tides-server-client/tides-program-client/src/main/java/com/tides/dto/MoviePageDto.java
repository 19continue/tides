package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MoviePageDto", description = "Movie page query")
public class MoviePageDto extends BasePageDto {

    @Schema(name = "areaId", type = "Long", description = "city or district id")
    private Long areaId;

    @Schema(name = "content", type = "String", description = "movie name, alias, director or actor keyword, at least 2 characters")
    private String content;

    @Schema(name = "programCategoryId", type = "Long", description = "movie child category id")
    private Long programCategoryId;

    @Schema(name = "releaseStatus", type = "Integer", description = "1 presale, 2 on show, 3 off show")
    private Integer releaseStatus;
}
