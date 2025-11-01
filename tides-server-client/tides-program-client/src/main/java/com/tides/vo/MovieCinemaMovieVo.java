package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(title = "MovieCinemaMovieVo", description = "Cinema movie")
public class MovieCinemaMovieVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long movieId;

    private Long programId;

    private String movieName;

    private String poster;

    private Integer durationMinutes;

    private String language;

    private String region;

    private Date releaseDate;

    private BigDecimal lowestPrice;

    private Date nearestShowTime;

    private Integer screeningCount;
}
