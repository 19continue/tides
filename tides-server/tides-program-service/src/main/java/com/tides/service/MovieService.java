package com.tides.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tides.client.BaseDataClient;
import com.tides.common.ApiResponse;
import com.tides.core.RedisKeyManage;
import com.tides.dto.AreaGetDto;
import com.tides.dto.AreaSelectDto;
import com.tides.dto.MovieCinemaDetailDto;
import com.tides.dto.MovieCinemaMovieListDto;
import com.tides.dto.MovieCinemaPageDto;
import com.tides.dto.MovieCinemaScreeningListDto;
import com.tides.dto.MovieDetailDto;
import com.tides.dto.MoviePageDto;
import com.tides.dto.ProgramOperateDataDto;
import com.tides.dto.ReduceRemainNumberDto;
import com.tides.dto.TicketCategoryCountDto;
import com.tides.dto.MovieScreeningListDto;
import com.tides.dto.MovieScreeningSeatDto;
import com.tides.entity.Artist;
import com.tides.entity.Cinema;
import com.tides.entity.Hall;
import com.tides.entity.Movie;
import com.tides.entity.MovieInventoryEvent;
import com.tides.entity.MovieMedia;
import com.tides.entity.MovieProfile;
import com.tides.entity.MovieScreening;
import com.tides.entity.MovieScreeningPrice;
import com.tides.entity.MovieScreeningSeat;
import com.tides.entity.ProgramArtist;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.enums.ProgramOrderVersion;
import com.tides.enums.SeatType;
import com.tides.enums.SellStatus;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.ArtistMapper;
import com.tides.mapper.CinemaMapper;
import com.tides.mapper.HallMapper;
import com.tides.mapper.MovieInventoryEventMapper;
import com.tides.mapper.MovieMapper;
import com.tides.mapper.MovieMediaMapper;
import com.tides.mapper.MovieProfileMapper;
import com.tides.mapper.MovieScreeningMapper;
import com.tides.mapper.MovieScreeningPriceMapper;
import com.tides.mapper.MovieScreeningSeatMapper;
import com.tides.mapper.ProgramArtistMapper;
import com.tides.page.PageUtil;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.service.lua.ProgramSeatCacheData;
import com.tides.service.es.MovieEs;
import com.tides.service.tool.SearchKeywordUtil;
import com.tides.servicelock.LockType;
import com.tides.util.DateUtils;
import com.tides.util.ServiceLockTool;
import com.tides.util.StringUtil;
import com.tides.vo.AreaVo;
import com.tides.vo.MovieArtistVo;
import com.tides.vo.MovieCinemaDetailVo;
import com.tides.vo.MovieCinemaMovieVo;
import com.tides.vo.MovieCinemaVo;
import com.tides.vo.MovieDetailVo;
import com.tides.vo.MovieHallVo;
import com.tides.vo.MovieListVo;
import com.tides.vo.MovieMediaVo;
import com.tides.vo.MovieScreeningVo;
import com.tides.vo.MovieSeatRelateInfoVo;
import com.tides.vo.SeatVo;
import com.tides.vo.TicketCategoryVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tides.core.DistributedLockConstants.GET_REMAIN_NUMBER_LOCK;
import static com.tides.core.DistributedLockConstants.GET_SEAT_LOCK;

/**
 * @description: 电影业务 service
 */
@Slf4j
@Service
public class MovieService {

    private static final Long NATIONAL_AREA_ID = 1L;

    private static final String INVENTORY_EVENT_LOCK = "LOCK";

    private static final int MIN_SEARCH_CONTENT_LENGTH = 2;

    private static final String INVENTORY_EVENT_SOLD = "SOLD";

    private static final String INVENTORY_EVENT_RELEASE = "RELEASE";

    private static final String INVENTORY_SOURCE_ORDER_CREATE_LOCK = "ORDER_CREATE_LOCK";

    private static final String INVENTORY_SOURCE_ORDER_PAY = "ORDER_PAY";

    private static final String INVENTORY_SOURCE_ORDER_RELEASE = "ORDER_RELEASE";

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private MovieProfileMapper movieProfileMapper;

    @Autowired
    private MovieMediaMapper movieMediaMapper;

    @Autowired
    private ProgramArtistMapper programArtistMapper;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private MovieScreeningMapper movieScreeningMapper;

    @Autowired
    private MovieScreeningSeatMapper movieScreeningSeatMapper;

    @Autowired
    private MovieScreeningPriceMapper movieScreeningPriceMapper;

    @Autowired
    private MovieInventoryEventMapper movieInventoryEventMapper;

    @Autowired
    private CinemaMapper cinemaMapper;

    @Autowired
    private HallMapper hallMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private BaseDataClient baseDataClient;

    @Autowired
    private ServiceLockTool serviceLockTool;

    @Autowired
    private ProgramSeatCacheData programSeatCacheData;

    @Autowired
    private MovieEs movieEs;

    @Autowired
    private UidGenerator uidGenerator;

    public IPage<MovieListVo> page(MoviePageDto moviePageDto) {
        Page<MovieListVo> movieListVoPage = new Page<>(moviePageDto.getPageNumber(), moviePageDto.getPageSize());
        List<MovieListVo> movieListVoList = selectAvailableMovieList(moviePageDto);
        if (StringUtil.isNotEmpty(moviePageDto.getContent())) {
            movieListVoList = applyMovieSearchResult(movieListVoList, moviePageDto.getContent());
        }
        movieListVoPage.setTotal(movieListVoList.size());
        int fromIndex = Math.min((moviePageDto.getPageNumber() - 1) * moviePageDto.getPageSize(),
                movieListVoList.size());
        int toIndex = Math.min(fromIndex + moviePageDto.getPageSize(), movieListVoList.size());
        movieListVoPage.setRecords(movieListVoList.subList(fromIndex, toIndex));
        return movieListVoPage;
    }

    public List<MovieListVo> listForSearchIndex() {
        MoviePageDto moviePageDto = new MoviePageDto();
        return selectAvailableMovieList(moviePageDto);
    }

    private List<MovieListVo> selectAvailableMovieList(MoviePageDto moviePageDto) {
        Set<Long> cinemaIdSet = selectCinemaIdSetByAreaId(moviePageDto.getAreaId());
        if (Objects.nonNull(cinemaIdSet) && CollectionUtil.isEmpty(cinemaIdSet)) {
            return new ArrayList<>();
        }

        List<MovieScreening> movieScreeningList = selectSellingScreeningList(null, null,
                null, cinemaIdSet, null, true);
        if (CollectionUtil.isEmpty(movieScreeningList)) {
            return new ArrayList<>();
        }

        Map<Long, List<MovieScreening>> screeningMap = movieScreeningList.stream()
                .filter(movieScreening -> Objects.nonNull(movieScreening.getProgramId()))
                .collect(Collectors.groupingBy(MovieScreening::getProgramId));
        Map<Long, Movie> movieMap = selectMovieMapByProgramIds(screeningMap.keySet());
        Map<Long, MovieProfile> movieProfileMap = selectMovieProfileMapByProgramIds(screeningMap.keySet());
        Integer releaseStatus = releaseStatusByCategory(moviePageDto.getProgramCategoryId(),
                moviePageDto.getReleaseStatus());

        List<MovieListVo> movieListVoList = screeningMap.entrySet().stream()
                .map(entry -> buildMovieListVo(movieMap.get(entry.getKey()), movieProfileMap.get(entry.getKey()),
                        entry.getValue()))
                .filter(Objects::nonNull)
                .filter(item -> Objects.isNull(releaseStatus) || Objects.equals(item.getReleaseStatus(), releaseStatus))
                .sorted(Comparator.comparing(MovieListVo::getReleaseStatus, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MovieListVo::getNearestShowTime, Comparator.nullsLast(Date::compareTo)))
                .toList();
        return movieListVoList;
    }

    private List<MovieListVo> applyMovieSearchResult(List<MovieListVo> movieListVoList, String content) {
        if (CollectionUtil.isEmpty(movieListVoList) || !validMovieSearchContent(content)) {
            return new ArrayList<>();
        }
        List<MovieListVo> esMovieList = movieEs.searchAll(content);
        Map<Long, Integer> programOrderMap = new HashMap<>(esMovieList.size());
        Map<Long, MovieListVo> highlightMovieMap = new HashMap<>(esMovieList.size());
        for (int index = 0; index < esMovieList.size(); index++) {
            MovieListVo movieListVo = esMovieList.get(index);
            if (Objects.isNull(movieListVo.getProgramId())) {
                continue;
            }
            programOrderMap.put(movieListVo.getProgramId(), index);
            highlightMovieMap.put(movieListVo.getProgramId(), movieListVo);
        }
        return movieListVoList.stream()
                .filter(item -> programOrderMap.containsKey(item.getProgramId()) || matchMovieContent(item, content))
                .map(item -> applyMovieHighlight(item, highlightMovieMap.get(item.getProgramId())))
                .sorted(Comparator.comparingInt((MovieListVo item) ->
                                programOrderMap.getOrDefault(item.getProgramId(), Integer.MAX_VALUE))
                        .thenComparing(Comparator.comparingInt((MovieListVo item) ->
                                movieSearchScore(item, content)).reversed())
                        .thenComparing(MovieListVo::getNearestShowTime, Comparator.nullsLast(Date::compareTo)))
                .toList();
    }

    private MovieListVo applyMovieHighlight(MovieListVo movieListVo, MovieListVo highlightMovie) {
        if (Objects.isNull(highlightMovie)) {
            return movieListVo;
        }
        if (containsHighlight(highlightMovie.getMovieName())) {
            movieListVo.setMovieName(highlightMovie.getMovieName());
        }
        if (containsHighlight(highlightMovie.getMovieAlias())) {
            movieListVo.setMovieAlias(highlightMovie.getMovieAlias());
        }
        if (containsHighlight(highlightMovie.getDirector())) {
            movieListVo.setDirector(highlightMovie.getDirector());
        }
        if (containsHighlight(highlightMovie.getActors())) {
            movieListVo.setActors(highlightMovie.getActors());
        }
        return movieListVo;
    }

    private boolean containsHighlight(String value) {
        return StringUtil.isNotEmpty(value) && value.contains("<em>");
    }

    private boolean matchMovieContent(MovieListVo movieListVo, String content) {
        return movieSearchScore(movieListVo, content) > 0;
    }

    private boolean validMovieSearchContent(String content) {
        return SearchKeywordUtil.validContent(content);
    }

    private int movieSearchScore(MovieListVo movieListVo, String content) {
        if (!validMovieSearchContent(content)) {
            return 0;
        }
        int score = 0;
        score += SearchKeywordUtil.fieldScore(movieListVo.getMovieName(), content, 80);
        score += SearchKeywordUtil.fieldScore(movieListVo.getActors(), content, 70);
        score += SearchKeywordUtil.fieldScore(movieListVo.getMovieAlias(), content, 50);
        score += SearchKeywordUtil.fieldScore(movieListVo.getDirector(), content, 40);
        return score;
    }

    public MovieDetailVo detail(MovieDetailDto movieDetailDto) {
        Movie movie = selectMovie(movieDetailDto.getProgramId(), movieDetailDto.getMovieId());
        MovieDetailVo movieDetailVo = new MovieDetailVo();
        BeanUtil.copyProperties(movie, movieDetailVo);
        movieDetailVo.setMovieId(movie.getId());

        MovieProfile movieProfile = movieProfileMapper.selectOne(Wrappers.lambdaQuery(MovieProfile.class)
                .eq(MovieProfile::getProgramId, movie.getProgramId())
                .eq(MovieProfile::getMovieId, movie.getId())
                .eq(MovieProfile::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.nonNull(movieProfile)) {
            BeanUtil.copyProperties(movieProfile, movieDetailVo);
            movieDetailVo.setMovieId(movie.getId());
            movieDetailVo.setProgramId(movie.getProgramId());
        }

        movieDetailVo.setArtistList(selectMovieArtistList(movie.getProgramId()));
        movieDetailVo.setMediaList(selectMovieMediaList(movie.getId()));

        Set<Long> cinemaIdSet = selectCinemaIdSetByAreaId(movieDetailDto.getAreaId());
        List<MovieScreening> movieScreeningList = Objects.nonNull(cinemaIdSet) && CollectionUtil.isEmpty(cinemaIdSet) ?
                new ArrayList<>() :
                selectSellingScreeningList(movie.getProgramId(), movie.getId(), null, cinemaIdSet, null, true);
        movieDetailVo.setScreeningList(buildMovieScreeningVoList(movieScreeningList));
        movieDetailVo.setCinemaCount((int) movieScreeningList.stream()
                .map(MovieScreening::getCinemaId)
                .filter(Objects::nonNull)
                .distinct()
                .count());
        movieDetailVo.setLowestPrice(minLowestPrice(movieScreeningList));
        movieDetailVo.setNearestShowTime(nearestShowTime(movieScreeningList));
        movieDetailVo.setShowDayList(movieScreeningList.stream()
                .map(MovieScreening::getShowDayTime)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList());
        return movieDetailVo;
    }

    public IPage<MovieCinemaVo> cinemaPage(MovieCinemaPageDto movieCinemaPageDto) {
        IPage<MovieCinemaVo> movieCinemaVoPage =
                new Page<>(movieCinemaPageDto.getPageNumber(), movieCinemaPageDto.getPageSize());
        Set<Long> cinemaIdFilterSet = selectCinemaIdFilterSet(movieCinemaPageDto);
        if (Objects.nonNull(cinemaIdFilterSet) && CollectionUtil.isEmpty(cinemaIdFilterSet)) {
            return movieCinemaVoPage;
        }

        IPage<Cinema> cinemaPage = cinemaMapper.selectPage(PageUtil.getPageParams(movieCinemaPageDto.getPageNumber(),
                movieCinemaPageDto.getPageSize()), Wrappers.lambdaQuery(Cinema.class)
                .eq(Objects.nonNull(movieCinemaPageDto.getAreaId()) && !isAllArea(movieCinemaPageDto.getAreaId()),
                        Cinema::getAreaId, movieCinemaPageDto.getAreaId())
                .like(StringUtil.isNotEmpty(movieCinemaPageDto.getCinemaName()), Cinema::getCinemaName,
                        movieCinemaPageDto.getCinemaName())
                .like(StringUtil.isNotEmpty(movieCinemaPageDto.getBrandName()), Cinema::getBrandName,
                        movieCinemaPageDto.getBrandName())
                .like(StringUtil.isNotEmpty(movieCinemaPageDto.getBusinessArea()), Cinema::getBusinessArea,
                        movieCinemaPageDto.getBusinessArea())
                .in(Objects.nonNull(cinemaIdFilterSet), Cinema::getId, cinemaIdFilterSet)
                .eq(Cinema::getStatus, BusinessStatus.YES.getCode())
                .orderByDesc(Cinema::getEditTime));
        if (CollectionUtil.isEmpty(cinemaPage.getRecords())) {
            return movieCinemaVoPage;
        }

        Map<Long, String> areaNameMap = selectAreaNameMap(cinemaPage.getRecords().stream()
                .map(Cinema::getAreaId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Map<Long, CinemaScreeningSummary> cinemaSummaryMap = selectCinemaScreeningSummaryMap(
                cinemaPage.getRecords().stream().map(Cinema::getId).toList(),
                movieCinemaPageDto.getProgramId(), movieCinemaPageDto.getMovieId(), movieCinemaPageDto.getShowDayTime());
        List<MovieCinemaVo> movieCinemaVoList = cinemaPage.getRecords().stream()
                .map(cinema -> buildMovieCinemaVo(cinema, areaNameMap, cinemaSummaryMap.get(cinema.getId())))
                .toList();
        BeanUtil.copyProperties(cinemaPage, movieCinemaVoPage);
        movieCinemaVoPage.setRecords(movieCinemaVoList);
        return movieCinemaVoPage;
    }

    public MovieCinemaDetailVo cinemaDetail(MovieCinemaDetailDto movieCinemaDetailDto) {
        Cinema cinema = selectCinema(movieCinemaDetailDto.getCinemaId());
        Map<Long, String> areaNameMap = selectAreaNameMap(Objects.isNull(cinema.getAreaId()) ?
                new ArrayList<>() : List.of(cinema.getAreaId()));
        Map<Long, CinemaScreeningSummary> cinemaSummaryMap = selectCinemaScreeningSummaryMap(
                List.of(cinema.getId()), null, null, null);

        MovieCinemaDetailVo movieCinemaDetailVo = new MovieCinemaDetailVo();
        BeanUtil.copyProperties(buildMovieCinemaVo(cinema, areaNameMap, cinemaSummaryMap.get(cinema.getId())),
                movieCinemaDetailVo);
        movieCinemaDetailVo.setHallList(selectHallList(cinema.getId()));

        MovieCinemaMovieListDto movieCinemaMovieListDto = new MovieCinemaMovieListDto();
        movieCinemaMovieListDto.setCinemaId(cinema.getId());
        movieCinemaDetailVo.setMovieList(cinemaMovieList(movieCinemaMovieListDto));
        return movieCinemaDetailVo;
    }

    public List<MovieCinemaMovieVo> cinemaMovieList(MovieCinemaMovieListDto movieCinemaMovieListDto) {
        List<MovieScreening> movieScreeningList = selectSellingScreeningList(null, null,
                movieCinemaMovieListDto.getCinemaId(), movieCinemaMovieListDto.getShowDayTime(), true);
        if (CollectionUtil.isEmpty(movieScreeningList)) {
            return new ArrayList<>();
        }
        Map<Long, List<MovieScreening>> screeningMap = movieScreeningList.stream()
                .collect(Collectors.groupingBy(MovieScreening::getProgramId));
        Map<Long, Movie> movieMap = selectMovieMapByProgramIds(screeningMap.keySet());
        return screeningMap.entrySet().stream()
                .map(entry -> buildMovieCinemaMovieVo(movieMap.get(entry.getKey()), entry.getValue()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MovieCinemaMovieVo::getNearestShowTime,
                        Comparator.nullsLast(Date::compareTo)))
                .toList();
    }

    public List<MovieScreeningVo> cinemaScreeningList(MovieCinemaScreeningListDto movieCinemaScreeningListDto) {
        Long programId = movieCinemaScreeningListDto.getProgramId();
        Long movieId = movieCinemaScreeningListDto.getMovieId();
        if (Objects.isNull(programId) && Objects.nonNull(movieId)) {
            Movie movie = selectMovie(null, movieId);
            programId = movie.getProgramId();
        }
        List<MovieScreening> movieScreeningList = selectSellingScreeningList(programId, movieId,
                movieCinemaScreeningListDto.getCinemaId(), movieCinemaScreeningListDto.getShowDayTime(), true);
        return buildMovieScreeningVoList(movieScreeningList);
    }

    public List<MovieScreeningVo> screeningList(MovieScreeningListDto movieScreeningListDto) {
        Set<Long> cinemaIdSet = selectCinemaIdSetByAreaId(movieScreeningListDto.getAreaId());
        if (Objects.nonNull(cinemaIdSet) && CollectionUtil.isEmpty(cinemaIdSet)) {
            return new ArrayList<>();
        }
        List<MovieScreening> movieScreeningList = selectSellingScreeningList(movieScreeningListDto.getProgramId(),
                null, movieScreeningListDto.getCinemaId(), cinemaIdSet, movieScreeningListDto.getShowDayTime(), true);
        if (CollectionUtil.isEmpty(movieScreeningList)) {
            return new ArrayList<>();
        }

        Map<Long, Cinema> cinemaMap = selectCinemaMap(movieScreeningList);
        Map<Long, Hall> hallMap = selectHallMap(movieScreeningList);
        return movieScreeningList.stream()
                .map(movieScreening -> buildMovieScreeningVo(movieScreening, cinemaMap, hallMap))
                .collect(Collectors.toList());
    }

    public MovieSeatRelateInfoVo screeningSeatInfo(MovieScreeningSeatDto movieScreeningSeatDto) {
        MovieScreening movieScreening = selectSellingScreening(movieScreeningSeatDto.getScreeningId());

        List<MovieScreeningPrice> movieScreeningPriceList = movieScreeningPriceMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningPrice.class)
                        .eq(MovieScreeningPrice::getScreeningId, movieScreening.getId())
                        .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode())
                        .orderByAsc(MovieScreeningPrice::getPrice));
        Long seatCacheExpireTime = DateUtils.countBetweenSecond(DateUtils.now(), movieScreening.getShowTime());
        List<SeatVo> seatVoList = new ArrayList<>();
        for (MovieScreeningPrice movieScreeningPrice : movieScreeningPriceList) {
            seatVoList.addAll(selectSeatResolution(movieScreening.getId(), movieScreeningPrice.getTicketCategoryId(),
                    seatCacheExpireTime, TimeUnit.SECONDS));
        }
        seatVoList = mergeSeatVoById(seatVoList);
        Map<String, List<SeatVo>> seatVoMap = seatVoList.stream()
                .collect(Collectors.groupingBy(seatVo -> priceKey(seatVo.getPrice())));

        Cinema cinema = cinemaMapper.selectById(movieScreening.getCinemaId());
        Hall hall = hallMapper.selectById(movieScreening.getHallId());

        MovieSeatRelateInfoVo movieSeatRelateInfoVo = new MovieSeatRelateInfoVo();
        movieSeatRelateInfoVo.setScreeningId(movieScreening.getId());
        movieSeatRelateInfoVo.setProgramId(movieScreening.getProgramId());
        movieSeatRelateInfoVo.setMovieId(movieScreening.getMovieId());
        movieSeatRelateInfoVo.setCinemaId(movieScreening.getCinemaId());
        movieSeatRelateInfoVo.setHallId(movieScreening.getHallId());
        movieSeatRelateInfoVo.setShowTime(movieScreening.getShowTime());
        movieSeatRelateInfoVo.setShowWeekTime(movieScreening.getShowWeekTime());
        if (Objects.nonNull(cinema)) {
            movieSeatRelateInfoVo.setCinemaName(cinema.getCinemaName());
        }
        if (Objects.nonNull(hall)) {
            movieSeatRelateInfoVo.setHallName(hall.getHallName());
            movieSeatRelateInfoVo.setHallType(hall.getHallType());
        }
        movieSeatRelateInfoVo.setPriceList(seatVoMap.keySet().stream()
                .sorted(Comparator.comparing(BigDecimal::new))
                .collect(Collectors.toList()));
        movieSeatRelateInfoVo.setSeatVoMap(seatVoMap);
        return movieSeatRelateInfoVo;
    }

    public MovieScreening selectSellingScreening(Long screeningId) {
        MovieScreening movieScreening = movieScreeningMapper.selectOne(
                Wrappers.lambdaQuery(MovieScreening.class)
                        .eq(MovieScreening::getId, screeningId)
                        .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(movieScreening)) {
            throw new TidesFrameException(BaseCode.MOVIE_SCREENING_NOT_EXIST);
        }
        if (!Objects.equals(movieScreening.getScreeningStatus(), BusinessStatus.YES.getCode())) {
            throw new TidesFrameException(BaseCode.MOVIE_SCREENING_NOT_SELLING);
        }
        Date now = DateUtils.now();
        if (Objects.isNull(movieScreening.getShowTime()) || !movieScreening.getShowTime().after(now)) {
            throw new TidesFrameException(BaseCode.MOVIE_SCREENING_NOT_SELLING);
        }
        if (Objects.nonNull(movieScreening.getStopSellTime()) && !movieScreening.getStopSellTime().after(now)) {
            throw new TidesFrameException(BaseCode.MOVIE_SCREENING_NOT_SELLING);
        }
        return movieScreening;
    }

    public MovieScreeningVo screeningInfo(Long screeningId) {
        MovieScreening movieScreening = selectSellingScreening(screeningId);
        Cinema cinema = cinemaMapper.selectById(movieScreening.getCinemaId());
        Hall hall = hallMapper.selectById(movieScreening.getHallId());
        Map<Long, Cinema> cinemaMap = Objects.isNull(cinema) ? new HashMap<>(0) : Map.of(cinema.getId(), cinema);
        Map<Long, Hall> hallMap = Objects.isNull(hall) ? new HashMap<>(0) : Map.of(hall.getId(), hall);
        return buildMovieScreeningVo(movieScreening, cinemaMap, hallMap);
    }

    public List<TicketCategoryVo> selectTicketCategoryListByScreeningId(Long screeningId) {
        selectSellingScreening(screeningId);
        List<MovieScreeningPrice> movieScreeningPriceList = movieScreeningPriceMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningPrice.class)
                        .eq(MovieScreeningPrice::getScreeningId, screeningId)
                        .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode())
                        .orderByAsc(MovieScreeningPrice::getPrice));
        if (CollectionUtil.isEmpty(movieScreeningPriceList)) {
            return new ArrayList<>();
        }
        return movieScreeningPriceList.stream()
                .map(movieScreeningPrice -> {
                    TicketCategoryVo ticketCategoryVo = new TicketCategoryVo();
                    ticketCategoryVo.setId(movieScreeningPrice.getTicketCategoryId());
                    ticketCategoryVo.setIntroduce(movieScreeningPrice.getPriceName());
                    ticketCategoryVo.setPrice(movieScreeningPrice.getPrice());
                    return ticketCategoryVo;
                })
                .collect(Collectors.toList());
    }

    public List<SeatVo> selectSeatResolution(Long screeningId, Long ticketCategoryId, Long expireTime, TimeUnit timeUnit) {
        List<SeatVo> seatVoList = getSeatVoListByCacheResolution(screeningId, ticketCategoryId);
        if (CollectionUtil.isNotEmpty(seatVoList)) {
            return seatVoList;
        }
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, GET_SEAT_LOCK,
                new String[]{"movie", String.valueOf(screeningId), String.valueOf(ticketCategoryId)});
        lock.lock();
        try {
            seatVoList = getSeatVoListByCacheResolution(screeningId, ticketCategoryId);
            if (CollectionUtil.isNotEmpty(seatVoList)) {
                return seatVoList;
            }
            List<MovieScreeningSeat> movieScreeningSeatList = movieScreeningSeatMapper.selectList(
                    Wrappers.lambdaQuery(MovieScreeningSeat.class)
                            .eq(MovieScreeningSeat::getScreeningId, screeningId)
                            .eq(MovieScreeningSeat::getTicketCategoryId, ticketCategoryId)
                            .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode()));
            seatVoList = movieScreeningSeatList.stream()
                    .map(this::buildSeatVo)
                    .sorted(Comparator.comparingInt(SeatVo::getRowCode).thenComparingInt(SeatVo::getColCode))
                    .collect(Collectors.toList());
            long cacheExpireTime = Math.max(1L, expireTime);
            Map<Integer, List<SeatVo>> seatMap = seatVoList.stream().collect(Collectors.groupingBy(SeatVo::getSellStatus));
            putSeatHash(screeningId, ticketCategoryId, RedisKeyManage.MOVIE_SCREENING_SEAT_NO_SOLD_RESOLUTION_HASH,
                    seatMap.get(SellStatus.NO_SOLD.getCode()), cacheExpireTime, timeUnit);
            putSeatHash(screeningId, ticketCategoryId, RedisKeyManage.MOVIE_SCREENING_SEAT_LOCK_RESOLUTION_HASH,
                    seatMap.get(SellStatus.LOCK.getCode()), cacheExpireTime, timeUnit);
            putSeatHash(screeningId, ticketCategoryId, RedisKeyManage.MOVIE_SCREENING_SEAT_SOLD_RESOLUTION_HASH,
                    seatMap.get(SellStatus.SOLD.getCode()), cacheExpireTime, timeUnit);
            return seatVoList;
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Long> getRemainNumberResolution(Long screeningId, Long ticketCategoryId) {
        Map<String, Long> remainNumberMap =
                redisCache.getAllMapForHash(RedisKeyBuild.createRedisKey(
                        RedisKeyManage.MOVIE_SCREENING_TICKET_REMAIN_NUMBER_HASH_RESOLUTION,
                        screeningId, ticketCategoryId), Long.class);
        if (CollectionUtil.isNotEmpty(remainNumberMap)) {
            return remainNumberMap;
        }
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, GET_REMAIN_NUMBER_LOCK,
                new String[]{"movie", String.valueOf(screeningId), String.valueOf(ticketCategoryId)});
        lock.lock();
        try {
            remainNumberMap = redisCache.getAllMapForHash(RedisKeyBuild.createRedisKey(
                    RedisKeyManage.MOVIE_SCREENING_TICKET_REMAIN_NUMBER_HASH_RESOLUTION,
                    screeningId, ticketCategoryId), Long.class);
            if (CollectionUtil.isNotEmpty(remainNumberMap)) {
                return remainNumberMap;
            }
            List<MovieScreeningPrice> movieScreeningPriceList = movieScreeningPriceMapper.selectList(
                    Wrappers.lambdaQuery(MovieScreeningPrice.class)
                            .eq(MovieScreeningPrice::getScreeningId, screeningId)
                            .eq(MovieScreeningPrice::getTicketCategoryId, ticketCategoryId)
                            .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
            Map<String, Long> map = movieScreeningPriceList.stream()
                    .collect(Collectors.toMap(movieScreeningPrice -> String.valueOf(movieScreeningPrice.getTicketCategoryId()),
                            MovieScreeningPrice::getRemainNumber, (v1, v2) -> v2));
            redisCache.putHash(RedisKeyBuild.createRedisKey(
                    RedisKeyManage.MOVIE_SCREENING_TICKET_REMAIN_NUMBER_HASH_RESOLUTION,
                    screeningId, ticketCategoryId), map);
            return map;
        } finally {
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean operateSeatLockAndTicketCategoryRemainNumber(ReduceRemainNumberDto reduceRemainNumberDto) {
        MovieScreening movieScreening = selectSellingScreening(reduceRemainNumberDto.getScreeningId());
        List<Long> seatIdList = reduceRemainNumberDto.getSeatIdList();
        List<MovieScreeningSeat> seatList = selectScreeningSeatList(reduceRemainNumberDto.getScreeningId(), seatIdList);
        for (MovieScreeningSeat seat : seatList) {
            if (!Objects.equals(seat.getSellStatus(), SellStatus.NO_SOLD.getCode())) {
                throw new TidesFrameException(BaseCode.SEAT_IS_NOT_NOT_SOLD);
            }
        }
        updateScreeningSeatStatus(reduceRemainNumberDto.getScreeningId(), seatIdList,
                reduceRemainNumberDto.getSellStatus(), List.of(SellStatus.NO_SOLD.getCode()));
        Map<Long, RemainChangeSnapshot> remainSnapshotMap = reduceScreeningRemainNumber(
                reduceRemainNumberDto.getScreeningId(), reduceRemainNumberDto.getTicketCategoryCountDtoList());
        recordInventoryEvents(movieScreening, seatList, reduceRemainNumberDto.getSellStatus(), remainSnapshotMap,
                INVENTORY_EVENT_LOCK, defaultString(reduceRemainNumberDto.getSourceType(), INVENTORY_SOURCE_ORDER_CREATE_LOCK),
                reduceRemainNumberDto.getBizNo(), reduceRemainNumberDto.getOperatorId(), "订单创建锁座");
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean operateProgramData(ProgramOperateDataDto programOperateDataDto) {
        MovieScreening movieScreening = selectSellingScreening(programOperateDataDto.getScreeningId());
        List<Long> seatIdList = programOperateDataDto.getSeatIdList();
        List<MovieScreeningSeat> seatList = selectScreeningSeatList(programOperateDataDto.getScreeningId(), seatIdList);
        if (!Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.SOLD.getCode()) &&
                !Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.NO_SOLD.getCode())) {
            throw new TidesFrameException(BaseCode.SEAT_OPERATE_IS_NOT_NOT_SOLD_OR_SOLD);
        }
        Map<Long, RemainChangeSnapshot> remainSnapshotMap = new HashMap<>();
        Integer orderVersion = programOperateDataDto.getOrderVersion();
        if (!ProgramOrderVersion.isV4Version(orderVersion)) {
            if (Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.SOLD.getCode())) {
                for (MovieScreeningSeat seat : seatList) {
                    if (Objects.equals(seat.getSellStatus(), SellStatus.SOLD.getCode())) {
                        throw new TidesFrameException(BaseCode.SEAT_SOLD);
                    }
                }
                updateScreeningSeatStatus(programOperateDataDto.getScreeningId(), seatIdList, SellStatus.SOLD.getCode());
                remainSnapshotMap = reduceScreeningRemainNumber(programOperateDataDto.getScreeningId(),
                        programOperateDataDto.getTicketCategoryCountDtoList());
            } else if (Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.NO_SOLD.getCode())) {
                updateScreeningSeatStatus(programOperateDataDto.getScreeningId(), seatIdList, SellStatus.NO_SOLD.getCode());
                remainSnapshotMap = increaseScreeningRemainNumber(programOperateDataDto.getScreeningId(),
                        programOperateDataDto.getTicketCategoryCountDtoList());
            }
        } else {
            for (MovieScreeningSeat seat : seatList) {
                if (Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.SOLD.getCode()) &&
                        !Objects.equals(seat.getSellStatus(), SellStatus.LOCK.getCode())) {
                    throw new TidesFrameException(BaseCode.SEAT_IS_NOT_NOT_LOCK);
                }
                if (Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.NO_SOLD.getCode()) &&
                        !Objects.equals(seat.getSellStatus(), SellStatus.LOCK.getCode()) &&
                        !Objects.equals(seat.getSellStatus(), SellStatus.SOLD.getCode())) {
                    throw new TidesFrameException(BaseCode.TICKET_STATUS_NOT_PERMIT);
                }
            }
            if (Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.SOLD.getCode())) {
                updateScreeningSeatStatus(programOperateDataDto.getScreeningId(), seatIdList,
                        programOperateDataDto.getSellStatus(), List.of(SellStatus.LOCK.getCode()));
            } else {
                updateScreeningSeatStatus(programOperateDataDto.getScreeningId(), seatIdList,
                        programOperateDataDto.getSellStatus(), List.of(SellStatus.LOCK.getCode(), SellStatus.SOLD.getCode()));
            }
            if (Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.NO_SOLD.getCode())) {
                remainSnapshotMap = increaseScreeningRemainNumber(programOperateDataDto.getScreeningId(),
                        programOperateDataDto.getTicketCategoryCountDtoList());
            }
        }
        recordInventoryEvents(movieScreening, seatList, programOperateDataDto.getSellStatus(), remainSnapshotMap,
                eventTypeBySellStatus(programOperateDataDto.getSellStatus()),
                defaultString(programOperateDataDto.getSourceType(),
                        Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.SOLD.getCode()) ?
                                INVENTORY_SOURCE_ORDER_PAY : INVENTORY_SOURCE_ORDER_RELEASE),
                programOperateDataDto.getBizNo(), programOperateDataDto.getOperatorId(), "订单状态驱动库存变更");
        return true;
    }

    private Movie selectMovie(Long programId, Long movieId) {
        if (Objects.isNull(programId) && Objects.isNull(movieId)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        Movie movie = movieMapper.selectOne(Wrappers.lambdaQuery(Movie.class)
                .eq(Objects.nonNull(programId), Movie::getProgramId, programId)
                .eq(Objects.nonNull(movieId), Movie::getId, movieId)
                .eq(Movie::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(movie)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return movie;
    }

    private Cinema selectCinema(Long cinemaId) {
        Cinema cinema = cinemaMapper.selectOne(Wrappers.lambdaQuery(Cinema.class)
                .eq(Cinema::getId, cinemaId)
                .eq(Cinema::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(cinema)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return cinema;
    }

    private Set<Long> selectCinemaIdFilterSet(MovieCinemaPageDto movieCinemaPageDto) {
        boolean needScreeningFilter = Objects.nonNull(movieCinemaPageDto.getProgramId()) ||
                Objects.nonNull(movieCinemaPageDto.getMovieId()) ||
                Objects.nonNull(movieCinemaPageDto.getShowDayTime());
        if (!needScreeningFilter) {
            return null;
        }
        Long programId = movieCinemaPageDto.getProgramId();
        Long movieId = movieCinemaPageDto.getMovieId();
        if (Objects.isNull(programId) && Objects.nonNull(movieId)) {
            Movie movie = selectMovie(null, movieId);
            programId = movie.getProgramId();
        }
        return selectSellingScreeningList(programId, movieId, null, movieCinemaPageDto.getShowDayTime(), true).stream()
                .map(MovieScreening::getCinemaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<MovieScreening> selectSellingScreeningList(Long programId, Long movieId, Long cinemaId,
                                                            Date showDayTime, boolean futureOnly) {
        return selectSellingScreeningList(programId, movieId, cinemaId, null, showDayTime, futureOnly);
    }

    private List<MovieScreening> selectSellingScreeningList(Long programId, Long movieId, Long cinemaId,
                                                            Set<Long> cinemaIdSet, Date showDayTime,
                                                            boolean futureOnly) {
        return movieScreeningMapper.selectList(Wrappers.lambdaQuery(MovieScreening.class)
                .eq(Objects.nonNull(programId), MovieScreening::getProgramId, programId)
                .eq(Objects.nonNull(movieId), MovieScreening::getMovieId, movieId)
                .eq(Objects.nonNull(cinemaId), MovieScreening::getCinemaId, cinemaId)
                .in(CollectionUtil.isNotEmpty(cinemaIdSet), MovieScreening::getCinemaId, cinemaIdSet)
                .eq(Objects.nonNull(showDayTime), MovieScreening::getShowDayTime, showDayTime)
                .ge(futureOnly, MovieScreening::getShowTime, DateUtils.now())
                .eq(MovieScreening::getScreeningStatus, BusinessStatus.YES.getCode())
                .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode())
                .orderByAsc(MovieScreening::getShowTime));
    }

    private List<MovieScreeningVo> buildMovieScreeningVoList(List<MovieScreening> movieScreeningList) {
        if (CollectionUtil.isEmpty(movieScreeningList)) {
            return new ArrayList<>();
        }
        Map<Long, Cinema> cinemaMap = selectCinemaMap(movieScreeningList);
        Map<Long, Hall> hallMap = selectHallMap(movieScreeningList);
        return movieScreeningList.stream()
                .map(movieScreening -> buildMovieScreeningVo(movieScreening, cinemaMap, hallMap))
                .toList();
    }

    private List<MovieArtistVo> selectMovieArtistList(Long programId) {
        List<ProgramArtist> programArtistList = programArtistMapper.selectList(Wrappers.lambdaQuery(ProgramArtist.class)
                .eq(ProgramArtist::getProgramId, programId)
                .eq(ProgramArtist::getDisplayFlag, BusinessStatus.YES.getCode())
                .eq(ProgramArtist::getStatus, BusinessStatus.YES.getCode())
                .orderByAsc(ProgramArtist::getSortOrder));
        if (CollectionUtil.isEmpty(programArtistList)) {
            return new ArrayList<>();
        }
        List<Long> artistIdList = programArtistList.stream()
                        .map(ProgramArtist::getArtistId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (CollectionUtil.isEmpty(artistIdList)) {
            return new ArrayList<>();
        }
        Map<Long, Artist> artistMap = artistMapper.selectBatchIds(artistIdList)
                .stream()
                .collect(Collectors.toMap(Artist::getId, artist -> artist, (v1, v2) -> v2));
        return programArtistList.stream().map(programArtist -> {
            Artist artist = artistMap.get(programArtist.getArtistId());
            if (Objects.isNull(artist)) {
                return null;
            }
            MovieArtistVo movieArtistVo = new MovieArtistVo();
            movieArtistVo.setArtistId(artist.getId());
            movieArtistVo.setArtistName(artist.getArtistName());
            movieArtistVo.setEnglishName(artist.getEnglishName());
            movieArtistVo.setAvatar(artist.getAvatar());
            movieArtistVo.setRegion(artist.getRegion());
            movieArtistVo.setProfession(artist.getProfession());
            movieArtistVo.setRepresentativeWorks(artist.getRepresentativeWorks());
            movieArtistVo.setRoleType(programArtist.getRoleType());
            movieArtistVo.setRoleName(programArtist.getRoleName());
            movieArtistVo.setSortOrder(programArtist.getSortOrder());
            return movieArtistVo;
        }).filter(Objects::nonNull).toList();
    }

    private List<MovieMediaVo> selectMovieMediaList(Long movieId) {
        List<MovieMedia> movieMediaList = movieMediaMapper.selectList(Wrappers.lambdaQuery(MovieMedia.class)
                .eq(MovieMedia::getMovieId, movieId)
                .eq(MovieMedia::getAuditStatus, BusinessStatus.YES.getCode())
                .eq(MovieMedia::getStatus, BusinessStatus.YES.getCode())
                .orderByAsc(MovieMedia::getSortOrder));
        if (CollectionUtil.isEmpty(movieMediaList)) {
            return new ArrayList<>();
        }
        return movieMediaList.stream().map(movieMedia -> {
            MovieMediaVo movieMediaVo = new MovieMediaVo();
            BeanUtil.copyProperties(movieMedia, movieMediaVo);
            return movieMediaVo;
        }).toList();
    }

    private Map<Long, String> selectAreaNameMap(Collection<Long> areaIdList) {
        if (CollectionUtil.isEmpty(areaIdList)) {
            return new HashMap<>(0);
        }
        AreaSelectDto areaSelectDto = new AreaSelectDto();
        areaSelectDto.setIdList(areaIdList.stream().filter(Objects::nonNull).distinct().toList());
        if (CollectionUtil.isEmpty(areaSelectDto.getIdList())) {
            return new HashMap<>(0);
        }
        ApiResponse<List<AreaVo>> areaResponse = baseDataClient.selectByIdList(areaSelectDto);
        if (!Objects.equals(areaResponse.getCode(), ApiResponse.ok().getCode())) {
            log.error("base-data selectByIdList rpc error areaResponse:{}", areaResponse);
            return new HashMap<>(0);
        }
        if (CollectionUtil.isEmpty(areaResponse.getData())) {
            return new HashMap<>(0);
        }
        return areaResponse.getData().stream()
                .collect(Collectors.toMap(AreaVo::getId, AreaVo::getName, (v1, v2) -> v2));
    }

    private List<MovieHallVo> selectHallList(Long cinemaId) {
        List<Hall> hallList = hallMapper.selectList(Wrappers.lambdaQuery(Hall.class)
                .eq(Hall::getCinemaId, cinemaId)
                .eq(Hall::getStatus, BusinessStatus.YES.getCode())
                .orderByAsc(Hall::getId));
        if (CollectionUtil.isEmpty(hallList)) {
            return new ArrayList<>();
        }
        return hallList.stream().map(hall -> {
            MovieHallVo movieHallVo = new MovieHallVo();
            BeanUtil.copyProperties(hall, movieHallVo);
            return movieHallVo;
        }).toList();
    }

    private Map<Long, Movie> selectMovieMapByProgramIds(Collection<Long> programIdList) {
        if (CollectionUtil.isEmpty(programIdList)) {
            return new HashMap<>(0);
        }
        return movieMapper.selectList(Wrappers.lambdaQuery(Movie.class)
                        .in(Movie::getProgramId, programIdList)
                        .eq(Movie::getStatus, BusinessStatus.YES.getCode()))
                .stream()
                .collect(Collectors.toMap(Movie::getProgramId, movie -> movie, (v1, v2) -> v2));
    }

    private Map<Long, MovieProfile> selectMovieProfileMapByProgramIds(Collection<Long> programIdList) {
        if (CollectionUtil.isEmpty(programIdList)) {
            return new HashMap<>(0);
        }
        return movieProfileMapper.selectList(Wrappers.lambdaQuery(MovieProfile.class)
                        .in(MovieProfile::getProgramId, programIdList)
                        .eq(MovieProfile::getStatus, BusinessStatus.YES.getCode()))
                .stream()
                .collect(Collectors.toMap(MovieProfile::getProgramId, movieProfile -> movieProfile, (v1, v2) -> v2));
    }

    private Set<Long> selectCinemaIdSetByAreaId(Long areaId) {
        if (Objects.isNull(areaId) || isAllArea(areaId)) {
            return null;
        }
        String areaName = resolveAreaName(areaId);
        List<Cinema> cinemaList = cinemaMapper.selectList(Wrappers.lambdaQuery(Cinema.class)
                .and(wrapper -> wrapper.eq(Cinema::getAreaId, areaId)
                        .or(StringUtil.isNotEmpty(areaName), sub -> sub.eq(Cinema::getCityName, areaName))
                        .or(StringUtil.isNotEmpty(areaName), sub -> sub.eq(Cinema::getDistrictName, areaName)))
                .eq(Cinema::getStatus, BusinessStatus.YES.getCode()));
        if (CollectionUtil.isEmpty(cinemaList)) {
            return new HashSet<>();
        }
        return cinemaList.stream().map(Cinema::getId).collect(Collectors.toSet());
    }

    private String resolveAreaName(Long areaId) {
        Map<Long, String> knownAreaNameMap = Map.of(
                2L, "北京",
                52L, "朝阳区",
                385L, "嘉兴",
                3248L, "南湖区");
        try {
            AreaGetDto areaGetDto = new AreaGetDto();
            areaGetDto.setId(areaId);
            ApiResponse<AreaVo> areaResponse = baseDataClient.getById(areaGetDto);
            if (Objects.equals(areaResponse.getCode(), ApiResponse.ok().getCode()) &&
                    Objects.nonNull(areaResponse.getData()) &&
                    StringUtil.isNotEmpty(areaResponse.getData().getName())) {
                return areaResponse.getData().getName();
            }
        } catch (Exception exception) {
            log.warn("base-data getById rpc error areaId:{}", areaId, exception);
        }
        return knownAreaNameMap.get(areaId);
    }

    private boolean isAllArea(Long areaId) {
        return Objects.equals(areaId, NATIONAL_AREA_ID) || Objects.equals(areaId, 0L);
    }

    private Integer releaseStatusByCategory(Long programCategoryId, Integer releaseStatus) {
        if (Objects.nonNull(releaseStatus)) {
            return releaseStatus;
        }
        if (Objects.equals(programCategoryId, 23L)) {
            return 2;
        }
        if (Objects.equals(programCategoryId, 24L)) {
            return 1;
        }
        return null;
    }

    private MovieListVo buildMovieListVo(Movie movie, MovieProfile movieProfile,
                                         List<MovieScreening> movieScreeningList) {
        if (Objects.isNull(movie) || CollectionUtil.isEmpty(movieScreeningList)) {
            return null;
        }
        MovieListVo movieListVo = new MovieListVo();
        BeanUtil.copyProperties(movie, movieListVo);
        movieListVo.setMovieId(movie.getId());
        movieListVo.setProgramId(movie.getProgramId());
        if (Objects.nonNull(movieProfile)) {
            BeanUtil.copyProperties(movieProfile, movieListVo);
            movieListVo.setMovieId(movie.getId());
            movieListVo.setProgramId(movie.getProgramId());
        }
        CinemaScreeningSummary cinemaScreeningSummary = buildCinemaScreeningSummary(movieScreeningList);
        movieListVo.setLowestPrice(cinemaScreeningSummary.lowestPrice());
        movieListVo.setNearestShowTime(cinemaScreeningSummary.nearestShowTime());
        movieListVo.setScreeningCount(cinemaScreeningSummary.screeningCount());
        movieListVo.setCinemaCount((int) movieScreeningList.stream()
                .map(MovieScreening::getCinemaId)
                .filter(Objects::nonNull)
                .distinct()
                .count());
        return movieListVo;
    }

    private Map<Long, CinemaScreeningSummary> selectCinemaScreeningSummaryMap(List<Long> cinemaIdList, Long programId,
                                                                              Long movieId, Date showDayTime) {
        if (CollectionUtil.isEmpty(cinemaIdList)) {
            return new HashMap<>(0);
        }
        List<MovieScreening> movieScreeningList = movieScreeningMapper.selectList(Wrappers.lambdaQuery(MovieScreening.class)
                .in(MovieScreening::getCinemaId, cinemaIdList)
                .eq(Objects.nonNull(programId), MovieScreening::getProgramId, programId)
                .eq(Objects.nonNull(movieId), MovieScreening::getMovieId, movieId)
                .eq(Objects.nonNull(showDayTime), MovieScreening::getShowDayTime, showDayTime)
                .ge(MovieScreening::getShowTime, DateUtils.now())
                .eq(MovieScreening::getScreeningStatus, BusinessStatus.YES.getCode())
                .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode()));
        if (CollectionUtil.isEmpty(movieScreeningList)) {
            return new HashMap<>(0);
        }
        return movieScreeningList.stream()
                .collect(Collectors.groupingBy(MovieScreening::getCinemaId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> buildCinemaScreeningSummary(entry.getValue())));
    }

    private MovieCinemaVo buildMovieCinemaVo(Cinema cinema, Map<Long, String> areaNameMap,
                                             CinemaScreeningSummary cinemaScreeningSummary) {
        MovieCinemaVo movieCinemaVo = new MovieCinemaVo();
        BeanUtil.copyProperties(cinema, movieCinemaVo);
        movieCinemaVo.setAreaName(areaNameMap.get(cinema.getAreaId()));
        if (Objects.nonNull(cinemaScreeningSummary)) {
            movieCinemaVo.setLowestPrice(cinemaScreeningSummary.lowestPrice());
            movieCinemaVo.setNearestShowTime(cinemaScreeningSummary.nearestShowTime());
            movieCinemaVo.setScreeningCount(cinemaScreeningSummary.screeningCount());
        }
        return movieCinemaVo;
    }

    private MovieCinemaMovieVo buildMovieCinemaMovieVo(Movie movie, List<MovieScreening> movieScreeningList) {
        if (Objects.isNull(movie) || CollectionUtil.isEmpty(movieScreeningList)) {
            return null;
        }
        MovieCinemaMovieVo movieCinemaMovieVo = new MovieCinemaMovieVo();
        movieCinemaMovieVo.setMovieId(movie.getId());
        movieCinemaMovieVo.setProgramId(movie.getProgramId());
        movieCinemaMovieVo.setMovieName(movie.getMovieName());
        movieCinemaMovieVo.setPoster(movie.getPoster());
        movieCinemaMovieVo.setDurationMinutes(movie.getDurationMinutes());
        movieCinemaMovieVo.setLanguage(movie.getLanguage());
        movieCinemaMovieVo.setRegion(movie.getRegion());
        movieCinemaMovieVo.setReleaseDate(movie.getReleaseDate());
        CinemaScreeningSummary cinemaScreeningSummary = buildCinemaScreeningSummary(movieScreeningList);
        movieCinemaMovieVo.setLowestPrice(cinemaScreeningSummary.lowestPrice());
        movieCinemaMovieVo.setNearestShowTime(cinemaScreeningSummary.nearestShowTime());
        movieCinemaMovieVo.setScreeningCount(cinemaScreeningSummary.screeningCount());
        return movieCinemaMovieVo;
    }

    private CinemaScreeningSummary buildCinemaScreeningSummary(List<MovieScreening> movieScreeningList) {
        return new CinemaScreeningSummary(minLowestPrice(movieScreeningList), nearestShowTime(movieScreeningList),
                movieScreeningList.size());
    }

    private BigDecimal minLowestPrice(List<MovieScreening> movieScreeningList) {
        return movieScreeningList.stream()
                .map(MovieScreening::getLowestPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private Date nearestShowTime(List<MovieScreening> movieScreeningList) {
        return movieScreeningList.stream()
                .map(MovieScreening::getShowTime)
                .filter(Objects::nonNull)
                .min(Date::compareTo)
                .orElse(null);
    }

    private MovieScreeningVo buildMovieScreeningVo(MovieScreening movieScreening,
                                                   Map<Long, Cinema> cinemaMap,
                                                   Map<Long, Hall> hallMap) {
        MovieScreeningVo movieScreeningVo = new MovieScreeningVo();
        BeanUtil.copyProperties(movieScreening, movieScreeningVo);
        Cinema cinema = cinemaMap.get(movieScreening.getCinemaId());
        if (Objects.nonNull(cinema)) {
            movieScreeningVo.setCinemaName(cinema.getCinemaName());
            movieScreeningVo.setCinemaAddress(cinema.getAddress());
        }
        Hall hall = hallMap.get(movieScreening.getHallId());
        if (Objects.nonNull(hall)) {
            movieScreeningVo.setHallName(hall.getHallName());
            movieScreeningVo.setHallType(hall.getHallType());
        }
        return movieScreeningVo;
    }

    private SeatVo buildSeatVo(MovieScreeningSeat movieScreeningSeat) {
        SeatVo seatVo = new SeatVo();
        seatVo.setId(movieScreeningSeat.getId());
        seatVo.setProgramId(movieScreeningSeat.getProgramId());
        seatVo.setTicketCategoryId(movieScreeningSeat.getTicketCategoryId());
        seatVo.setRowCode(movieScreeningSeat.getRowCode());
        seatVo.setColCode(movieScreeningSeat.getColCode());
        seatVo.setSeatNo(movieScreeningSeat.getSeatNo());
        seatVo.setZoneName(movieScreeningSeat.getZoneName());
        seatVo.setPriceLevel(movieScreeningSeat.getPriceLevel());
        seatVo.setSeatType(movieScreeningSeat.getSeatType());
        seatVo.setSeatTypeName(SeatType.getMsg(movieScreeningSeat.getSeatType()));
        seatVo.setPrice(movieScreeningSeat.getPrice());
        seatVo.setSellStatus(movieScreeningSeat.getSellStatus());
        seatVo.setAisleFlag(movieScreeningSeat.getAisleFlag());
        seatVo.setCoupleFlag(movieScreeningSeat.getCoupleFlag());
        seatVo.setAccessibleFlag(movieScreeningSeat.getAccessibleFlag());
        seatVo.setVipFlag(movieScreeningSeat.getVipFlag());
        seatVo.setRepairFlag(movieScreeningSeat.getRepairFlag());
        seatVo.setSellableFlag(movieScreeningSeat.getSellableFlag());
        return seatVo;
    }

    private List<SeatVo> mergeSeatVoById(List<SeatVo> seatVoList) {
        if (CollectionUtil.isEmpty(seatVoList)) {
            return new ArrayList<>();
        }
        Map<Long, SeatVo> seatVoMap = new LinkedHashMap<>(seatVoList.size());
        for (SeatVo seatVo : seatVoList) {
            SeatVo existsSeatVo = seatVoMap.get(seatVo.getId());
            if (Objects.isNull(existsSeatVo) ||
                    seatStatusPriority(seatVo.getSellStatus()) > seatStatusPriority(existsSeatVo.getSellStatus())) {
                seatVoMap.put(seatVo.getId(), seatVo);
            }
        }
        return seatVoMap.values().stream()
                .sorted(Comparator.comparingInt(SeatVo::getRowCode).thenComparingInt(SeatVo::getColCode))
                .collect(Collectors.toList());
    }

    private int seatStatusPriority(Integer sellStatus) {
        if (Objects.equals(sellStatus, SellStatus.SOLD.getCode())) {
            return 3;
        }
        if (Objects.equals(sellStatus, SellStatus.LOCK.getCode())) {
            return 2;
        }
        if (Objects.equals(sellStatus, SellStatus.NO_SOLD.getCode())) {
            return 1;
        }
        return 0;
    }

    private List<SeatVo> getSeatVoListByCacheResolution(Long screeningId, Long ticketCategoryId) {
        List<String> keys = new ArrayList<>(4);
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.MOVIE_SCREENING_SEAT_NO_SOLD_RESOLUTION_HASH,
                screeningId, ticketCategoryId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.MOVIE_SCREENING_SEAT_LOCK_RESOLUTION_HASH,
                screeningId, ticketCategoryId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.MOVIE_SCREENING_SEAT_SOLD_RESOLUTION_HASH,
                screeningId, ticketCategoryId).getRelKey());
        return programSeatCacheData.getData(keys, new String[]{});
    }

    private void putSeatHash(Long screeningId, Long ticketCategoryId, RedisKeyManage redisKeyManage,
                             List<SeatVo> seatVoList, Long expireTime, TimeUnit timeUnit) {
        if (CollectionUtil.isEmpty(seatVoList)) {
            return;
        }
        redisCache.putHash(RedisKeyBuild.createRedisKey(redisKeyManage, screeningId, ticketCategoryId),
                seatVoList.stream().collect(Collectors.toMap(seatVo -> String.valueOf(seatVo.getId()),
                        seatVo -> seatVo, (v1, v2) -> v2)), expireTime, timeUnit);
    }

    private List<MovieScreeningSeat> selectScreeningSeatList(Long screeningId, List<Long> seatIdList) {
        List<MovieScreeningSeat> seatList = movieScreeningSeatMapper.selectList(
                Wrappers.lambdaQuery(MovieScreeningSeat.class)
                        .eq(MovieScreeningSeat::getScreeningId, screeningId)
                        .eq(MovieScreeningSeat::getStatus, BusinessStatus.YES.getCode())
                        .in(MovieScreeningSeat::getId, seatIdList));
        if (CollectionUtil.isEmpty(seatList)) {
            throw new TidesFrameException(BaseCode.SEAT_NOT_EXIST);
        }
        if (seatList.size() != seatIdList.size()) {
            throw new TidesFrameException(BaseCode.SEAT_UPDATE_REL_COUNT_NOT_EQUAL_PRESET_COUNT);
        }
        return seatList;
    }

    private void updateScreeningSeatStatus(Long screeningId, List<Long> seatIdList, Integer sellStatus) {
        updateScreeningSeatStatus(screeningId, seatIdList, sellStatus, null);
    }

    private void updateScreeningSeatStatus(Long screeningId, List<Long> seatIdList, Integer sellStatus,
                                           List<Integer> beforeSellStatusList) {
        MovieScreeningSeat updateSeat = new MovieScreeningSeat();
        updateSeat.setSellStatus(sellStatus);
        updateSeat.setEditTime(DateUtils.now());
        int updateCount = movieScreeningSeatMapper.update(updateSeat, Wrappers.lambdaUpdate(MovieScreeningSeat.class)
                .eq(MovieScreeningSeat::getScreeningId, screeningId)
                .in(MovieScreeningSeat::getId, seatIdList)
                .in(CollectionUtil.isNotEmpty(beforeSellStatusList), MovieScreeningSeat::getSellStatus, beforeSellStatusList));
        if (updateCount != seatIdList.size()) {
            throw new TidesFrameException(BaseCode.SEAT_UPDATE_REL_COUNT_NOT_EQUAL_PRESET_COUNT);
        }
    }

    private Map<Long, RemainChangeSnapshot> reduceScreeningRemainNumber(Long screeningId,
                                                                        List<TicketCategoryCountDto> ticketCategoryCountDtoList) {
        Map<Long, RemainChangeSnapshot> remainSnapshotMap = new HashMap<>(ticketCategoryCountDtoList.size());
        for (TicketCategoryCountDto ticketCategoryCountDto : ticketCategoryCountDtoList) {
            RemainChangeSnapshot remainChangeSnapshot = updateScreeningRemainNumber(screeningId, ticketCategoryCountDto, true);
            remainSnapshotMap.put(ticketCategoryCountDto.getTicketCategoryId(), remainChangeSnapshot);
        }
        return remainSnapshotMap;
    }

    private Map<Long, RemainChangeSnapshot> increaseScreeningRemainNumber(Long screeningId,
                                                                          List<TicketCategoryCountDto> ticketCategoryCountDtoList) {
        Map<Long, RemainChangeSnapshot> remainSnapshotMap = new HashMap<>(ticketCategoryCountDtoList.size());
        for (TicketCategoryCountDto ticketCategoryCountDto : ticketCategoryCountDtoList) {
            RemainChangeSnapshot remainChangeSnapshot = updateScreeningRemainNumber(screeningId, ticketCategoryCountDto, false);
            remainSnapshotMap.put(ticketCategoryCountDto.getTicketCategoryId(), remainChangeSnapshot);
        }
        return remainSnapshotMap;
    }

    private RemainChangeSnapshot updateScreeningRemainNumber(Long screeningId, TicketCategoryCountDto ticketCategoryCountDto,
                                                             boolean reduce) {
        Long count = ticketCategoryCountDto.getCount();
        if (Objects.isNull(count) || count < 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        MovieScreeningPrice movieScreeningPrice = movieScreeningPriceMapper.selectOne(Wrappers.lambdaQuery(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, screeningId)
                .eq(MovieScreeningPrice::getTicketCategoryId, ticketCategoryCountDto.getTicketCategoryId())
                .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode())
                .last("for update"));
        if (Objects.isNull(movieScreeningPrice)) {
            throw new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2);
        }
        Long beforeRemainNumber = Objects.isNull(movieScreeningPrice.getRemainNumber()) ? 0L : movieScreeningPrice.getRemainNumber();
        if (reduce && beforeRemainNumber < count) {
            throw new TidesFrameException(BaseCode.UPDATE_TICKET_CATEGORY_COUNT_NOT_CORRECT);
        }
        if (!reduce && Objects.nonNull(movieScreeningPrice.getTotalNumber()) &&
                beforeRemainNumber + count > movieScreeningPrice.getTotalNumber()) {
            throw new TidesFrameException(BaseCode.UPDATE_TICKET_CATEGORY_COUNT_NOT_CORRECT);
        }
        Long afterRemainNumber = reduce ? beforeRemainNumber - count : beforeRemainNumber + count;
        MovieScreeningPrice updateMovieScreeningPrice = new MovieScreeningPrice();
        updateMovieScreeningPrice.setRemainNumber(afterRemainNumber);
        updateMovieScreeningPrice.setEditTime(DateUtils.now());
        int updateCount = movieScreeningPriceMapper.update(updateMovieScreeningPrice, Wrappers.lambdaUpdate(MovieScreeningPrice.class)
                .eq(MovieScreeningPrice::getScreeningId, screeningId)
                .eq(MovieScreeningPrice::getId, movieScreeningPrice.getId())
                .eq(MovieScreeningPrice::getTicketCategoryId, ticketCategoryCountDto.getTicketCategoryId())
                .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.UPDATE_TICKET_CATEGORY_COUNT_NOT_CORRECT);
        }
        long changeCount = reduce ? -count : count;
        return new RemainChangeSnapshot(ticketCategoryCountDto.getTicketCategoryId(), beforeRemainNumber,
                afterRemainNumber, changeCount);
    }

    private void recordInventoryEvents(MovieScreening movieScreening, List<MovieScreeningSeat> beforeSeatList,
                                       Integer afterSellStatus, Map<Long, RemainChangeSnapshot> remainSnapshotMap,
                                       String eventType, String sourceType, String bizNo, Long operatorId,
                                       String remark) {
        if (CollectionUtil.isEmpty(beforeSeatList)) {
            return;
        }
        Map<Long, RemainChangeSnapshot> fullRemainSnapshotMap = fillRemainSnapshotMap(movieScreening.getId(),
                beforeSeatList, remainSnapshotMap);
        Date now = DateUtils.now();
        for (MovieScreeningSeat beforeSeat : beforeSeatList) {
            MovieInventoryEvent movieInventoryEvent = new MovieInventoryEvent();
            movieInventoryEvent.setId(uidGenerator.getUid());
            movieInventoryEvent.setScreeningId(movieScreening.getId());
            movieInventoryEvent.setProgramId(movieScreening.getProgramId());
            movieInventoryEvent.setTicketCategoryId(beforeSeat.getTicketCategoryId());
            movieInventoryEvent.setSeatId(beforeSeat.getId());
            movieInventoryEvent.setEventType(eventType);
            movieInventoryEvent.setBeforeSellStatus(beforeSeat.getSellStatus());
            movieInventoryEvent.setAfterSellStatus(afterSellStatus);
            RemainChangeSnapshot remainChangeSnapshot = fullRemainSnapshotMap.get(beforeSeat.getTicketCategoryId());
            if (Objects.nonNull(remainChangeSnapshot)) {
                movieInventoryEvent.setBeforeRemainNumber(remainChangeSnapshot.beforeRemainNumber());
                movieInventoryEvent.setAfterRemainNumber(remainChangeSnapshot.afterRemainNumber());
            }
            movieInventoryEvent.setChangeCount(remainDeltaBySeatStatus(beforeSeat.getSellStatus(), afterSellStatus));
            movieInventoryEvent.setSourceType(sourceType);
            movieInventoryEvent.setBizNo(bizNo);
            movieInventoryEvent.setOperatorId(operatorId);
            movieInventoryEvent.setEventTime(now);
            movieInventoryEvent.setRemark(remark);
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

    private Long remainDeltaBySeatStatus(Integer beforeSellStatus, Integer afterSellStatus) {
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

    private String eventTypeBySellStatus(Integer sellStatus) {
        if (Objects.equals(sellStatus, SellStatus.SOLD.getCode())) {
            return INVENTORY_EVENT_SOLD;
        }
        return INVENTORY_EVENT_RELEASE;
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtil.isNotEmpty(value) ? value : defaultValue;
    }

    private Map<Long, Cinema> selectCinemaMap(List<MovieScreening> movieScreeningList) {
        Set<Long> cinemaIdSet = movieScreeningList.stream()
                .map(MovieScreening::getCinemaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtil.isEmpty(cinemaIdSet)) {
            return new HashMap<>(0);
        }
        return cinemaMapper.selectBatchIds(cinemaIdSet).stream()
                .collect(Collectors.toMap(Cinema::getId, cinema -> cinema, (v1, v2) -> v2));
    }

    private Map<Long, Hall> selectHallMap(List<MovieScreening> movieScreeningList) {
        Set<Long> hallIdSet = movieScreeningList.stream()
                .map(MovieScreening::getHallId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtil.isEmpty(hallIdSet)) {
            return new HashMap<>(0);
        }
        return hallMapper.selectBatchIds(hallIdSet).stream()
                .collect(Collectors.toMap(Hall::getId, hall -> hall, (v1, v2) -> v2));
    }

    private String priceKey(BigDecimal price) {
        return price.stripTrailingZeros().toPlainString();
    }

    private record RemainChangeSnapshot(Long ticketCategoryId, Long beforeRemainNumber, Long afterRemainNumber,
                                        Long changeCount) {
    }

    private record CinemaScreeningSummary(BigDecimal lowestPrice, Date nearestShowTime, Integer screeningCount) {
    }
}
