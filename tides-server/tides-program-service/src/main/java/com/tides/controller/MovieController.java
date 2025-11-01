package com.tides.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tides.common.ApiResponse;
import com.tides.dto.MovieCinemaDetailDto;
import com.tides.dto.MovieCinemaMovieListDto;
import com.tides.dto.MovieCinemaPageDto;
import com.tides.dto.MovieCinemaScreeningListDto;
import com.tides.dto.MovieDetailDto;
import com.tides.dto.MoviePageDto;
import com.tides.dto.MovieScreeningListDto;
import com.tides.dto.MovieScreeningSeatDto;
import com.tides.service.MovieService;
import com.tides.vo.MovieCinemaDetailVo;
import com.tides.vo.MovieCinemaMovieVo;
import com.tides.vo.MovieCinemaVo;
import com.tides.vo.MovieDetailVo;
import com.tides.vo.MovieListVo;
import com.tides.vo.MovieScreeningVo;
import com.tides.vo.MovieSeatRelateInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/movie")
@Tag(name = "movie", description = "电影业务")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @Operation(summary = "查询电影分页")
    @PostMapping(value = "/page")
    public ApiResponse<IPage<MovieListVo>> page(@Valid @RequestBody MoviePageDto moviePageDto) {
        return ApiResponse.ok(movieService.page(moviePageDto));
    }

    @Operation(summary = "查询电影详情聚合信息")
    @PostMapping(value = "/detail")
    public ApiResponse<MovieDetailVo> detail(@Valid @RequestBody MovieDetailDto movieDetailDto) {
        return ApiResponse.ok(movieService.detail(movieDetailDto));
    }

    @Operation(summary = "查询可购影院分页")
    @PostMapping(value = "/cinema/page")
    public ApiResponse<IPage<MovieCinemaVo>> cinemaPage(@Valid @RequestBody MovieCinemaPageDto movieCinemaPageDto) {
        return ApiResponse.ok(movieService.cinemaPage(movieCinemaPageDto));
    }

    @Operation(summary = "查询影院详情")
    @PostMapping(value = "/cinema/detail")
    public ApiResponse<MovieCinemaDetailVo> cinemaDetail(
            @Valid @RequestBody MovieCinemaDetailDto movieCinemaDetailDto) {
        return ApiResponse.ok(movieService.cinemaDetail(movieCinemaDetailDto));
    }

    @Operation(summary = "查询影院下可购影片")
    @PostMapping(value = "/cinema/movie/list")
    public ApiResponse<List<MovieCinemaMovieVo>> cinemaMovieList(
            @Valid @RequestBody MovieCinemaMovieListDto movieCinemaMovieListDto) {
        return ApiResponse.ok(movieService.cinemaMovieList(movieCinemaMovieListDto));
    }

    @Operation(summary = "查询影院下电影放映场次")
    @PostMapping(value = "/cinema/screening/list")
    public ApiResponse<List<MovieScreeningVo>> cinemaScreeningList(
            @Valid @RequestBody MovieCinemaScreeningListDto movieCinemaScreeningListDto) {
        return ApiResponse.ok(movieService.cinemaScreeningList(movieCinemaScreeningListDto));
    }

    @Operation(summary = "查询电影放映场次")
    @PostMapping(value = "/screening/list")
    public ApiResponse<List<MovieScreeningVo>> screeningList(
            @Valid @RequestBody MovieScreeningListDto movieScreeningListDto) {
        return ApiResponse.ok(movieService.screeningList(movieScreeningListDto));
    }

    @Operation(summary = "查询电影场次座位")
    @PostMapping(value = "/screening/seat/info")
    public ApiResponse<MovieSeatRelateInfoVo> screeningSeatInfo(
            @Valid @RequestBody MovieScreeningSeatDto movieScreeningSeatDto) {
        return ApiResponse.ok(movieService.screeningSeatInfo(movieScreeningSeatDto));
    }
}
