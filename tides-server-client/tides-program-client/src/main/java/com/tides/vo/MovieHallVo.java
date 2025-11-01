package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(title = "MovieHallVo", description = "Movie hall")
public class MovieHallVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long cinemaId;

    private String hallName;

    private String hallType;

    private String screenType;

    private String soundType;

    private String screenSize;

    private Integer rowCount;

    private Integer colCount;

    private Integer seatCount;

    private String hallTags;

    private Integer status;
}
