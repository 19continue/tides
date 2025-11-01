package com.tides.service;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tides.entity.MovieScreening;
import com.tides.enums.BusinessStatus;
import com.tides.mapper.MovieScreeningMapper;
import com.tides.util.DateUtils;
import com.tides.vo.TimeRenewalResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class MovieScreeningTimeRenewalService {

    private static final int RENEWAL_THRESHOLD_DAYS = 2;

    private static final int RENEWAL_MONTHS = 1;

    @Autowired
    private MovieScreeningMapper movieScreeningMapper;

    @Transactional(rollbackFor = Exception.class)
    public TimeRenewalResultVo renewal() {
        Date now = DateUtils.now();
        List<MovieScreening> movieScreeningList = movieScreeningMapper.selectList(
                Wrappers.lambdaQuery(MovieScreening.class)
                        .le(MovieScreening::getShowTime, DateUtils.addDay(now, RENEWAL_THRESHOLD_DAYS))
                        .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode())
                        .orderByAsc(MovieScreening::getShowTime));
        List<Long> screeningIdList = new ArrayList<>();
        Set<Long> programIdSet = new LinkedHashSet<>();
        if (CollectionUtil.isEmpty(movieScreeningList)) {
            return buildResult(screeningIdList, programIdSet);
        }
        for (MovieScreening movieScreening : movieScreeningList) {
            if (Objects.isNull(movieScreening.getShowTime())) {
                continue;
            }
            Date newShowTime = getRenewalShowTime(movieScreening.getShowTime(), now);
            long offsetMillis = newShowTime.getTime() - movieScreening.getShowTime().getTime();

            MovieScreening updateMovieScreening = new MovieScreening();
            updateMovieScreening.setShowTime(newShowTime);
            updateMovieScreening.setShowDayTime(DateUtils.parseDateTime(DateUtils.formatDate(newShowTime) + " 00:00:00"));
            updateMovieScreening.setShowWeekTime(DateUtils.getWeekStr(newShowTime));
            updateMovieScreening.setEndTime(offsetDate(movieScreening.getEndTime(), offsetMillis));
            updateMovieScreening.setStopSellTime(offsetDate(movieScreening.getStopSellTime(), offsetMillis));
            updateMovieScreening.setEditTime(now);

            int updateCount = movieScreeningMapper.update(updateMovieScreening,
                    Wrappers.lambdaUpdate(MovieScreening.class)
                            .eq(MovieScreening::getId, movieScreening.getId())
                            .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode()));
            if (updateCount > 0) {
                screeningIdList.add(movieScreening.getId());
                if (Objects.nonNull(movieScreening.getProgramId())) {
                    programIdSet.add(movieScreening.getProgramId());
                }
                log.info("电影场次时间续期 screeningId : {}, oldShowTime : {}, newShowTime : {}",
                        movieScreening.getId(), movieScreening.getShowTime(), newShowTime);
            }
        }
        return buildResult(screeningIdList, programIdSet);
    }

    private Date getRenewalShowTime(Date showTime, Date now) {
        Date newShowTime = DateUtils.addMonth(showTime, RENEWAL_MONTHS);
        while (!newShowTime.after(now)) {
            newShowTime = DateUtils.addMonth(newShowTime, RENEWAL_MONTHS);
        }
        return newShowTime;
    }

    private Date offsetDate(Date date, long offsetMillis) {
        if (Objects.isNull(date)) {
            return null;
        }
        return new Date(date.getTime() + offsetMillis);
    }

    private TimeRenewalResultVo buildResult(List<Long> screeningIdList, Set<Long> programIdSet) {
        TimeRenewalResultVo resultVo = new TimeRenewalResultVo();
        resultVo.setRenewalCount(screeningIdList.size());
        resultVo.setScreeningIdList(screeningIdList);
        resultVo.setProgramIdList(new ArrayList<>(programIdSet));
        return resultVo;
    }
}
