package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MovieMediaManageDto", description = "Movie media manage query")
public class MovieMediaManageDto extends BasePageDto {

    private Long mediaId;

    private Long movieId;

    private Integer mediaType;

    private Integer auditStatus;

    private Integer status;
}
