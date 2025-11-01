package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(title = "MovieMediaVo", description = "Movie media")
public class MovieMediaVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long movieId;

    private Integer mediaType;

    private String title;

    private String coverUrl;

    private String mediaUrl;

    private Integer sortOrder;
}
