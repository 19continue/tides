package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MovieCinemaDetailVo", description = "Movie cinema detail")
public class MovieCinemaDetailVo extends MovieCinemaVo {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<MovieHallVo> hallList;

    private List<MovieCinemaMovieVo> movieList;
}
