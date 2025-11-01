package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MovieProfileManageDto", description = "Movie profile manage query")
public class MovieProfileManageDto extends BasePageDto {

    private Long profileId;

    private Long movieId;

    private Long programId;

    private Integer releaseStatus;

    private Integer status;
}
