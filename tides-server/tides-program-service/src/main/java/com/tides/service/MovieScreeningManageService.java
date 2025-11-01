package com.tides.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tides.client.OrderClient;
import com.tides.common.ApiResponse;
import com.tides.core.RedisKeyManage;
import com.tides.dto.MovieInventoryEventManageDto;
import com.tides.dto.MovieInventoryReconcileDto;
import com.tides.dto.MovieInventoryReconcileIssueHandleDto;
import com.tides.dto.MovieInventoryReconcileIssueManageDto;
import com.tides.dto.MovieOrderSalesCountDto;
import com.tides.dto.MovieScreeningManageDto;
import com.tides.dto.MovieScreeningPriceManageDto;
import com.tides.dto.MovieScreeningPriceSaveDto;
import com.tides.dto.MovieScreeningPriceStatusUpdateDto;
import com.tides.dto.MovieScreeningSaveDto;
import com.tides.dto.MovieScreeningSeatGenerateDto;
import com.tides.dto.MovieScreeningSeatPageManageDto;
import com.tides.dto.MovieScreeningSeatStatusUpdateDto;
import com.tides.dto.MovieScreeningStatusUpdateDto;
import com.tides.entity.Cinema;
import com.tides.entity.Hall;
import com.tides.entity.HallSeat;
import com.tides.entity.Movie;
import com.tides.entity.MovieInventoryEvent;
import com.tides.entity.MovieInventoryReconcileIssue;
import com.tides.entity.MovieScreening;
import com.tides.entity.MovieScreeningPrice;
import com.tides.entity.MovieScreeningSeat;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.enums.SeatType;
import com.tides.enums.SellStatus;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.CinemaMapper;
import com.tides.mapper.HallMapper;
import com.tides.mapper.HallSeatMapper;
import com.tides.mapper.MovieInventoryEventMapper;
import com.tides.mapper.MovieInventoryReconcileIssueMapper;
import com.tides.mapper.MovieMapper;
import com.tides.mapper.MovieScreeningMapper;
import com.tides.mapper.MovieScreeningPriceMapper;
import com.tides.mapper.MovieScreeningSeatMapper;
import com.tides.page.PageUtil;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.util.DateUtils;
import com.tides.util.StringUtil;
import com.tides.vo.MovieInventoryEventManageVo;
import com.tides.vo.MovieInventoryReconcileItemVo;
import com.tides.vo.MovieInventoryReconcileIssueManageVo;
import com.tides.vo.MovieInventoryReconcileVo;
import com.tides.vo.MovieOrderSalesCountVo;
import com.tides.vo.MovieScreeningManageVo;
import com.tides.vo.MovieScreeningPriceManageVo;
import com.tides.vo.MovieScreeningSeatManageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 电影排期库存后台管理 service，负责排片、票价、场次座位、库存流水和对账工单。
 */
@Slf4j
@Service
public class MovieScreeningManageService  {
    private static final String INVENTORY_EVENT_ADMIN_CHANGE = "ADMIN_CHANGE";

    private static final String INVENTORY_SOURCE_ADMIN_MANUAL = "ADMIN_MANUAL";

    private static final int RECONCILE_ISSUE_OPEN = 1;

    private static final int RECONCILE_ISSUE_PROCESSED = 2;

    private static final int RECONCILE_ISSUE_IGNORED = 3;

    private static final String RECONCILE_ISSUE_TYPE_REMAIN = "REMAIN";

    private static final String RECONCILE_ISSUE_TYPE_REDIS_SEAT = "REDIS_SEAT";

    private static final String RECONCILE_ISSUE_TYPE_MIXED = "MIXED";

    @Autowired
    private UidGenerator uidGenerator;


    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private MovieScreeningMapper movieScreeningMapper;

    @Autowired
    private MovieScreeningPriceMapper movieScreeningPriceMapper;

    @Autowired
    private MovieScreeningSeatMapper movieScreeningSeatMapper;

    @Autowired
    private MovieInventoryEventMapper movieInventoryEventMapper;

    @Autowired
    private MovieInventoryReconcileIssueMapper movieInventoryReconcileIssueMapper;

    @Autowired
    private CinemaMapper cinemaMapper;

    @Autowired
    private HallMapper hallMapper;

    @Autowired
    private HallSeatMapper hallSeatMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private OrderClient orderClient;
    public IPage<MovieScreeningManageVo> movieScreeningPage(MovieScreeningManageDto movieScreeningManageDto) {
        IPage<MovieScreeningManageVo> movieScreeningManageVoPage =
                new Page<>(movieScreeningManageDto.getPageNumber(), movieScreeningManageDto.getPageSize());
        Set<Long> cinemaIdSet = selectCinemaIdSetByArea(movieScreeningManageDto.getAreaId(),
                movieScreeningManageDto.getAreaIds());
        if (hasAreaFilter(movieScreeningManageDto.getAreaId(), movieScreeningManageDto.getAreaIds()) &&
                CollectionUtil.isEmpty(cinemaIdSet)) {
            return movieScreeningManageVoPage;
        }
        IPage<MovieScreening> movieScreeningPage =
                movieScreeningMapper.selectPage(PageUtil.getPageParams(movieScreeningManageDto.getPageNumber(),
                        movieScreeningManageDto.getPageSize()), Wrappers.lambdaQuery(MovieScreening.class)
                        .in(CollectionUtil.isNotEmpty(cinemaIdSet),
                                MovieScreening::getCinemaId, cinemaIdSet)
                        .eq(Objects.nonNull(movieScreeningManageDto.getScreeningId()),
                                MovieScreening::getId, movieScreeningManageDto.getScreeningId())
                        .eq(Objects.nonNull(movieScreeningManageDto.getMovieId()),
                                MovieScreening::getMovieId, movieScreeningManageDto.getMovieId())
                        .eq(Objects.nonNull(movieScreeningManageDto.getProgramId()),
                                MovieScreening::getProgramId, movieScreeningManageDto.getProgramId())
                        .eq(Objects.nonNull(movieScreeningManageDto.getCinemaId()),
                                MovieScreening::getCinemaId, movieScreeningManageDto.getCinemaId())
                        .eq(Objects.nonNull(movieScreeningManageDto.getScreeningStatus()),
                                MovieScreening::getScreeningStatus, movieScreeningManageDto.getScreeningStatus())
                        .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode())
                        .orderByDesc(MovieScreening::getShowTime));
        if (CollectionUtil.isEmpty(movieScreeningPage.getRecords())) {
            return movieScreeningManageVoPage;
        }
        Map<Long, Movie> movieMap = selectMovieMap(movieScreeningPage.getRecords());
        Map<Long, Cinema> cinemaMap = selectCinemaMap(movieScreeningPage.getRecords());
        Map<Long, Hall> hallMap = selectHallMap(movieScreeningPage.getRecords());
        List<Long> screeningIdList = movieScreeningPage.getRecords().stream().map(MovieScreening::getId).toList();
        Map<Long, ScreeningInventoryAggregate> screeningInventoryAggregateMap =
                selectScreeningInventoryAggregateMap(screeningIdList);
        Map<Long, MovieOrderSalesCountVo> screeningSalesMap = selectScreeningSalesMap(movieScreeningPage.getRecords());
        List<MovieScreeningManageVo> movieScreeningManageVoList = movieScreeningPage.getRecords().stream()
                .map(movieScreening -> {
                    MovieScreeningManageVo movieScreeningManageVo = new MovieScreeningManageVo();
                    BeanUtil.copyProperties(movieScreening, movieScreeningManageVo);
                    Movie movie = movieMap.get(movieScreening.getMovieId());
                    if (Objects.nonNull(movie)) {
                        movieScreeningManageVo.setMovieName(movie.getMovieName());
                        movieScreeningManageVo.setPoster(movie.getPoster());
                    }
                    Cinema cinema = cinemaMap.get(movieScreening.getCinemaId());
                    if (Objects.nonNull(cinema)) {
                        movieScreeningManageVo.setAreaId(cinema.getAreaId());
                        movieScreeningManageVo.setCinemaName(cinema.getCinemaName());
                        movieScreeningManageVo.setCinemaAddress(cinema.getAddress());
                        movieScreeningManageVo.setCityName(cinema.getCityName());
                        movieScreeningManageVo.setDistrictName(cinema.getDistrictName());
                    }
                    Hall hall = hallMap.get(movieScreening.getHallId());
                    if (Objects.nonNull(hall)) {
                        movieScreeningManageVo.setHallName(hall.getHallName());
                        movieScreeningManageVo.setHallType(hall.getHallType());
                    }
                    ScreeningInventoryAggregate inventoryAggregate =
                            screeningInventoryAggregateMap.get(movieScreening.getId());
                    Long totalNumber = Optional.ofNullable(inventoryAggregate).map(ScreeningInventoryAggregate::totalNumber)
                            .orElse(0L);
                    Long remainNumber = Optional.ofNullable(inventoryAggregate).map(ScreeningInventoryAggregate::remainNumber)
                            .orElse(0L);
                    Long lockedNumber = Optional.ofNullable(inventoryAggregate).map(ScreeningInventoryAggregate::lockedNumber)
                            .orElse(0L);
                    Long soldSeatNumber = Optional.ofNullable(inventoryAggregate).map(ScreeningInventoryAggregate::soldSeatNumber)
                            .orElse(0L);
                    MovieOrderSalesCountVo salesCountVo = screeningSalesMap.get(movieScreening.getId());
                    Long paidOrderCount = Optional.ofNullable(salesCountVo)
                            .map(MovieOrderSalesCountVo::getPaidOrderCount).orElse(0L);
                    Long paidTicketCount = Optional.ofNullable(salesCountVo)
                            .map(MovieOrderSalesCountVo::getPaidTicketCount).orElse(0L);
                    movieScreeningManageVo.setTotalNumber(totalNumber);
                    movieScreeningManageVo.setDbRemainNumber(remainNumber);
                    movieScreeningManageVo.setSoldNumber(paidTicketCount);
                    movieScreeningManageVo.setSoldSeatNumber(soldSeatNumber);
                    movieScreeningManageVo.setLockedNumber(lockedNumber);
                    movieScreeningManageVo.setOccupiedNumber(Math.max(0L, totalNumber - remainNumber));
                    movieScreeningManageVo.setSoldOut(totalNumber > 0 && remainNumber <= 0);
                    movieScreeningManageVo.setPaidOrderCount(paidOrderCount);
                    movieScreeningManageVo.setPaidTicketCount(paidTicketCount);
                    return movieScreeningManageVo;
                }).toList();
        BeanUtils.copyProperties(movieScreeningPage, movieScreeningManageVoPage);
        movieScreeningManageVoPage.setRecords(movieScreeningManageVoList);
        return movieScreeningManageVoPage;
    }

    public List<MovieScreeningPriceManageVo> movieScreeningPriceList(MovieScreeningPriceManageDto movieScreeningPriceManageDto) {
        List<MovieScreeningPrice> movieScreeningPriceList = movieScreeningPriceMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningPrice.class)
                        .eq(MovieScreeningPrice::getScreeningId, movieScreeningPriceManageDto.getScreeningId())
                        .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode())
                        .orderByAsc(MovieScreeningPrice::getPrice));
        return movieScreeningPriceList.stream().map(movieScreeningPrice -> {
            MovieScreeningPriceManageVo movieScreeningPriceManageVo = new MovieScreeningPriceManageVo();
            BeanUtil.copyProperties(movieScreeningPrice, movieScreeningPriceManageVo);
            movieScreeningPriceManageVo.setDbRemainNumber(movieScreeningPrice.getRemainNumber());
            Map<String, Long> ticketCategoryRemainNumber =
                    redisCache.getAllMapForHash(RedisKeyBuild.createRedisKey(
                            RedisKeyManage.MOVIE_SCREENING_TICKET_REMAIN_NUMBER_HASH_RESOLUTION,
                            movieScreeningPrice.getScreeningId(), movieScreeningPrice.getTicketCategoryId()), Long.class);
            if (CollectionUtil.isNotEmpty(ticketCategoryRemainNumber)) {
                movieScreeningPriceManageVo.setRedisRemainNumber(
                        ticketCategoryRemainNumber.get(String.valueOf(movieScreeningPrice.getTicketCategoryId())));
            }
            return movieScreeningPriceManageVo;
        }).toList();
    }

    public IPage<MovieScreeningSeatManageVo> movieScreeningSeatPage(MovieScreeningSeatPageManageDto movieScreeningSeatPageManageDto) {
        IPage<MovieScreeningSeatManageVo> movieScreeningSeatManageVoPage =
                new Page<>(movieScreeningSeatPageManageDto.getPageNumber(), movieScreeningSeatPageManageDto.getPageSize());
        IPage<MovieScreeningSeat> movieScreeningSeatPage =
                movieScreeningSeatMapper.selectPage(PageUtil.getPageParams(movieScreeningSeatPageManageDto.getPageNumber(),
                        movieScreeningSeatPageManageDto.getPageSize()), Wrappers.lambdaQuery(MovieScreeningSeat.class)
                        .eq(MovieScreeningSeat::getScreeningId, movieScreeningSeatPageManageDto.getScreeningId())
                        .eq(Objects.nonNull(movieScreeningSeatPageManageDto.getTicketCategoryId()),
                                MovieScreeningSeat::getTicketCategoryId, movieScreeningSeatPageManageDto.getTicketCategoryId())
                        .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode())
                        .orderByAsc(MovieScreeningSeat::getRowCode)
                        .orderByAsc(MovieScreeningSeat::getColCode));
        if (CollectionUtil.isEmpty(movieScreeningSeatPage.getRecords())) {
            return movieScreeningSeatManageVoPage;
        }
        Map<Long, List<MovieScreeningSeat>> seatMap = movieScreeningSeatPage.getRecords().stream()
                .collect(Collectors.groupingBy(MovieScreeningSeat::getTicketCategoryId));
        Map<Long, MovieScreeningSeat> redisSeatMap = new HashMap<>(movieScreeningSeatPage.getRecords().size());
        for (Entry<Long, List<MovieScreeningSeat>> entry : seatMap.entrySet()) {
            Long ticketCategoryId = entry.getKey();
            List<String> seatIdList = entry.getValue().stream().map(MovieScreeningSeat::getId).map(String::valueOf).toList();
            putMovieRedisSeat(redisSeatMap, RedisKeyManage.MOVIE_SCREENING_SEAT_NO_SOLD_RESOLUTION_HASH,
                    movieScreeningSeatPageManageDto.getScreeningId(), ticketCategoryId, seatIdList);
            putMovieRedisSeat(redisSeatMap, RedisKeyManage.MOVIE_SCREENING_SEAT_LOCK_RESOLUTION_HASH,
                    movieScreeningSeatPageManageDto.getScreeningId(), ticketCategoryId, seatIdList);
            putMovieRedisSeat(redisSeatMap, RedisKeyManage.MOVIE_SCREENING_SEAT_SOLD_RESOLUTION_HASH,
                    movieScreeningSeatPageManageDto.getScreeningId(), ticketCategoryId, seatIdList);
        }
        List<MovieScreeningSeatManageVo> movieScreeningSeatManageVoList = new ArrayList<>();
        for (MovieScreeningSeat movieScreeningSeat : movieScreeningSeatPage.getRecords()) {
            MovieScreeningSeatManageVo movieScreeningSeatManageVo = new MovieScreeningSeatManageVo();
            BeanUtil.copyProperties(movieScreeningSeat, movieScreeningSeatManageVo);
            movieScreeningSeatManageVo.setSeatTypeName(SeatType.getMsg(movieScreeningSeat.getSeatType()));
            movieScreeningSeatManageVo.setDbSellStatus(movieScreeningSeat.getSellStatus());
            movieScreeningSeatManageVo.setDbSellStatusName(SellStatus.getMsg(movieScreeningSeat.getSellStatus()));
            MovieScreeningSeat redisSeat = redisSeatMap.get(movieScreeningSeat.getId());
            if (Objects.nonNull(redisSeat)) {
                movieScreeningSeatManageVo.setRedisSellStatus(redisSeat.getSellStatus());
                movieScreeningSeatManageVo.setRedisSellStatusName(Optional.ofNullable(SellStatus.getMsg(redisSeat.getSellStatus()))
                        .filter(StringUtil::isNotEmpty).orElse(""));
            }
            movieScreeningSeatManageVoList.add(movieScreeningSeatManageVo);
        }
        BeanUtils.copyProperties(movieScreeningSeatPage, movieScreeningSeatManageVoPage);
        movieScreeningSeatManageVoPage.setRecords(movieScreeningSeatManageVoList);
        return movieScreeningSeatManageVoPage;
    }

    public IPage<MovieInventoryEventManageVo> movieInventoryEventPage(MovieInventoryEventManageDto movieInventoryEventManageDto) {
        IPage<MovieInventoryEventManageVo> movieInventoryEventManageVoPage =
                new Page<>(movieInventoryEventManageDto.getPageNumber(), movieInventoryEventManageDto.getPageSize());
        IPage<MovieInventoryEvent> movieInventoryEventPage =
                movieInventoryEventMapper.selectPage(PageUtil.getPageParams(movieInventoryEventManageDto.getPageNumber(),
                        movieInventoryEventManageDto.getPageSize()), Wrappers.lambdaQuery(MovieInventoryEvent.class)
                        .eq(Objects.nonNull(movieInventoryEventManageDto.getScreeningId()),
                                MovieInventoryEvent::getScreeningId, movieInventoryEventManageDto.getScreeningId())
                        .eq(Objects.nonNull(movieInventoryEventManageDto.getTicketCategoryId()),
                                MovieInventoryEvent::getTicketCategoryId, movieInventoryEventManageDto.getTicketCategoryId())
                        .eq(Objects.nonNull(movieInventoryEventManageDto.getSeatId()),
                                MovieInventoryEvent::getSeatId, movieInventoryEventManageDto.getSeatId())
                        .eq(StringUtil.isNotEmpty(movieInventoryEventManageDto.getEventType()),
                                MovieInventoryEvent::getEventType, movieInventoryEventManageDto.getEventType())
                        .eq(StringUtil.isNotEmpty(movieInventoryEventManageDto.getSourceType()),
                                MovieInventoryEvent::getSourceType, movieInventoryEventManageDto.getSourceType())
                        .eq(StringUtil.isNotEmpty(movieInventoryEventManageDto.getBizNo()),
                                MovieInventoryEvent::getBizNo, movieInventoryEventManageDto.getBizNo())
                        .eq(MovieInventoryEvent::getStatus, BusinessStatus.YES.getCode())
                        .orderByDesc(MovieInventoryEvent::getEventTime)
                        .orderByDesc(MovieInventoryEvent::getId));
        if (CollectionUtil.isEmpty(movieInventoryEventPage.getRecords())) {
            return movieInventoryEventManageVoPage;
        }
        List<MovieInventoryEventManageVo> movieInventoryEventManageVoList =
                BeanUtil.copyToList(movieInventoryEventPage.getRecords(), MovieInventoryEventManageVo.class);
        for (MovieInventoryEventManageVo movieInventoryEventManageVo : movieInventoryEventManageVoList) {
            movieInventoryEventManageVo.setBeforeSellStatusName(SellStatus.getMsg(movieInventoryEventManageVo.getBeforeSellStatus()));
            movieInventoryEventManageVo.setAfterSellStatusName(SellStatus.getMsg(movieInventoryEventManageVo.getAfterSellStatus()));
        }
        BeanUtils.copyProperties(movieInventoryEventPage, movieInventoryEventManageVoPage);
        movieInventoryEventManageVoPage.setRecords(movieInventoryEventManageVoList);
        return movieInventoryEventManageVoPage;
    }

    public MovieInventoryReconcileVo movieInventoryReconcile(MovieInventoryReconcileDto movieInventoryReconcileDto) {
        MovieScreening movieScreening = validateScreeningExists(movieInventoryReconcileDto.getScreeningId());
        List<MovieScreeningPrice> movieScreeningPriceList = movieScreeningPriceMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningPrice.class)
                        .eq(MovieScreeningPrice::getScreeningId, movieInventoryReconcileDto.getScreeningId())
                        .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode())
                        .orderByAsc(MovieScreeningPrice::getTicketCategoryId));
        if (CollectionUtil.isEmpty(movieScreeningPriceList)) {
            throw new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2);
        }
        List<MovieScreeningSeat> movieScreeningSeatList = movieScreeningSeatMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningSeat.class)
                        .eq(MovieScreeningSeat::getScreeningId, movieInventoryReconcileDto.getScreeningId())
                        .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode()));
        Map<Long, Map<Integer, Long>> dbSeatStatusCountMap = movieScreeningSeatList.stream()
                .collect(Collectors.groupingBy(MovieScreeningSeat::getTicketCategoryId,
                        Collectors.groupingBy(MovieScreeningSeat::getSellStatus, Collectors.counting())));

        MovieInventoryReconcileVo movieInventoryReconcileVo = new MovieInventoryReconcileVo();
        movieInventoryReconcileVo.setScreeningId(movieScreening.getId());
        movieInventoryReconcileVo.setProgramId(movieScreening.getProgramId());
        movieInventoryReconcileVo.setMovieId(movieScreening.getMovieId());
        movieInventoryReconcileVo.setCinemaId(movieScreening.getCinemaId());
        movieInventoryReconcileVo.setHallId(movieScreening.getHallId());

        List<MovieInventoryReconcileItemVo> itemList = new ArrayList<>(movieScreeningPriceList.size());
        int issueCount = 0;
        for (MovieScreeningPrice movieScreeningPrice : movieScreeningPriceList) {
            MovieInventoryReconcileItemVo itemVo = buildInventoryReconcileItem(movieInventoryReconcileDto.getScreeningId(),
                    movieScreeningPrice, dbSeatStatusCountMap.get(movieScreeningPrice.getTicketCategoryId()));
            if (!Boolean.TRUE.equals(itemVo.getConsistent())) {
                issueCount++;
            }
            itemList.add(itemVo);
        }
        movieInventoryReconcileVo.setItemList(itemList);
        movieInventoryReconcileVo.setIssueCount(issueCount);
        movieInventoryReconcileVo.setConsistent(issueCount == 0);
        movieInventoryReconcileVo.setTotalDbRemainNumber(sumLong(itemList.stream()
                .map(MovieInventoryReconcileItemVo::getDbRemainNumber).toList()));
        movieInventoryReconcileVo.setTotalDbNoSoldCount(sumLong(itemList.stream()
                .map(MovieInventoryReconcileItemVo::getDbNoSoldCount).toList()));
        movieInventoryReconcileVo.setTotalDbLockCount(sumLong(itemList.stream()
                .map(MovieInventoryReconcileItemVo::getDbLockCount).toList()));
        movieInventoryReconcileVo.setTotalDbSoldCount(sumLong(itemList.stream()
                .map(MovieInventoryReconcileItemVo::getDbSoldCount).toList()));
        movieInventoryReconcileVo.setTotalRedisNoSoldCount(sumLong(itemList.stream()
                .map(MovieInventoryReconcileItemVo::getRedisNoSoldCount).toList()));
        movieInventoryReconcileVo.setTotalRedisLockCount(sumLong(itemList.stream()
                .map(MovieInventoryReconcileItemVo::getRedisLockCount).toList()));
        movieInventoryReconcileVo.setTotalRedisSoldCount(sumLong(itemList.stream()
                .map(MovieInventoryReconcileItemVo::getRedisSoldCount).toList()));
        upsertInventoryReconcileIssues(movieScreening, itemList);
        return movieInventoryReconcileVo;
    }

    public IPage<MovieInventoryReconcileIssueManageVo> movieInventoryReconcileIssuePage(
            MovieInventoryReconcileIssueManageDto movieInventoryReconcileIssueManageDto) {
        IPage<MovieInventoryReconcileIssueManageVo> movieInventoryReconcileIssueManageVoPage =
                new Page<>(movieInventoryReconcileIssueManageDto.getPageNumber(),
                        movieInventoryReconcileIssueManageDto.getPageSize());
        IPage<MovieInventoryReconcileIssue> movieInventoryReconcileIssuePage =
                movieInventoryReconcileIssueMapper.selectPage(PageUtil.getPageParams(
                                movieInventoryReconcileIssueManageDto.getPageNumber(),
                                movieInventoryReconcileIssueManageDto.getPageSize()),
                        Wrappers.lambdaQuery(MovieInventoryReconcileIssue.class)
                                .eq(Objects.nonNull(movieInventoryReconcileIssueManageDto.getScreeningId()),
                                        MovieInventoryReconcileIssue::getScreeningId,
                                        movieInventoryReconcileIssueManageDto.getScreeningId())
                                .eq(Objects.nonNull(movieInventoryReconcileIssueManageDto.getTicketCategoryId()),
                                        MovieInventoryReconcileIssue::getTicketCategoryId,
                                        movieInventoryReconcileIssueManageDto.getTicketCategoryId())
                                .eq(Objects.nonNull(movieInventoryReconcileIssueManageDto.getIssueStatus()),
                                        MovieInventoryReconcileIssue::getIssueStatus,
                                        movieInventoryReconcileIssueManageDto.getIssueStatus())
                                .eq(MovieInventoryReconcileIssue::getStatus, BusinessStatus.YES.getCode())
                                .orderByAsc(MovieInventoryReconcileIssue::getIssueStatus)
                                .orderByDesc(MovieInventoryReconcileIssue::getLastReconcileTime)
                                .orderByDesc(MovieInventoryReconcileIssue::getId));
        if (CollectionUtil.isEmpty(movieInventoryReconcileIssuePage.getRecords())) {
            return movieInventoryReconcileIssueManageVoPage;
        }
        List<MovieInventoryReconcileIssueManageVo> movieInventoryReconcileIssueManageVoList =
                BeanUtil.copyToList(movieInventoryReconcileIssuePage.getRecords(),
                        MovieInventoryReconcileIssueManageVo.class);
        for (MovieInventoryReconcileIssueManageVo movieInventoryReconcileIssueManageVo :
                movieInventoryReconcileIssueManageVoList) {
            movieInventoryReconcileIssueManageVo.setIssueStatusName(
                    reconcileIssueStatusName(movieInventoryReconcileIssueManageVo.getIssueStatus()));
        }
        BeanUtils.copyProperties(movieInventoryReconcileIssuePage, movieInventoryReconcileIssueManageVoPage);
        movieInventoryReconcileIssueManageVoPage.setRecords(movieInventoryReconcileIssueManageVoList);
        return movieInventoryReconcileIssueManageVoPage;
    }

    public Boolean movieInventoryReconcileIssueHandle(
            MovieInventoryReconcileIssueHandleDto movieInventoryReconcileIssueHandleDto) {
        if (!Objects.equals(movieInventoryReconcileIssueHandleDto.getIssueStatus(), RECONCILE_ISSUE_PROCESSED) &&
                !Objects.equals(movieInventoryReconcileIssueHandleDto.getIssueStatus(), RECONCILE_ISSUE_IGNORED)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        MovieInventoryReconcileIssue updateIssue = new MovieInventoryReconcileIssue();
        updateIssue.setIssueStatus(movieInventoryReconcileIssueHandleDto.getIssueStatus());
        updateIssue.setHandlerId(movieInventoryReconcileIssueHandleDto.getHandlerId());
        updateIssue.setHandleRemark(movieInventoryReconcileIssueHandleDto.getHandleRemark());
        updateIssue.setHandleTime(DateUtils.now());
        updateIssue.setEditTime(DateUtils.now());
        int updateCount = movieInventoryReconcileIssueMapper.update(updateIssue,
                Wrappers.lambdaUpdate(MovieInventoryReconcileIssue.class)
                        .eq(MovieInventoryReconcileIssue::getId, movieInventoryReconcileIssueHandleDto.getId())
                        .eq(MovieInventoryReconcileIssue::getScreeningId,
                                movieInventoryReconcileIssueHandleDto.getScreeningId())
                        .eq(MovieInventoryReconcileIssue::getStatus, BusinessStatus.YES.getCode()));
        return updateCount > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long movieScreeningSave(MovieScreeningSaveDto movieScreeningSaveDto) {
        Movie movie = validateScreeningMasterData(movieScreeningSaveDto);
        MovieScreening movieScreening = new MovieScreening();
        BeanUtil.copyProperties(movieScreeningSaveDto, movieScreening);
        fillScreeningTime(movieScreening, movie);
        if (Objects.isNull(movieScreening.getScreeningStatus())) {
            movieScreening.setScreeningStatus(BusinessStatus.YES.getCode());
        }
        movieScreening.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(movieScreening.getId())) {
            movieScreening.setId(uidGenerator.getUid());
            movieScreeningMapper.insert(movieScreening);
        } else {
            movieScreening.setEditTime(DateUtils.now());
            int updateCount = movieScreeningMapper.update(movieScreening, Wrappers.lambdaUpdate(MovieScreening.class)
                    .eq(MovieScreening::getId, movieScreening.getId())
                    .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode()));
            if (updateCount == 0) {
                throw new TidesFrameException(BaseCode.MOVIE_SCREENING_NOT_EXIST);
            }
        }
        return movieScreening.getId();
    }

    public Boolean movieScreeningStatusUpdate(MovieScreeningStatusUpdateDto movieScreeningStatusUpdateDto) {
        if (Objects.isNull(BusinessStatus.getRc(movieScreeningStatusUpdateDto.getScreeningStatus()))) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        MovieScreening movieScreening = new MovieScreening();
        movieScreening.setScreeningStatus(movieScreeningStatusUpdateDto.getScreeningStatus());
        movieScreening.setEditTime(DateUtils.now());
        int updateCount = movieScreeningMapper.update(movieScreening, Wrappers.lambdaUpdate(MovieScreening.class)
                .eq(MovieScreening::getId, movieScreeningStatusUpdateDto.getScreeningId())
                .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.MOVIE_SCREENING_NOT_EXIST);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long movieScreeningPriceSave(MovieScreeningPriceSaveDto movieScreeningPriceSaveDto) {
        validateScreeningExists(movieScreeningPriceSaveDto.getScreeningId());
        validatePriceNumber(movieScreeningPriceSaveDto.getTotalNumber(), movieScreeningPriceSaveDto.getRemainNumber());
        MovieScreeningPrice movieScreeningPrice = new MovieScreeningPrice();
        BeanUtil.copyProperties(movieScreeningPriceSaveDto, movieScreeningPrice);
        movieScreeningPrice.setStatus(BusinessStatus.YES.getCode());

        Set<Long> cacheTicketCategoryIdSet = new HashSet<>();
        cacheTicketCategoryIdSet.add(movieScreeningPriceSaveDto.getTicketCategoryId());
        MovieScreeningPrice existsMovieScreeningPrice = selectMovieScreeningPrice(movieScreeningPriceSaveDto.getScreeningId(),
                movieScreeningPriceSaveDto.getId(), movieScreeningPriceSaveDto.getTicketCategoryId());
        if (Objects.nonNull(existsMovieScreeningPrice)) {
            cacheTicketCategoryIdSet.add(existsMovieScreeningPrice.getTicketCategoryId());
            movieScreeningPrice.setId(existsMovieScreeningPrice.getId());
            movieScreeningPrice.setEditTime(DateUtils.now());
            movieScreeningPriceMapper.update(movieScreeningPrice, Wrappers.lambdaUpdate(MovieScreeningPrice.class)
                    .eq(MovieScreeningPrice::getScreeningId, movieScreeningPriceSaveDto.getScreeningId())
                    .eq(MovieScreeningPrice::getId, existsMovieScreeningPrice.getId()));
        } else {
            movieScreeningPrice.setId(uidGenerator.getUid());
            movieScreeningPriceMapper.insert(movieScreeningPrice);
        }
        updateScreeningSeatPrice(movieScreeningPriceSaveDto.getScreeningId(), movieScreeningPriceSaveDto.getTicketCategoryId(),
                movieScreeningPriceSaveDto.getPrice());
        clearScreeningRemainCache(movieScreeningPriceSaveDto.getScreeningId(), cacheTicketCategoryIdSet);
        clearScreeningSeatCache(movieScreeningPriceSaveDto.getScreeningId(), cacheTicketCategoryIdSet);
        return movieScreeningPrice.getId();
    }

    public Boolean movieScreeningPriceStatusUpdate(MovieScreeningPriceStatusUpdateDto movieScreeningPriceStatusUpdateDto) {
        if (Objects.isNull(BusinessStatus.getRc(movieScreeningPriceStatusUpdateDto.getStatus()))) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        MovieScreeningPrice dbMovieScreeningPrice = movieScreeningPriceMapper.selectOne(Wrappers.lambdaQuery(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, movieScreeningPriceStatusUpdateDto.getScreeningId())
                .eq(MovieScreeningPrice::getId, movieScreeningPriceStatusUpdateDto.getPriceId()));
        if (Objects.isNull(dbMovieScreeningPrice)) {
            throw new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2);
        }
        MovieScreeningPrice movieScreeningPrice = new MovieScreeningPrice();
        movieScreeningPrice.setStatus(movieScreeningPriceStatusUpdateDto.getStatus());
        movieScreeningPrice.setEditTime(DateUtils.now());
        movieScreeningPriceMapper.update(movieScreeningPrice, Wrappers.lambdaUpdate(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, movieScreeningPriceStatusUpdateDto.getScreeningId())
                .eq(MovieScreeningPrice::getId, movieScreeningPriceStatusUpdateDto.getPriceId()));
        clearScreeningRemainCache(movieScreeningPriceStatusUpdateDto.getScreeningId(),
                List.of(dbMovieScreeningPrice.getTicketCategoryId()));
        clearScreeningSeatCache(movieScreeningPriceStatusUpdateDto.getScreeningId(),
                List.of(dbMovieScreeningPrice.getTicketCategoryId()));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer movieScreeningSeatGenerate(MovieScreeningSeatGenerateDto movieScreeningSeatGenerateDto) {
        MovieScreening movieScreening = validateScreeningExists(movieScreeningSeatGenerateDto.getScreeningId());
        List<HallSeat> hallSeatList = hallSeatMapper.selectList(Wrappers.lambdaQuery(HallSeat.class)
                .eq(HallSeat::getCinemaId, movieScreening.getCinemaId())
                .eq(HallSeat::getHallId, movieScreening.getHallId())
                .eq(HallSeat::getSeatStatus, BusinessStatus.YES.getCode())
                .eq(HallSeat::getStatus, BusinessStatus.YES.getCode())
                .orderByAsc(HallSeat::getRowCode)
                .orderByAsc(HallSeat::getColCode));
        if (CollectionUtil.isEmpty(hallSeatList)) {
            return 0;
        }
        Set<String> existsSeatPositionSet = movieScreeningSeatMapper.selectList(Wrappers.lambdaQuery(MovieScreeningSeat.class)
                        .eq(MovieScreeningSeat::getScreeningId, movieScreeningSeatGenerateDto.getScreeningId())
                        .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode()))
                .stream()
                .map(seat -> seatPositionKey(seat.getRowCode(), seat.getColCode()))
                .collect(Collectors.toSet());
        int insertCount = 0;
        for (HallSeat hallSeat : hallSeatList) {
            if (existsSeatPositionSet.contains(seatPositionKey(hallSeat.getRowCode(), hallSeat.getColCode()))) {
                continue;
            }
            MovieScreeningSeat movieScreeningSeat = new MovieScreeningSeat();
            movieScreeningSeat.setId(uidGenerator.getUid());
            movieScreeningSeat.setScreeningId(movieScreeningSeatGenerateDto.getScreeningId());
            movieScreeningSeat.setProgramId(movieScreening.getProgramId());
            movieScreeningSeat.setHallSeatId(hallSeat.getId());
            movieScreeningSeat.setTicketCategoryId(movieScreeningSeatGenerateDto.getTicketCategoryId());
            movieScreeningSeat.setRowCode(hallSeat.getRowCode());
            movieScreeningSeat.setColCode(hallSeat.getColCode());
            movieScreeningSeat.setSeatNo(hallSeat.getSeatNo());
            movieScreeningSeat.setZoneName(hallSeat.getZoneName());
            movieScreeningSeat.setPriceLevel(hallSeat.getPriceLevel());
            movieScreeningSeat.setSeatType(hallSeat.getSeatType());
            movieScreeningSeat.setPrice(movieScreeningSeatGenerateDto.getPrice());
            movieScreeningSeat.setSellStatus(isHallSeatSellable(hallSeat) ?
                    SellStatus.NO_SOLD.getCode() : SellStatus.SOLD.getCode());
            movieScreeningSeat.setAisleFlag(hallSeat.getAisleFlag());
            movieScreeningSeat.setCoupleFlag(hallSeat.getCoupleFlag());
            movieScreeningSeat.setAccessibleFlag(hallSeat.getAccessibleFlag());
            movieScreeningSeat.setVipFlag(hallSeat.getVipFlag());
            movieScreeningSeat.setRepairFlag(hallSeat.getRepairFlag());
            movieScreeningSeat.setSellableFlag(hallSeat.getSellableFlag());
            movieScreeningSeat.setStatus(BusinessStatus.YES.getCode());
            movieScreeningSeatMapper.insert(movieScreeningSeat);
            insertCount++;
        }
        upsertGeneratedSeatPrice(movieScreeningSeatGenerateDto);
        clearScreeningRemainCache(movieScreeningSeatGenerateDto.getScreeningId(),
                List.of(movieScreeningSeatGenerateDto.getTicketCategoryId()));
        clearScreeningSeatCache(movieScreeningSeatGenerateDto.getScreeningId(),
                List.of(movieScreeningSeatGenerateDto.getTicketCategoryId()));
        return insertCount;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean movieScreeningSeatStatusUpdate(MovieScreeningSeatStatusUpdateDto movieScreeningSeatStatusUpdateDto) {
        if (Objects.isNull(SellStatus.getRc(movieScreeningSeatStatusUpdateDto.getSellStatus()))) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        List<MovieScreeningSeat> movieScreeningSeatList = movieScreeningSeatMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningSeat.class)
                        .eq(MovieScreeningSeat::getScreeningId, movieScreeningSeatStatusUpdateDto.getScreeningId())
                        .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode())
                        .in(MovieScreeningSeat::getId, movieScreeningSeatStatusUpdateDto.getSeatIdList()));
        if (CollectionUtil.isEmpty(movieScreeningSeatList)) {
            throw new TidesFrameException(BaseCode.SEAT_NOT_EXIST);
        }
        if (movieScreeningSeatList.size() != movieScreeningSeatStatusUpdateDto.getSeatIdList().size()) {
            throw new TidesFrameException(BaseCode.SEAT_UPDATE_REL_COUNT_NOT_EQUAL_PRESET_COUNT);
        }
        Map<Long, Long> remainChangeMap = new HashMap<>();
        for (MovieScreeningSeat movieScreeningSeat : movieScreeningSeatList) {
            long delta = getRemainDelta(movieScreeningSeat.getSellStatus(), movieScreeningSeatStatusUpdateDto.getSellStatus());
            if (delta != 0L) {
                remainChangeMap.merge(movieScreeningSeat.getTicketCategoryId(), delta, Long::sum);
            }
        }
        MovieScreeningSeat movieScreeningSeat = new MovieScreeningSeat();
        movieScreeningSeat.setSellStatus(movieScreeningSeatStatusUpdateDto.getSellStatus());
        movieScreeningSeat.setEditTime(DateUtils.now());
        movieScreeningSeatMapper.update(movieScreeningSeat, Wrappers.lambdaUpdate(MovieScreeningSeat.class)
                .eq(MovieScreeningSeat::getScreeningId, movieScreeningSeatStatusUpdateDto.getScreeningId())
                .in(MovieScreeningSeat::getId, movieScreeningSeatStatusUpdateDto.getSeatIdList()));
        Map<Long, RemainChangeSnapshot> remainSnapshotMap = new HashMap<>(remainChangeMap.size());
        for (Entry<Long, Long> entry : remainChangeMap.entrySet()) {
            remainSnapshotMap.put(entry.getKey(), adjustScreeningRemainNumber(
                    movieScreeningSeatStatusUpdateDto.getScreeningId(), entry.getKey(), entry.getValue()));
        }
        recordAdminInventoryEvents(movieScreeningSeatStatusUpdateDto, movieScreeningSeatList, remainSnapshotMap);
        clearScreeningRemainCache(movieScreeningSeatStatusUpdateDto.getScreeningId(), remainChangeMap.keySet());
        clearScreeningSeatCache(movieScreeningSeatStatusUpdateDto.getScreeningId(),
                movieScreeningSeatList.stream().map(MovieScreeningSeat::getTicketCategoryId).collect(Collectors.toSet()));
        return true;
    }

    private void putMovieRedisSeat(Map<Long, MovieScreeningSeat> redisSeatMap, RedisKeyManage redisKeyManage,
                                   Long screeningId, Long ticketCategoryId, List<String> seatIdList) {
        List<MovieScreeningSeat> movieScreeningSeatList = redisCache.multiGetForHash(RedisKeyBuild.createRedisKey(
                redisKeyManage, screeningId, ticketCategoryId), seatIdList, MovieScreeningSeat.class);
        if (CollectionUtil.isEmpty(movieScreeningSeatList)) {
            return;
        }
        for (MovieScreeningSeat movieScreeningSeat : movieScreeningSeatList) {
            if (Objects.nonNull(movieScreeningSeat)) {
                redisSeatMap.put(movieScreeningSeat.getId(), movieScreeningSeat);
            }
        }
    }

    private Movie validateScreeningMasterData(MovieScreeningSaveDto movieScreeningSaveDto) {
        Movie movie = movieMapper.selectOne(Wrappers.lambdaQuery(Movie.class)
                .eq(Movie::getId, movieScreeningSaveDto.getMovieId())
                .eq(Movie::getProgramId, movieScreeningSaveDto.getProgramId())
                .eq(Movie::getStatus, BusinessStatus.YES.getCode()));
        Cinema cinema = cinemaMapper.selectOne(Wrappers.lambdaQuery(Cinema.class)
                .eq(Cinema::getId, movieScreeningSaveDto.getCinemaId())
                .eq(Cinema::getStatus, BusinessStatus.YES.getCode()));
        Hall hall = hallMapper.selectOne(Wrappers.lambdaQuery(Hall.class)
                .eq(Hall::getId, movieScreeningSaveDto.getHallId())
                .eq(Hall::getCinemaId, movieScreeningSaveDto.getCinemaId())
                .eq(Hall::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(movie) || Objects.isNull(cinema) || Objects.isNull(hall)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return movie;
    }

    private MovieScreening validateScreeningExists(Long screeningId) {
        MovieScreening movieScreening = movieScreeningMapper.selectOne(Wrappers.lambdaQuery(MovieScreening.class)
                .eq(MovieScreening::getId, screeningId)
                .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(movieScreening)) {
            throw new TidesFrameException(BaseCode.MOVIE_SCREENING_NOT_EXIST);
        }
        return movieScreening;
    }

    private void fillScreeningTime(MovieScreening movieScreening, Movie movie) {
        if (Objects.isNull(movieScreening.getShowDayTime())) {
            movieScreening.setShowDayTime(DateUtils.getDateStart(movieScreening.getShowTime()));
        }
        if (!StringUtil.isNotEmpty(movieScreening.getShowWeekTime())) {
            movieScreening.setShowWeekTime(DateUtils.getWeekStr(movieScreening.getShowTime()));
        }
        if (Objects.isNull(movieScreening.getEndTime()) && Objects.nonNull(movie.getDurationMinutes())) {
            movieScreening.setEndTime(DateUtils.addMinute(movieScreening.getShowTime(), movie.getDurationMinutes()));
        }
        if (Objects.isNull(movieScreening.getStopSellTime())) {
            movieScreening.setStopSellTime(movieScreening.getShowTime());
        }
    }

    private void validatePriceNumber(Long totalNumber, Long remainNumber) {
        if (totalNumber < 0 || remainNumber < 0 || remainNumber > totalNumber) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private MovieScreeningPrice selectMovieScreeningPrice(Long screeningId, Long priceId, Long ticketCategoryId) {
        if (Objects.nonNull(priceId)) {
            return movieScreeningPriceMapper.selectOne(Wrappers.lambdaQuery(MovieScreeningPrice.class)
                    .eq(MovieScreeningPrice::getScreeningId, screeningId)
                    .eq(MovieScreeningPrice::getId, priceId));
        }
        return movieScreeningPriceMapper.selectOne(Wrappers.lambdaQuery(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, screeningId)
                .eq(MovieScreeningPrice::getTicketCategoryId, ticketCategoryId)
                .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
    }

    private void updateScreeningSeatPrice(Long screeningId, Long ticketCategoryId, BigDecimal price) {
        MovieScreeningSeat movieScreeningSeat = new MovieScreeningSeat();
        movieScreeningSeat.setPrice(price);
        movieScreeningSeat.setEditTime(DateUtils.now());
        movieScreeningSeatMapper.update(movieScreeningSeat, Wrappers.lambdaUpdate(MovieScreeningSeat.class)
                .eq(MovieScreeningSeat::getScreeningId, screeningId)
                .eq(MovieScreeningSeat::getTicketCategoryId, ticketCategoryId)
                .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode())
                .eq(MovieScreeningSeat::getSellStatus, SellStatus.NO_SOLD.getCode()));
    }

    private void upsertGeneratedSeatPrice(MovieScreeningSeatGenerateDto movieScreeningSeatGenerateDto) {
        Long activeSeatCount = movieScreeningSeatMapper.selectCount(Wrappers.lambdaQuery(MovieScreeningSeat.class)
                .eq(MovieScreeningSeat::getScreeningId, movieScreeningSeatGenerateDto.getScreeningId())
                .eq(MovieScreeningSeat::getTicketCategoryId, movieScreeningSeatGenerateDto.getTicketCategoryId())
                .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode()));
        Long noSoldSeatCount = movieScreeningSeatMapper.selectCount(Wrappers.lambdaQuery(MovieScreeningSeat.class)
                .eq(MovieScreeningSeat::getScreeningId, movieScreeningSeatGenerateDto.getScreeningId())
                .eq(MovieScreeningSeat::getTicketCategoryId, movieScreeningSeatGenerateDto.getTicketCategoryId())
                .eq(MovieScreeningSeat::getSellStatus, SellStatus.NO_SOLD.getCode())
                .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode()));
        MovieScreeningPrice movieScreeningPrice = movieScreeningPriceMapper.selectOne(Wrappers.lambdaQuery(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, movieScreeningSeatGenerateDto.getScreeningId())
                .eq(MovieScreeningPrice::getTicketCategoryId, movieScreeningSeatGenerateDto.getTicketCategoryId())
                .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(movieScreeningPrice)) {
            movieScreeningPrice = new MovieScreeningPrice();
            movieScreeningPrice.setId(uidGenerator.getUid());
            movieScreeningPrice.setScreeningId(movieScreeningSeatGenerateDto.getScreeningId());
            movieScreeningPrice.setTicketCategoryId(movieScreeningSeatGenerateDto.getTicketCategoryId());
            movieScreeningPrice.setPriceName(movieScreeningSeatGenerateDto.getPriceName());
            movieScreeningPrice.setPrice(movieScreeningSeatGenerateDto.getPrice());
            movieScreeningPrice.setTotalNumber(activeSeatCount);
            movieScreeningPrice.setRemainNumber(noSoldSeatCount);
            movieScreeningPrice.setStatus(BusinessStatus.YES.getCode());
            movieScreeningPriceMapper.insert(movieScreeningPrice);
            return;
        }
        MovieScreeningPrice updateMovieScreeningPrice = new MovieScreeningPrice();
        updateMovieScreeningPrice.setPriceName(movieScreeningSeatGenerateDto.getPriceName());
        updateMovieScreeningPrice.setPrice(movieScreeningSeatGenerateDto.getPrice());
        updateMovieScreeningPrice.setTotalNumber(activeSeatCount);
        updateMovieScreeningPrice.setRemainNumber(noSoldSeatCount);
        updateMovieScreeningPrice.setEditTime(DateUtils.now());
        movieScreeningPriceMapper.update(updateMovieScreeningPrice, Wrappers.lambdaUpdate(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, movieScreeningSeatGenerateDto.getScreeningId())
                .eq(MovieScreeningPrice::getId, movieScreeningPrice.getId()));
    }

    private long getRemainDelta(Integer beforeSellStatus, Integer afterSellStatus) {
        boolean beforeNoSold = Objects.equals(beforeSellStatus, SellStatus.NO_SOLD.getCode());
        boolean afterNoSold = Objects.equals(afterSellStatus, SellStatus.NO_SOLD.getCode());
        if (beforeNoSold && !afterNoSold) {
            return -1L;
        }
        if (!beforeNoSold && afterNoSold) {
            return 1L;
        }
        return 0L;
    }

    private RemainChangeSnapshot adjustScreeningRemainNumber(Long screeningId, Long ticketCategoryId, Long delta) {
        if (delta == 0L) {
            return selectRemainSnapshot(screeningId, ticketCategoryId);
        }
        MovieScreeningPrice dbMovieScreeningPrice = movieScreeningPriceMapper.selectOne(Wrappers.lambdaQuery(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, screeningId)
                .eq(MovieScreeningPrice::getTicketCategoryId, ticketCategoryId)
                .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode())
                .last("for update"));
        if (Objects.isNull(dbMovieScreeningPrice)) {
            throw new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2);
        }
        Long beforeRemainNumber = Objects.isNull(dbMovieScreeningPrice.getRemainNumber()) ? 0L :
                dbMovieScreeningPrice.getRemainNumber();
        if (delta < 0 && beforeRemainNumber < Math.abs(delta)) {
            throw new TidesFrameException(BaseCode.UPDATE_TICKET_CATEGORY_COUNT_NOT_CORRECT);
        }
        Long afterRemainNumber = beforeRemainNumber + delta;
        MovieScreeningPrice updateMovieScreeningPrice = new MovieScreeningPrice();
        updateMovieScreeningPrice.setRemainNumber(afterRemainNumber);
        updateMovieScreeningPrice.setEditTime(DateUtils.now());
        int updateCount = movieScreeningPriceMapper.update(updateMovieScreeningPrice, Wrappers.lambdaUpdate(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, screeningId)
                .eq(MovieScreeningPrice::getId, dbMovieScreeningPrice.getId())
                .eq(MovieScreeningPrice::getTicketCategoryId, ticketCategoryId)
                .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.UPDATE_TICKET_CATEGORY_COUNT_NOT_CORRECT);
        }
        return new RemainChangeSnapshot(ticketCategoryId, beforeRemainNumber, afterRemainNumber, delta);
    }

    private RemainChangeSnapshot selectRemainSnapshot(Long screeningId, Long ticketCategoryId) {
        MovieScreeningPrice movieScreeningPrice = movieScreeningPriceMapper.selectOne(Wrappers.lambdaQuery(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, screeningId)
                .eq(MovieScreeningPrice::getTicketCategoryId, ticketCategoryId)
                .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(movieScreeningPrice)) {
            return null;
        }
        Long remainNumber = Objects.isNull(movieScreeningPrice.getRemainNumber()) ? 0L : movieScreeningPrice.getRemainNumber();
        return new RemainChangeSnapshot(ticketCategoryId, remainNumber, remainNumber, 0L);
    }

    private void recordAdminInventoryEvents(MovieScreeningSeatStatusUpdateDto movieScreeningSeatStatusUpdateDto,
                                            List<MovieScreeningSeat> beforeSeatList,
                                            Map<Long, RemainChangeSnapshot> remainSnapshotMap) {
        if (CollectionUtil.isEmpty(beforeSeatList)) {
            return;
        }
        Map<Long, RemainChangeSnapshot> fullRemainSnapshotMap = fillRemainSnapshotMap(
                movieScreeningSeatStatusUpdateDto.getScreeningId(), beforeSeatList, remainSnapshotMap);
        Date now = DateUtils.now();
        for (MovieScreeningSeat beforeSeat : beforeSeatList) {
            MovieInventoryEvent movieInventoryEvent = new MovieInventoryEvent();
            movieInventoryEvent.setId(uidGenerator.getUid());
            movieInventoryEvent.setScreeningId(movieScreeningSeatStatusUpdateDto.getScreeningId());
            movieInventoryEvent.setProgramId(beforeSeat.getProgramId());
            movieInventoryEvent.setTicketCategoryId(beforeSeat.getTicketCategoryId());
            movieInventoryEvent.setSeatId(beforeSeat.getId());
            movieInventoryEvent.setEventType(INVENTORY_EVENT_ADMIN_CHANGE);
            movieInventoryEvent.setBeforeSellStatus(beforeSeat.getSellStatus());
            movieInventoryEvent.setAfterSellStatus(movieScreeningSeatStatusUpdateDto.getSellStatus());
            RemainChangeSnapshot remainChangeSnapshot = fullRemainSnapshotMap.get(beforeSeat.getTicketCategoryId());
            if (Objects.nonNull(remainChangeSnapshot)) {
                movieInventoryEvent.setBeforeRemainNumber(remainChangeSnapshot.beforeRemainNumber());
                movieInventoryEvent.setAfterRemainNumber(remainChangeSnapshot.afterRemainNumber());
            }
            movieInventoryEvent.setChangeCount(getRemainDelta(beforeSeat.getSellStatus(),
                    movieScreeningSeatStatusUpdateDto.getSellStatus()));
            movieInventoryEvent.setSourceType(INVENTORY_SOURCE_ADMIN_MANUAL);
            movieInventoryEvent.setBizNo(movieScreeningSeatStatusUpdateDto.getBizNo());
            movieInventoryEvent.setOperatorId(movieScreeningSeatStatusUpdateDto.getOperatorId());
            movieInventoryEvent.setEventTime(now);
            movieInventoryEvent.setRemark(StringUtil.isNotEmpty(movieScreeningSeatStatusUpdateDto.getRemark()) ?
                    movieScreeningSeatStatusUpdateDto.getRemark() : "后台手工调整座位状态");
            movieInventoryEvent.setStatus(BusinessStatus.YES.getCode());
            movieInventoryEventMapper.insert(movieInventoryEvent);
        }
    }

    private Map<Long, RemainChangeSnapshot> fillRemainSnapshotMap(Long screeningId, List<MovieScreeningSeat> beforeSeatList,
                                                                  Map<Long, RemainChangeSnapshot> remainSnapshotMap) {
        Map<Long, RemainChangeSnapshot> fullRemainSnapshotMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(remainSnapshotMap)) {
            fullRemainSnapshotMap.putAll(remainSnapshotMap);
        }
        Set<Long> missingTicketCategoryIdSet = new HashSet<>();
        for (MovieScreeningSeat beforeSeat : beforeSeatList) {
            Long ticketCategoryId = beforeSeat.getTicketCategoryId();
            if (Objects.nonNull(ticketCategoryId) && !fullRemainSnapshotMap.containsKey(ticketCategoryId)) {
                missingTicketCategoryIdSet.add(ticketCategoryId);
            }
        }
        if (CollectionUtil.isEmpty(missingTicketCategoryIdSet)) {
            return fullRemainSnapshotMap;
        }
        List<MovieScreeningPrice> movieScreeningPriceList = movieScreeningPriceMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningPrice.class)
                        .eq(MovieScreeningPrice::getScreeningId, screeningId)
                        .in(MovieScreeningPrice::getTicketCategoryId, missingTicketCategoryIdSet)
                        .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
        for (MovieScreeningPrice movieScreeningPrice : movieScreeningPriceList) {
            Long remainNumber = Objects.isNull(movieScreeningPrice.getRemainNumber()) ? 0L : movieScreeningPrice.getRemainNumber();
            fullRemainSnapshotMap.put(movieScreeningPrice.getTicketCategoryId(),
                    new RemainChangeSnapshot(movieScreeningPrice.getTicketCategoryId(), remainNumber, remainNumber, 0L));
        }
        return fullRemainSnapshotMap;
    }

    private MovieInventoryReconcileItemVo buildInventoryReconcileItem(Long screeningId,
                                                                      MovieScreeningPrice movieScreeningPrice,
                                                                      Map<Integer, Long> dbStatusCountMap) {
        MovieInventoryReconcileItemVo itemVo = new MovieInventoryReconcileItemVo();
        Long ticketCategoryId = movieScreeningPrice.getTicketCategoryId();
        itemVo.setTicketCategoryId(ticketCategoryId);
        itemVo.setPriceName(movieScreeningPrice.getPriceName());
        itemVo.setDbRemainNumber(Optional.ofNullable(movieScreeningPrice.getRemainNumber()).orElse(0L));
        itemVo.setDbNoSoldCount(getStatusCount(dbStatusCountMap, SellStatus.NO_SOLD.getCode()));
        itemVo.setDbLockCount(getStatusCount(dbStatusCountMap, SellStatus.LOCK.getCode()));
        itemVo.setDbSoldCount(getStatusCount(dbStatusCountMap, SellStatus.SOLD.getCode()));

        RedisCountSnapshot redisCountSnapshot = selectRedisSeatCountSnapshot(screeningId, ticketCategoryId);
        itemVo.setRedisCacheReady(redisCountSnapshot.cacheReady());
        itemVo.setRedisNoSoldCount(redisCountSnapshot.noSoldCount());
        itemVo.setRedisLockCount(redisCountSnapshot.lockCount());
        itemVo.setRedisSoldCount(redisCountSnapshot.soldCount());

        boolean remainConsistent = Objects.equals(itemVo.getDbRemainNumber(), itemVo.getDbNoSoldCount());
        boolean seatStatusConsistent = !redisCountSnapshot.cacheReady() ||
                (Objects.equals(itemVo.getDbNoSoldCount(), itemVo.getRedisNoSoldCount()) &&
                        Objects.equals(itemVo.getDbLockCount(), itemVo.getRedisLockCount()) &&
                        Objects.equals(itemVo.getDbSoldCount(), itemVo.getRedisSoldCount()));
        itemVo.setRemainConsistent(remainConsistent);
        itemVo.setSeatStatusConsistent(seatStatusConsistent);
        itemVo.setConsistent(remainConsistent && seatStatusConsistent);
        itemVo.setProblemDesc(buildReconcileProblemDesc(itemVo));
        return itemVo;
    }

    private void upsertInventoryReconcileIssues(MovieScreening movieScreening,
                                                List<MovieInventoryReconcileItemVo> itemList) {
        if (CollectionUtil.isEmpty(itemList)) {
            return;
        }
        Date now = DateUtils.now();
        for (MovieInventoryReconcileItemVo itemVo : itemList) {
            if (Boolean.TRUE.equals(itemVo.getConsistent())) {
                continue;
            }
            MovieInventoryReconcileIssue issue = movieInventoryReconcileIssueMapper.selectOne(
                    Wrappers.lambdaQuery(MovieInventoryReconcileIssue.class)
                            .eq(MovieInventoryReconcileIssue::getScreeningId, movieScreening.getId())
                            .eq(MovieInventoryReconcileIssue::getTicketCategoryId, itemVo.getTicketCategoryId())
                            .eq(MovieInventoryReconcileIssue::getIssueStatus, RECONCILE_ISSUE_OPEN)
                            .eq(MovieInventoryReconcileIssue::getStatus, BusinessStatus.YES.getCode())
                            .last("limit 1"));
            boolean needInsert = Objects.isNull(issue);
            if (needInsert) {
                issue = new MovieInventoryReconcileIssue();
                issue.setId(uidGenerator.getUid());
                issue.setScreeningId(movieScreening.getId());
                issue.setProgramId(movieScreening.getProgramId());
                issue.setMovieId(movieScreening.getMovieId());
                issue.setCinemaId(movieScreening.getCinemaId());
                issue.setHallId(movieScreening.getHallId());
                issue.setTicketCategoryId(itemVo.getTicketCategoryId());
                issue.setIssueStatus(RECONCILE_ISSUE_OPEN);
                issue.setStatus(BusinessStatus.YES.getCode());
                issue.setCreateTime(now);
            }
            fillReconcileIssueSnapshot(issue, itemVo, now);
            if (needInsert) {
                movieInventoryReconcileIssueMapper.insert(issue);
            } else {
                issue.setEditTime(now);
                movieInventoryReconcileIssueMapper.update(issue,
                        Wrappers.lambdaUpdate(MovieInventoryReconcileIssue.class)
                                .eq(MovieInventoryReconcileIssue::getId, issue.getId())
                                .eq(MovieInventoryReconcileIssue::getScreeningId, issue.getScreeningId())
                                .eq(MovieInventoryReconcileIssue::getStatus, BusinessStatus.YES.getCode()));
            }
        }
    }

    private void fillReconcileIssueSnapshot(MovieInventoryReconcileIssue issue,
                                            MovieInventoryReconcileItemVo itemVo,
                                            Date now) {
        issue.setPriceName(itemVo.getPriceName());
        issue.setIssueType(reconcileIssueType(itemVo));
        issue.setDbRemainNumber(itemVo.getDbRemainNumber());
        issue.setDbNoSoldCount(itemVo.getDbNoSoldCount());
        issue.setDbLockCount(itemVo.getDbLockCount());
        issue.setDbSoldCount(itemVo.getDbSoldCount());
        issue.setRedisNoSoldCount(itemVo.getRedisNoSoldCount());
        issue.setRedisLockCount(itemVo.getRedisLockCount());
        issue.setRedisSoldCount(itemVo.getRedisSoldCount());
        issue.setRedisCacheReady(booleanToStatus(itemVo.getRedisCacheReady()));
        issue.setRemainConsistent(booleanToStatus(itemVo.getRemainConsistent()));
        issue.setSeatStatusConsistent(booleanToStatus(itemVo.getSeatStatusConsistent()));
        issue.setProblemDesc(itemVo.getProblemDesc());
        issue.setLastReconcileTime(now);
        issue.setEditTime(now);
    }

    private String reconcileIssueType(MovieInventoryReconcileItemVo itemVo) {
        boolean remainIssue = !Boolean.TRUE.equals(itemVo.getRemainConsistent());
        boolean seatIssue = !Boolean.TRUE.equals(itemVo.getSeatStatusConsistent());
        if (remainIssue && seatIssue) {
            return RECONCILE_ISSUE_TYPE_MIXED;
        }
        if (remainIssue) {
            return RECONCILE_ISSUE_TYPE_REMAIN;
        }
        return RECONCILE_ISSUE_TYPE_REDIS_SEAT;
    }

    private Integer booleanToStatus(Boolean value) {
        return Boolean.TRUE.equals(value) ? BusinessStatus.YES.getCode() : BusinessStatus.NO.getCode();
    }

    private String reconcileIssueStatusName(Integer issueStatus) {
        if (Objects.equals(issueStatus, RECONCILE_ISSUE_OPEN)) {
            return "open";
        }
        if (Objects.equals(issueStatus, RECONCILE_ISSUE_PROCESSED)) {
            return "processed";
        }
        if (Objects.equals(issueStatus, RECONCILE_ISSUE_IGNORED)) {
            return "ignored";
        }
        return "unknown";
    }

    private RedisCountSnapshot selectRedisSeatCountSnapshot(Long screeningId, Long ticketCategoryId) {
        RedisKeyBuild noSoldKey = RedisKeyBuild.createRedisKey(
                RedisKeyManage.MOVIE_SCREENING_SEAT_NO_SOLD_RESOLUTION_HASH, screeningId, ticketCategoryId);
        RedisKeyBuild lockKey = RedisKeyBuild.createRedisKey(
                RedisKeyManage.MOVIE_SCREENING_SEAT_LOCK_RESOLUTION_HASH, screeningId, ticketCategoryId);
        RedisKeyBuild soldKey = RedisKeyBuild.createRedisKey(
                RedisKeyManage.MOVIE_SCREENING_SEAT_SOLD_RESOLUTION_HASH, screeningId, ticketCategoryId);
        boolean cacheReady = Boolean.TRUE.equals(redisCache.hasKey(noSoldKey)) ||
                Boolean.TRUE.equals(redisCache.hasKey(lockKey)) ||
                Boolean.TRUE.equals(redisCache.hasKey(soldKey));
        return new RedisCountSnapshot(cacheReady, hashSize(noSoldKey), hashSize(lockKey), hashSize(soldKey));
    }

    private Long hashSize(RedisKeyBuild redisKeyBuild) {
        return Optional.ofNullable(redisCache.sizeForHash(redisKeyBuild)).orElse(0L);
    }

    private String buildReconcileProblemDesc(MovieInventoryReconcileItemVo itemVo) {
        List<String> problemList = new ArrayList<>(3);
        if (!Boolean.TRUE.equals(itemVo.getRemainConsistent())) {
            problemList.add("DB票档余量与DB未售座位数不一致");
        }
        if (!Boolean.TRUE.equals(itemVo.getRedisCacheReady())) {
            problemList.add("Redis座位缓存未预热");
        } else if (!Boolean.TRUE.equals(itemVo.getSeatStatusConsistent())) {
            problemList.add("Redis座位状态数与DB座位状态数不一致");
        }
        if (CollectionUtil.isEmpty(problemList)) {
            return "一致";
        }
        return String.join("；", problemList);
    }

    private Long getStatusCount(Map<Integer, Long> dbStatusCountMap, Integer sellStatus) {
        if (CollectionUtil.isEmpty(dbStatusCountMap)) {
            return 0L;
        }
        return Optional.ofNullable(dbStatusCountMap.get(sellStatus)).orElse(0L);
    }

    private Long sumLong(List<Long> valueList) {
        if (CollectionUtil.isEmpty(valueList)) {
            return 0L;
        }
        return valueList.stream().filter(Objects::nonNull).mapToLong(Long::longValue).sum();
    }

    private void clearScreeningRemainCache(Long screeningId, Collection<Long> ticketCategoryIdList) {
        if (CollectionUtil.isEmpty(ticketCategoryIdList)) {
            return;
        }
        List<RedisKeyBuild> redisKeyBuildList = ticketCategoryIdList.stream()
                .filter(Objects::nonNull)
                .map(ticketCategoryId -> RedisKeyBuild.createRedisKey(
                        RedisKeyManage.MOVIE_SCREENING_TICKET_REMAIN_NUMBER_HASH_RESOLUTION, screeningId, ticketCategoryId))
                .toList();
        if (CollectionUtil.isNotEmpty(redisKeyBuildList)) {
            redisCache.del(redisKeyBuildList);
        }
    }

    private void clearScreeningSeatCache(Long screeningId, Collection<Long> ticketCategoryIdList) {
        if (CollectionUtil.isEmpty(ticketCategoryIdList)) {
            return;
        }
        List<RedisKeyBuild> redisKeyBuildList = new ArrayList<>();
        for (Long ticketCategoryId : ticketCategoryIdList) {
            if (Objects.isNull(ticketCategoryId)) {
                continue;
            }
            redisKeyBuildList.add(RedisKeyBuild.createRedisKey(
                    RedisKeyManage.MOVIE_SCREENING_SEAT_NO_SOLD_RESOLUTION_HASH, screeningId, ticketCategoryId));
            redisKeyBuildList.add(RedisKeyBuild.createRedisKey(
                    RedisKeyManage.MOVIE_SCREENING_SEAT_LOCK_RESOLUTION_HASH, screeningId, ticketCategoryId));
            redisKeyBuildList.add(RedisKeyBuild.createRedisKey(
                    RedisKeyManage.MOVIE_SCREENING_SEAT_SOLD_RESOLUTION_HASH, screeningId, ticketCategoryId));
        }
        if (CollectionUtil.isNotEmpty(redisKeyBuildList)) {
            redisCache.del(redisKeyBuildList);
        }
    }

    private boolean isHallSeatSellable(HallSeat hallSeat) {
        return Objects.equals(hallSeat.getSeatStatus(), BusinessStatus.YES.getCode()) &&
                Objects.equals(hallSeat.getSellableFlag(), BusinessStatus.YES.getCode()) &&
                !Objects.equals(hallSeat.getAisleFlag(), BusinessStatus.YES.getCode()) &&
                !Objects.equals(hallSeat.getRepairFlag(), BusinessStatus.YES.getCode());
    }

    private String seatPositionKey(Integer rowCode, Integer colCode) {
        return rowCode + "_" + colCode;
    }

    private Map<Long, Movie> selectMovieMap(List<MovieScreening> movieScreeningList) {
        List<Long> movieIdList = movieScreeningList.stream()
                .map(MovieScreening::getMovieId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtil.isEmpty(movieIdList)) {
            return new HashMap<>();
        }
        return movieMapper.selectList(Wrappers.lambdaQuery(Movie.class)
                        .in(Movie::getId, movieIdList)
                        .eq(Movie::getStatus, BusinessStatus.YES.getCode()))
                .stream()
                .collect(Collectors.toMap(Movie::getId, movie -> movie, (v1, v2) -> v2));
    }

    private Map<Long, Cinema> selectCinemaMap(List<MovieScreening> movieScreeningList) {
        List<Long> cinemaIdList = movieScreeningList.stream().map(MovieScreening::getCinemaId).filter(Objects::nonNull).distinct().toList();
        return selectCinemaMapByIds(cinemaIdList);
    }

    private Map<Long, Cinema> selectCinemaMapByIds(Collection<Long> cinemaIdList) {
        if (CollectionUtil.isEmpty(cinemaIdList)) {
            return new HashMap<>();
        }
        return cinemaMapper.selectBatchIds(cinemaIdList).stream()
                .collect(Collectors.toMap(Cinema::getId, cinema -> cinema, (v1, v2) -> v2));
    }

    private Map<Long, Hall> selectHallMap(List<MovieScreening> movieScreeningList) {
        List<Long> hallIdList = movieScreeningList.stream().map(MovieScreening::getHallId).filter(Objects::nonNull).distinct().toList();
        return selectHallMapByIds(hallIdList);
    }

    private Map<Long, Hall> selectHallMapByIds(Collection<Long> hallIdList) {
        if (CollectionUtil.isEmpty(hallIdList)) {
            return new HashMap<>();
        }
        return hallMapper.selectBatchIds(hallIdList).stream()
                .collect(Collectors.toMap(Hall::getId, hall -> hall, (v1, v2) -> v2));
    }

    private Set<Long> selectCinemaIdSetByArea(Long areaId, List<Long> areaIds) {
        if (!hasAreaFilter(areaId, areaIds)) {
            return new HashSet<>();
        }
        Set<Long> queryAreaIdSet = new HashSet<>();
        if (CollectionUtil.isNotEmpty(areaIds)) {
            areaIds.stream().filter(Objects::nonNull).forEach(queryAreaIdSet::add);
        }
        if (Objects.nonNull(areaId)) {
            queryAreaIdSet.add(areaId);
        }
        if (CollectionUtil.isEmpty(queryAreaIdSet)) {
            return new HashSet<>();
        }
        return cinemaMapper.selectList(Wrappers.lambdaQuery(Cinema.class)
                        .in(Cinema::getAreaId, queryAreaIdSet)
                        .eq(Cinema::getStatus, BusinessStatus.YES.getCode()))
                .stream()
                .map(Cinema::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean hasAreaFilter(Long areaId, List<Long> areaIds) {
        return Objects.nonNull(areaId) || CollectionUtil.isNotEmpty(areaIds);
    }

    private Map<Long, MovieOrderSalesCountVo> selectScreeningSalesMap(List<MovieScreening> movieScreeningList) {
        if (CollectionUtil.isEmpty(movieScreeningList)) {
            return new HashMap<>();
        }
        List<Long> screeningIdList = movieScreeningList.stream()
                .map(MovieScreening::getId).filter(Objects::nonNull).toList();
        List<Long> programIdList = movieScreeningList.stream()
                .map(MovieScreening::getProgramId).filter(Objects::nonNull).distinct().toList();
        MovieOrderSalesCountDto movieOrderSalesCountDto = new MovieOrderSalesCountDto();
        movieOrderSalesCountDto.setScreeningIdList(screeningIdList);
        movieOrderSalesCountDto.setProgramIdList(programIdList);
        try {
            ApiResponse<List<MovieOrderSalesCountVo>> response = orderClient.movieSalesCount(movieOrderSalesCountDto);
            if (!Objects.equals(response.getCode(), BaseCode.SUCCESS.getCode()) ||
                    CollectionUtil.isEmpty(response.getData())) {
                return new HashMap<>();
            }
            return response.getData().stream()
                    .filter(item -> Objects.nonNull(item.getScreeningId()))
                    .collect(Collectors.toMap(MovieOrderSalesCountVo::getScreeningId, item -> item, (v1, v2) -> {
                        MovieOrderSalesCountVo merged = new MovieOrderSalesCountVo();
                        merged.setProgramId(Optional.ofNullable(v1.getProgramId()).orElse(v2.getProgramId()));
                        merged.setScreeningId(v1.getScreeningId());
                        merged.setPaidOrderCount(Optional.ofNullable(v1.getPaidOrderCount()).orElse(0L) +
                                Optional.ofNullable(v2.getPaidOrderCount()).orElse(0L));
                        merged.setPaidTicketCount(Optional.ofNullable(v1.getPaidTicketCount()).orElse(0L) +
                                Optional.ofNullable(v2.getPaidTicketCount()).orElse(0L));
                        return merged;
                    }));
        } catch (Exception exception) {
            log.warn("order-service movie screening sales count rpc error screeningIdList:{}", screeningIdList, exception);
            return new HashMap<>();
        }
    }

    private Map<Long, ScreeningInventoryAggregate> selectScreeningInventoryAggregateMap(List<Long> screeningIdList) {
        if (CollectionUtil.isEmpty(screeningIdList)) {
            return new HashMap<>();
        }
        List<MovieScreeningPrice> movieScreeningPriceList = movieScreeningPriceMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningPrice.class)
                        .in(MovieScreeningPrice::getScreeningId, screeningIdList)
                        .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
        if (CollectionUtil.isEmpty(movieScreeningPriceList)) {
            return new HashMap<>();
        }
        Map<Long, Map<Integer, Long>> screeningSeatStatusCountMap =
                selectScreeningSeatStatusCountMap(screeningIdList);
        return movieScreeningPriceList.stream()
                .collect(Collectors.groupingBy(MovieScreeningPrice::getScreeningId,
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            Long screeningId = list.get(0).getScreeningId();
                            Map<Integer, Long> statusCountMap =
                                    Optional.ofNullable(screeningSeatStatusCountMap.get(screeningId))
                                            .orElse(new HashMap<>());
                            return new ScreeningInventoryAggregate(
                                    list.stream().map(MovieScreeningPrice::getTotalNumber)
                                            .filter(Objects::nonNull).mapToLong(Long::longValue).sum(),
                                    list.stream().map(MovieScreeningPrice::getRemainNumber)
                                            .filter(Objects::nonNull).mapToLong(Long::longValue).sum(),
                                    Optional.ofNullable(statusCountMap.get(SellStatus.LOCK.getCode())).orElse(0L),
                                    Optional.ofNullable(statusCountMap.get(SellStatus.SOLD.getCode())).orElse(0L)
                            );
                        })));
    }

    private Map<Long, Map<Integer, Long>> selectScreeningSeatStatusCountMap(List<Long> screeningIdList) {
        if (CollectionUtil.isEmpty(screeningIdList)) {
            return new HashMap<>();
        }
        List<MovieScreeningSeat> movieScreeningSeatList = movieScreeningSeatMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningSeat.class)
                        .in(MovieScreeningSeat::getScreeningId, screeningIdList)
                        .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode()));
        if (CollectionUtil.isEmpty(movieScreeningSeatList)) {
            return new HashMap<>();
        }
        return movieScreeningSeatList.stream()
                .collect(Collectors.groupingBy(MovieScreeningSeat::getScreeningId,
                        Collectors.groupingBy(MovieScreeningSeat::getSellStatus, Collectors.counting())));
    }

    private Map<Long, Long> selectScreeningRemainNumberMap(List<Long> screeningIdList) {
        if (CollectionUtil.isEmpty(screeningIdList)) {
            return new HashMap<>();
        }
        List<MovieScreeningPrice> movieScreeningPriceList = movieScreeningPriceMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningPrice.class)
                        .in(MovieScreeningPrice::getScreeningId, screeningIdList)
                        .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
        if (CollectionUtil.isEmpty(movieScreeningPriceList)) {
            return new HashMap<>();
        }
        return movieScreeningPriceList.stream()
                .collect(Collectors.groupingBy(MovieScreeningPrice::getScreeningId,
                        Collectors.summingLong(movieScreeningPrice ->
                                Optional.ofNullable(movieScreeningPrice.getRemainNumber()).orElse(0L))));
    }

    private record RemainChangeSnapshot(Long ticketCategoryId, Long beforeRemainNumber, Long afterRemainNumber,
                                        Long changeCount) {
    }

    private record RedisCountSnapshot(Boolean cacheReady, Long noSoldCount, Long lockCount, Long soldCount) {
    }

    private record ScreeningInventoryAggregate(Long totalNumber, Long remainNumber, Long lockedNumber,
                                               Long soldSeatNumber) {
    }

}
