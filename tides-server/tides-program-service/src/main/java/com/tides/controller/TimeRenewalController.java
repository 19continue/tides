package com.tides.controller;

import com.tides.common.ApiResponse;
import com.tides.service.MovieScreeningTimeRenewalService;
import com.tides.service.init.MovieElasticsearchInitData;
import com.tides.service.init.ProgramElasticsearchInitData;
import com.tides.service.init.ProgramShowTimeRenewal;
import com.tides.vo.TimeRenewalResultVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/program/time/renewal")
@Tag(name = "program/time/renewal", description = "演出和电影时间续期")
public class TimeRenewalController {

    @Autowired
    private ProgramShowTimeRenewal programShowTimeRenewal;

    @Autowired
    private ProgramElasticsearchInitData programElasticsearchInitData;

    @Autowired
    private MovieScreeningTimeRenewalService movieScreeningTimeRenewalService;

    @Autowired
    private MovieElasticsearchInitData movieElasticsearchInitData;

    @Operation(summary = "执行演出时间续期")
    @PostMapping(value = "/program/execute")
    public ApiResponse<TimeRenewalResultVo> programRenewal() {
        Set<Long> programIdSet = programShowTimeRenewal.renewal();
        TimeRenewalResultVo resultVo = new TimeRenewalResultVo();
        resultVo.setProgramIdList(new ArrayList<>(programIdSet));
        resultVo.setScreeningIdList(new ArrayList<>());
        resultVo.setRenewalCount(programIdSet.size());
        if (!programIdSet.isEmpty()) {
            rebuildProgramSearchIndex();
        }
        return ApiResponse.ok(resultVo);
    }

    @Operation(summary = "执行电影场次时间续期")
    @PostMapping(value = "/movie/execute")
    public ApiResponse<TimeRenewalResultVo> movieRenewal() {
        TimeRenewalResultVo resultVo = movieScreeningTimeRenewalService.renewal();
        if (resultVo.getRenewalCount() > 0) {
            rebuildMovieSearchIndex();
        }
        return ApiResponse.ok(resultVo);
    }

    private void rebuildProgramSearchIndex() {
        try {
            programElasticsearchInitData.initElasticsearchData();
        } catch (Exception exception) {
            log.error("program time renewal rebuild es error", exception);
        }
    }

    private void rebuildMovieSearchIndex() {
        try {
            movieElasticsearchInitData.initElasticsearchData();
        } catch (Exception exception) {
            log.error("movie time renewal rebuild es error", exception);
        }
    }
}
