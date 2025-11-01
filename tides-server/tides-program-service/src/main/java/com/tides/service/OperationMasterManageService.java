package com.tides.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tides.client.OrderClient;
import com.tides.common.ApiResponse;
import com.tides.dto.ArtistManageDto;
import com.tides.dto.ArtistSaveDto;
import com.tides.dto.ArtistStatusUpdateDto;
import com.tides.dto.CinemaManageDto;
import com.tides.dto.CinemaSaveDto;
import com.tides.dto.CinemaStatusUpdateDto;
import com.tides.dto.HallManageDto;
import com.tides.dto.HallSaveDto;
import com.tides.dto.HallSeatBatchGenerateDto;
import com.tides.dto.HallSeatPageManageDto;
import com.tides.dto.HallSeatSaveDto;
import com.tides.dto.HallSeatStatusUpdateDto;
import com.tides.dto.HallStatusUpdateDto;
import com.tides.dto.MovieManageDto;
import com.tides.dto.MovieMediaManageDto;
import com.tides.dto.MovieMediaSaveDto;
import com.tides.dto.MovieMediaStatusUpdateDto;
import com.tides.dto.MovieOrderSalesCountDto;
import com.tides.dto.MovieProfileManageDto;
import com.tides.dto.MovieProfileSaveDto;
import com.tides.dto.MovieProfileStatusUpdateDto;
import com.tides.dto.MovieSaveDto;
import com.tides.dto.MovieStatusUpdateDto;
import com.tides.dto.ProgramArtistManageDto;
import com.tides.dto.ProgramArtistSaveDto;
import com.tides.dto.ProgramArtistStatusUpdateDto;
import com.tides.dto.VenueManageDto;
import com.tides.dto.VenueSaveDto;
import com.tides.dto.VenueStatusUpdateDto;
import com.tides.entity.Artist;
import com.tides.entity.Cinema;
import com.tides.entity.Hall;
import com.tides.entity.HallSeat;
import com.tides.entity.Movie;
import com.tides.entity.MovieMedia;
import com.tides.entity.MovieProfile;
import com.tides.entity.MovieScreening;
import com.tides.entity.MovieScreeningPrice;
import com.tides.entity.Program;
import com.tides.entity.ProgramArtist;
import com.tides.entity.Venue;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.enums.SeatType;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.ArtistMapper;
import com.tides.mapper.CinemaMapper;
import com.tides.mapper.HallMapper;
import com.tides.mapper.HallSeatMapper;
import com.tides.mapper.MovieMapper;
import com.tides.mapper.MovieMediaMapper;
import com.tides.mapper.MovieProfileMapper;
import com.tides.mapper.MovieScreeningMapper;
import com.tides.mapper.MovieScreeningPriceMapper;
import com.tides.mapper.ProgramArtistMapper;
import com.tides.mapper.ProgramMapper;
import com.tides.mapper.VenueMapper;
import com.tides.page.PageUtil;
import com.tides.util.DateUtils;
import com.tides.util.StringUtil;
import com.tides.vo.ArtistManageVo;
import com.tides.vo.CinemaManageVo;
import com.tides.vo.HallManageVo;
import com.tides.vo.HallSeatManageVo;
import com.tides.vo.MovieManageVo;
import com.tides.vo.MovieMediaManageVo;
import com.tides.vo.MovieOrderSalesCountVo;
import com.tides.vo.MovieProfileManageVo;
import com.tides.vo.ProgramArtistManageVo;
import com.tides.vo.VenueManageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运营主数据后台管理 service，负责影片、影院、影厅、座位模板、场馆和影人等资料中心能力。
 */
@Slf4j
@Service
public class OperationMasterManageService  {
    @Autowired
    private UidGenerator uidGenerator;

    @Autowired
    private ProgramMapper programMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private CinemaMapper cinemaMapper;

    @Autowired
    private HallMapper hallMapper;

    @Autowired
    private HallSeatMapper hallSeatMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private ProgramArtistMapper programArtistMapper;

    @Autowired
    private MovieProfileMapper movieProfileMapper;

    @Autowired
    private MovieMediaMapper movieMediaMapper;

    @Autowired
    private MovieScreeningMapper movieScreeningMapper;

    @Autowired
    private MovieScreeningPriceMapper movieScreeningPriceMapper;

    @Autowired
    private OrderClient orderClient;
    public IPage<MovieManageVo> moviePage(MovieManageDto movieManageDto) {
        IPage<MovieManageVo> movieManageVoPage =
                new Page<>(movieManageDto.getPageNumber(), movieManageDto.getPageSize());
        Set<Long> areaMovieIdSet = selectMovieIdSetByArea(movieManageDto.getAreaId(), movieManageDto.getAreaIds());
        if (hasAreaFilter(movieManageDto.getAreaId(), movieManageDto.getAreaIds()) && CollectionUtil.isEmpty(areaMovieIdSet)) {
            return movieManageVoPage;
        }
        IPage<Movie> moviePage = movieMapper.selectPage(PageUtil.getPageParams(movieManageDto.getPageNumber(),
                movieManageDto.getPageSize()), Wrappers.lambdaQuery(Movie.class)
                .in(CollectionUtil.isNotEmpty(areaMovieIdSet), Movie::getId, areaMovieIdSet)
                .eq(Objects.nonNull(movieManageDto.getMovieId()), Movie::getId, movieManageDto.getMovieId())
                .eq(Objects.nonNull(movieManageDto.getProgramId()), Movie::getProgramId, movieManageDto.getProgramId())
                .like(StringUtil.isNotEmpty(movieManageDto.getMovieName()), Movie::getMovieName, movieManageDto.getMovieName())
                .eq(Objects.nonNull(movieManageDto.getStatus()), Movie::getStatus, movieManageDto.getStatus())
                .orderByDesc(Movie::getEditTime));
        if (CollectionUtil.isEmpty(moviePage.getRecords())) {
            return movieManageVoPage;
        }
        List<Long> programIdList = moviePage.getRecords().stream().map(Movie::getProgramId)
                .filter(Objects::nonNull).distinct().toList();
        List<MovieScreening> scopedScreeningList = selectMovieScreeningList(programIdList,
                movieManageDto.getAreaId(), movieManageDto.getAreaIds());
        Map<Long, MovieSalesAggregate> salesMap = selectMovieSalesMap(programIdList, scopedScreeningList,
                hasAreaFilter(movieManageDto.getAreaId(), movieManageDto.getAreaIds()));
        Map<Long, MovieScreeningAggregate> screeningAggregateMap =
                selectMovieScreeningAggregateMap(scopedScreeningList);
        List<MovieManageVo> movieManageVoList = moviePage.getRecords().stream().map(movie -> {
            MovieManageVo movieManageVo = new MovieManageVo();
            BeanUtil.copyProperties(movie, movieManageVo);
            MovieSalesAggregate salesAggregate = salesMap.get(movie.getProgramId());
            if (Objects.nonNull(salesAggregate)) {
                movieManageVo.setPaidOrderCount(salesAggregate.paidOrderCount());
                movieManageVo.setPaidTicketCount(salesAggregate.paidTicketCount());
            } else {
                movieManageVo.setPaidOrderCount(0L);
                movieManageVo.setPaidTicketCount(0L);
            }
            MovieScreeningAggregate screeningAggregate = screeningAggregateMap.get(movie.getProgramId());
            if (Objects.nonNull(screeningAggregate)) {
                movieManageVo.setScreeningCount(screeningAggregate.screeningCount());
                movieManageVo.setSoldOutScreeningCount(screeningAggregate.soldOutScreeningCount());
            } else {
                movieManageVo.setScreeningCount(0L);
                movieManageVo.setSoldOutScreeningCount(0L);
            }
            return movieManageVo;
        }).toList();
        BeanUtils.copyProperties(moviePage, movieManageVoPage);
        movieManageVoPage.setRecords(movieManageVoList);
        return movieManageVoPage;
    }

    private Set<Long> selectMovieIdSetByArea(Long areaId, List<Long> areaIds) {
        if (!hasAreaFilter(areaId, areaIds)) {
            return new HashSet<>();
        }
        Set<Long> cinemaIdSet = selectCinemaIdSetByArea(areaId, areaIds);
        if (CollectionUtil.isEmpty(cinemaIdSet)) {
            return new HashSet<>();
        }
        List<MovieScreening> movieScreeningList = movieScreeningMapper.selectList(Wrappers.lambdaQuery(MovieScreening.class)
                .in(MovieScreening::getCinemaId, cinemaIdSet)
                .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode()));
        if (CollectionUtil.isEmpty(movieScreeningList)) {
            return new HashSet<>();
        }
        return movieScreeningList.stream().map(MovieScreening::getMovieId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean hasAreaFilter(Long areaId, List<Long> areaIds) {
        return Objects.nonNull(areaId) || CollectionUtil.isNotEmpty(areaIds);
    }

    private Map<Long, MovieSalesAggregate> selectMovieSalesMap(List<Long> programIdList,
                                                               List<MovieScreening> scopedScreeningList,
                                                               boolean areaFiltered) {
        if (CollectionUtil.isEmpty(programIdList)) {
            return new HashMap<>();
        }
        MovieOrderSalesCountDto movieOrderSalesCountDto = new MovieOrderSalesCountDto();
        if (areaFiltered) {
            List<Long> screeningIdList = scopedScreeningList.stream()
                    .map(MovieScreening::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (CollectionUtil.isEmpty(screeningIdList)) {
                return new HashMap<>();
            }
            movieOrderSalesCountDto.setScreeningIdList(screeningIdList);
        } else {
            movieOrderSalesCountDto.setProgramIdList(programIdList);
        }
        try {
            ApiResponse<List<MovieOrderSalesCountVo>> response = orderClient.movieSalesCount(movieOrderSalesCountDto);
            if (!Objects.equals(response.getCode(), BaseCode.SUCCESS.getCode()) ||
                    CollectionUtil.isEmpty(response.getData())) {
                return new HashMap<>();
            }
            return response.getData().stream()
                    .filter(item -> Objects.nonNull(item.getProgramId()))
                    .collect(Collectors.groupingBy(MovieOrderSalesCountVo::getProgramId,
                            Collectors.collectingAndThen(Collectors.toList(), list -> new MovieSalesAggregate(
                                    list.stream().map(MovieOrderSalesCountVo::getPaidOrderCount)
                                            .filter(Objects::nonNull).mapToLong(Long::longValue).sum(),
                                    list.stream().map(MovieOrderSalesCountVo::getPaidTicketCount)
                                            .filter(Objects::nonNull).mapToLong(Long::longValue).sum()
                            ))));
        } catch (Exception exception) {
            log.warn("order-service movie sales count rpc error programIdList:{}", programIdList, exception);
            return new HashMap<>();
        }
    }

    private List<MovieScreening> selectMovieScreeningList(List<Long> programIdList, Long areaId, List<Long> areaIds) {
        if (CollectionUtil.isEmpty(programIdList)) {
            return List.of();
        }
        Set<Long> cinemaIdSet = selectCinemaIdSetByArea(areaId, areaIds);
        if (hasAreaFilter(areaId, areaIds) && CollectionUtil.isEmpty(cinemaIdSet)) {
            return List.of();
        }
        return movieScreeningMapper.selectList(Wrappers.lambdaQuery(MovieScreening.class)
                .in(MovieScreening::getProgramId, programIdList)
                .in(CollectionUtil.isNotEmpty(cinemaIdSet), MovieScreening::getCinemaId, cinemaIdSet)
                .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode()));
    }

    private Map<Long, MovieScreeningAggregate> selectMovieScreeningAggregateMap(List<MovieScreening> movieScreeningList) {
        if (CollectionUtil.isEmpty(movieScreeningList)) {
            return new HashMap<>();
        }
        Map<Long, Long> remainNumberMap = selectScreeningRemainNumberMap(movieScreeningList.stream()
                .map(MovieScreening::getId).filter(Objects::nonNull).toList());
        return movieScreeningList.stream().collect(Collectors.groupingBy(MovieScreening::getProgramId,
                Collectors.collectingAndThen(Collectors.toList(), list -> {
                    long soldOutCount = list.stream()
                            .filter(screening -> Optional.ofNullable(remainNumberMap.get(screening.getId())).orElse(0L) <= 0L)
                            .count();
                    return new MovieScreeningAggregate((long) list.size(), soldOutCount);
                })));
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

    @Transactional(rollbackFor = Exception.class)
    public Long movieSave(MovieSaveDto movieSaveDto) {
        validateProgramExists(movieSaveDto.getProgramId());
        Movie movie = new Movie();
        BeanUtil.copyProperties(movieSaveDto, movie);
        movie.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(movie.getId())) {
            movie.setId(uidGenerator.getUid());
            movieMapper.insert(movie);
            return movie.getId();
        }
        movie.setEditTime(DateUtils.now());
        int updateCount = movieMapper.update(movie, Wrappers.lambdaUpdate(Movie.class)
                .eq(Movie::getId, movie.getId())
                .eq(Movie::getProgramId, movie.getProgramId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return movie.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean movieStatusUpdate(MovieStatusUpdateDto movieStatusUpdateDto) {
        validateBusinessStatus(movieStatusUpdateDto.getStatus());
        Movie movie = new Movie();
        movie.setStatus(movieStatusUpdateDto.getStatus());
        movie.setEditTime(DateUtils.now());
        int updateCount = movieMapper.update(movie, Wrappers.lambdaUpdate(Movie.class)
                .eq(Movie::getId, movieStatusUpdateDto.getMovieId())
                .eq(Movie::getProgramId, movieStatusUpdateDto.getProgramId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return true;
    }

    public IPage<CinemaManageVo> cinemaPage(CinemaManageDto cinemaManageDto) {
        IPage<CinemaManageVo> cinemaManageVoPage =
                new Page<>(cinemaManageDto.getPageNumber(), cinemaManageDto.getPageSize());
        IPage<Cinema> cinemaPage = cinemaMapper.selectPage(PageUtil.getPageParams(cinemaManageDto.getPageNumber(),
                cinemaManageDto.getPageSize()), Wrappers.lambdaQuery(Cinema.class)
                .eq(Objects.nonNull(cinemaManageDto.getCinemaId()), Cinema::getId, cinemaManageDto.getCinemaId())
                .eq(Objects.nonNull(cinemaManageDto.getAreaId()), Cinema::getAreaId, cinemaManageDto.getAreaId())
                .like(StringUtil.isNotEmpty(cinemaManageDto.getCityName()), Cinema::getCityName, cinemaManageDto.getCityName())
                .like(StringUtil.isNotEmpty(cinemaManageDto.getDistrictName()), Cinema::getDistrictName, cinemaManageDto.getDistrictName())
                .like(StringUtil.isNotEmpty(cinemaManageDto.getBusinessArea()), Cinema::getBusinessArea, cinemaManageDto.getBusinessArea())
                .like(StringUtil.isNotEmpty(cinemaManageDto.getBrandName()), Cinema::getBrandName, cinemaManageDto.getBrandName())
                .like(StringUtil.isNotEmpty(cinemaManageDto.getCinemaName()), Cinema::getCinemaName, cinemaManageDto.getCinemaName())
                .eq(Objects.nonNull(cinemaManageDto.getStatus()), Cinema::getStatus, cinemaManageDto.getStatus())
                .orderByDesc(Cinema::getEditTime));
        if (CollectionUtil.isEmpty(cinemaPage.getRecords())) {
            return cinemaManageVoPage;
        }
        List<CinemaManageVo> cinemaManageVoList = cinemaPage.getRecords().stream().map(cinema -> {
            CinemaManageVo cinemaManageVo = new CinemaManageVo();
            BeanUtil.copyProperties(cinema, cinemaManageVo);
            return cinemaManageVo;
        }).toList();
        BeanUtils.copyProperties(cinemaPage, cinemaManageVoPage);
        cinemaManageVoPage.setRecords(cinemaManageVoList);
        return cinemaManageVoPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long cinemaSave(CinemaSaveDto cinemaSaveDto) {
        Cinema cinema = new Cinema();
        BeanUtil.copyProperties(cinemaSaveDto, cinema);
        cinema.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(cinema.getId())) {
            cinema.setId(uidGenerator.getUid());
            cinemaMapper.insert(cinema);
            return cinema.getId();
        }
        cinema.setEditTime(DateUtils.now());
        int updateCount = cinemaMapper.update(cinema, Wrappers.lambdaUpdate(Cinema.class)
                .eq(Cinema::getId, cinema.getId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return cinema.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean cinemaStatusUpdate(CinemaStatusUpdateDto cinemaStatusUpdateDto) {
        validateBusinessStatus(cinemaStatusUpdateDto.getStatus());
        Cinema cinema = new Cinema();
        cinema.setStatus(cinemaStatusUpdateDto.getStatus());
        cinema.setEditTime(DateUtils.now());
        int updateCount = cinemaMapper.update(cinema, Wrappers.lambdaUpdate(Cinema.class)
                .eq(Cinema::getId, cinemaStatusUpdateDto.getCinemaId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return true;
    }

    public IPage<HallManageVo> hallPage(HallManageDto hallManageDto) {
        IPage<HallManageVo> hallManageVoPage =
                new Page<>(hallManageDto.getPageNumber(), hallManageDto.getPageSize());
        IPage<Hall> hallPage = hallMapper.selectPage(PageUtil.getPageParams(hallManageDto.getPageNumber(),
                hallManageDto.getPageSize()), Wrappers.lambdaQuery(Hall.class)
                .eq(Objects.nonNull(hallManageDto.getHallId()), Hall::getId, hallManageDto.getHallId())
                .eq(Objects.nonNull(hallManageDto.getCinemaId()), Hall::getCinemaId, hallManageDto.getCinemaId())
                .like(StringUtil.isNotEmpty(hallManageDto.getHallName()), Hall::getHallName, hallManageDto.getHallName())
                .eq(Objects.nonNull(hallManageDto.getStatus()), Hall::getStatus, hallManageDto.getStatus())
                .orderByDesc(Hall::getEditTime));
        if (CollectionUtil.isEmpty(hallPage.getRecords())) {
            return hallManageVoPage;
        }
        Map<Long, Cinema> cinemaMap = selectCinemaMapByIds(hallPage.getRecords().stream()
                .map(Hall::getCinemaId).filter(Objects::nonNull).distinct().toList());
        List<HallManageVo> hallManageVoList = hallPage.getRecords().stream().map(hall -> {
            HallManageVo hallManageVo = new HallManageVo();
            BeanUtil.copyProperties(hall, hallManageVo);
            Cinema cinema = cinemaMap.get(hall.getCinemaId());
            if (Objects.nonNull(cinema)) {
                hallManageVo.setCinemaName(cinema.getCinemaName());
            }
            return hallManageVo;
        }).toList();
        BeanUtils.copyProperties(hallPage, hallManageVoPage);
        hallManageVoPage.setRecords(hallManageVoList);
        return hallManageVoPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long hallSave(HallSaveDto hallSaveDto) {
        validateCinemaExists(hallSaveDto.getCinemaId());
        Hall hall = new Hall();
        BeanUtil.copyProperties(hallSaveDto, hall);
        hall.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(hall.getId())) {
            hall.setId(uidGenerator.getUid());
            hallMapper.insert(hall);
            return hall.getId();
        }
        hall.setEditTime(DateUtils.now());
        int updateCount = hallMapper.update(hall, Wrappers.lambdaUpdate(Hall.class)
                .eq(Hall::getId, hall.getId())
                .eq(Hall::getCinemaId, hall.getCinemaId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return hall.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean hallStatusUpdate(HallStatusUpdateDto hallStatusUpdateDto) {
        validateBusinessStatus(hallStatusUpdateDto.getStatus());
        Hall hall = new Hall();
        hall.setStatus(hallStatusUpdateDto.getStatus());
        hall.setEditTime(DateUtils.now());
        int updateCount = hallMapper.update(hall, Wrappers.lambdaUpdate(Hall.class)
                .eq(Hall::getId, hallStatusUpdateDto.getHallId())
                .eq(Hall::getCinemaId, hallStatusUpdateDto.getCinemaId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return true;
    }

    public IPage<HallSeatManageVo> hallSeatPage(HallSeatPageManageDto hallSeatPageManageDto) {
        IPage<HallSeatManageVo> hallSeatManageVoPage =
                new Page<>(hallSeatPageManageDto.getPageNumber(), hallSeatPageManageDto.getPageSize());
        IPage<HallSeat> hallSeatPage = hallSeatMapper.selectPage(PageUtil.getPageParams(hallSeatPageManageDto.getPageNumber(),
                hallSeatPageManageDto.getPageSize()), Wrappers.lambdaQuery(HallSeat.class)
                .eq(Objects.nonNull(hallSeatPageManageDto.getCinemaId()), HallSeat::getCinemaId, hallSeatPageManageDto.getCinemaId())
                .eq(Objects.nonNull(hallSeatPageManageDto.getHallId()), HallSeat::getHallId, hallSeatPageManageDto.getHallId())
                .eq(Objects.nonNull(hallSeatPageManageDto.getRowCode()), HallSeat::getRowCode, hallSeatPageManageDto.getRowCode())
                .eq(Objects.nonNull(hallSeatPageManageDto.getColCode()), HallSeat::getColCode, hallSeatPageManageDto.getColCode())
                .eq(Objects.nonNull(hallSeatPageManageDto.getSeatStatus()), HallSeat::getSeatStatus, hallSeatPageManageDto.getSeatStatus())
                .eq(HallSeat::getStatus, BusinessStatus.YES.getCode())
                .orderByAsc(HallSeat::getRowCode)
                .orderByAsc(HallSeat::getColCode));
        if (CollectionUtil.isEmpty(hallSeatPage.getRecords())) {
            return hallSeatManageVoPage;
        }
        Map<Long, Cinema> cinemaMap = selectCinemaMapByIds(hallSeatPage.getRecords().stream()
                .map(HallSeat::getCinemaId).filter(Objects::nonNull).distinct().toList());
        Map<Long, Hall> hallMap = selectHallMapByIds(hallSeatPage.getRecords().stream()
                .map(HallSeat::getHallId).filter(Objects::nonNull).distinct().toList());
        List<HallSeatManageVo> hallSeatManageVoList = hallSeatPage.getRecords().stream().map(hallSeat -> {
            HallSeatManageVo hallSeatManageVo = new HallSeatManageVo();
            BeanUtil.copyProperties(hallSeat, hallSeatManageVo);
            Cinema cinema = cinemaMap.get(hallSeat.getCinemaId());
            if (Objects.nonNull(cinema)) {
                hallSeatManageVo.setCinemaName(cinema.getCinemaName());
            }
            Hall hall = hallMap.get(hallSeat.getHallId());
            if (Objects.nonNull(hall)) {
                hallSeatManageVo.setHallName(hall.getHallName());
            }
            hallSeatManageVo.setSeatTypeName(SeatType.getMsg(hallSeat.getSeatType()));
            hallSeatManageVo.setSeatStatusName(BusinessStatus.getMsg(hallSeat.getSeatStatus()));
            return hallSeatManageVo;
        }).toList();
        BeanUtils.copyProperties(hallSeatPage, hallSeatManageVoPage);
        hallSeatManageVoPage.setRecords(hallSeatManageVoList);
        return hallSeatManageVoPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long hallSeatSave(HallSeatSaveDto hallSeatSaveDto) {
        validateHallExists(hallSeatSaveDto.getCinemaId(), hallSeatSaveDto.getHallId());
        validateSeatPosition(hallSeatSaveDto.getRowCode(), hallSeatSaveDto.getColCode());
        HallSeat hallSeat = new HallSeat();
        BeanUtil.copyProperties(hallSeatSaveDto, hallSeat);
        fillHallSeatDefault(hallSeat);
        hallSeat.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(hallSeat.getId())) {
            HallSeat existsHallSeat = hallSeatMapper.selectOne(Wrappers.lambdaQuery(HallSeat.class)
                    .eq(HallSeat::getHallId, hallSeat.getHallId())
                    .eq(HallSeat::getRowCode, hallSeat.getRowCode())
                    .eq(HallSeat::getColCode, hallSeat.getColCode()));
            if (Objects.nonNull(existsHallSeat)) {
                hallSeat.setId(existsHallSeat.getId());
                hallSeat.setEditTime(DateUtils.now());
                hallSeatMapper.update(hallSeat, Wrappers.lambdaUpdate(HallSeat.class)
                        .eq(HallSeat::getId, existsHallSeat.getId())
                        .eq(HallSeat::getHallId, hallSeat.getHallId()));
                refreshHallSeatCount(hallSeat.getCinemaId(), hallSeat.getHallId());
                return hallSeat.getId();
            }
            hallSeat.setId(uidGenerator.getUid());
            hallSeatMapper.insert(hallSeat);
            refreshHallSeatCount(hallSeat.getCinemaId(), hallSeat.getHallId());
            return hallSeat.getId();
        }
        hallSeat.setEditTime(DateUtils.now());
        int updateCount = hallSeatMapper.update(hallSeat, Wrappers.lambdaUpdate(HallSeat.class)
                .eq(HallSeat::getId, hallSeat.getId())
                .eq(HallSeat::getCinemaId, hallSeat.getCinemaId())
                .eq(HallSeat::getHallId, hallSeat.getHallId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        refreshHallSeatCount(hallSeat.getCinemaId(), hallSeat.getHallId());
        return hallSeat.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer hallSeatBatchGenerate(HallSeatBatchGenerateDto hallSeatBatchGenerateDto) {
        validateHallExists(hallSeatBatchGenerateDto.getCinemaId(), hallSeatBatchGenerateDto.getHallId());
        if (Objects.isNull(hallSeatBatchGenerateDto.getRowCount()) || hallSeatBatchGenerateDto.getRowCount() <= 0 ||
                Objects.isNull(hallSeatBatchGenerateDto.getColCount()) || hallSeatBatchGenerateDto.getColCount() <= 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        Set<String> existsSeatPositionSet = hallSeatMapper.selectList(Wrappers.lambdaQuery(HallSeat.class)
                        .eq(HallSeat::getHallId, hallSeatBatchGenerateDto.getHallId()))
                .stream()
                .map(hallSeat -> seatPositionKey(hallSeat.getRowCode(), hallSeat.getColCode()))
                .collect(Collectors.toSet());
        int insertCount = 0;
        for (int rowCode = 1; rowCode <= hallSeatBatchGenerateDto.getRowCount(); rowCode++) {
            for (int colCode = 1; colCode <= hallSeatBatchGenerateDto.getColCount(); colCode++) {
                if (existsSeatPositionSet.contains(seatPositionKey(rowCode, colCode))) {
                    continue;
                }
                HallSeat hallSeat = new HallSeat();
                hallSeat.setId(uidGenerator.getUid());
                hallSeat.setCinemaId(hallSeatBatchGenerateDto.getCinemaId());
                hallSeat.setHallId(hallSeatBatchGenerateDto.getHallId());
                hallSeat.setRowCode(rowCode);
                hallSeat.setColCode(colCode);
                hallSeat.setSeatNo(rowCode + "排" + colCode + "座");
                hallSeat.setZoneName(hallSeatBatchGenerateDto.getZoneName());
                hallSeat.setPriceLevel(hallSeatBatchGenerateDto.getPriceLevel());
                hallSeat.setSeatType(hallSeatBatchGenerateDto.getSeatType());
                fillHallSeatDefault(hallSeat);
                hallSeat.setXCoordinate(colCode);
                hallSeat.setYCoordinate(rowCode);
                hallSeat.setStatus(BusinessStatus.YES.getCode());
                hallSeatMapper.insert(hallSeat);
                insertCount++;
            }
        }
        Hall hall = new Hall();
        hall.setRowCount(hallSeatBatchGenerateDto.getRowCount());
        hall.setColCount(hallSeatBatchGenerateDto.getColCount());
        hall.setEditTime(DateUtils.now());
        hallMapper.update(hall, Wrappers.lambdaUpdate(Hall.class)
                .eq(Hall::getId, hallSeatBatchGenerateDto.getHallId())
                .eq(Hall::getCinemaId, hallSeatBatchGenerateDto.getCinemaId()));
        refreshHallSeatCount(hallSeatBatchGenerateDto.getCinemaId(), hallSeatBatchGenerateDto.getHallId());
        return insertCount;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean hallSeatStatusUpdate(HallSeatStatusUpdateDto hallSeatStatusUpdateDto) {
        validateBusinessStatus(hallSeatStatusUpdateDto.getSeatStatus());
        HallSeat hallSeat = new HallSeat();
        hallSeat.setSeatStatus(hallSeatStatusUpdateDto.getSeatStatus());
        hallSeat.setEditTime(DateUtils.now());
        int updateCount = hallSeatMapper.update(hallSeat, Wrappers.lambdaUpdate(HallSeat.class)
                .eq(HallSeat::getCinemaId, hallSeatStatusUpdateDto.getCinemaId())
                .eq(HallSeat::getHallId, hallSeatStatusUpdateDto.getHallId())
                .in(HallSeat::getId, hallSeatStatusUpdateDto.getSeatIdList()));
        if (updateCount != hallSeatStatusUpdateDto.getSeatIdList().size()) {
            throw new TidesFrameException(BaseCode.SEAT_UPDATE_REL_COUNT_NOT_EQUAL_PRESET_COUNT);
        }
        refreshHallSeatCount(hallSeatStatusUpdateDto.getCinemaId(), hallSeatStatusUpdateDto.getHallId());
        return true;
    }

    public IPage<VenueManageVo> venuePage(VenueManageDto venueManageDto) {
        IPage<VenueManageVo> venueManageVoPage =
                new Page<>(venueManageDto.getPageNumber(), venueManageDto.getPageSize());
        IPage<Venue> venuePage = venueMapper.selectPage(PageUtil.getPageParams(venueManageDto.getPageNumber(),
                venueManageDto.getPageSize()), Wrappers.lambdaQuery(Venue.class)
                .eq(Objects.nonNull(venueManageDto.getVenueId()), Venue::getId, venueManageDto.getVenueId())
                .eq(Objects.nonNull(venueManageDto.getAreaId()), Venue::getAreaId, venueManageDto.getAreaId())
                .eq(Objects.nonNull(venueManageDto.getVenueType()), Venue::getVenueType, venueManageDto.getVenueType())
                .like(StringUtil.isNotEmpty(venueManageDto.getVenueName()), Venue::getVenueName, venueManageDto.getVenueName())
                .eq(Objects.nonNull(venueManageDto.getStatus()), Venue::getStatus, venueManageDto.getStatus())
                .orderByDesc(Venue::getEditTime));
        if (CollectionUtil.isEmpty(venuePage.getRecords())) {
            return venueManageVoPage;
        }
        List<VenueManageVo> venueManageVoList = venuePage.getRecords().stream().map(venue -> {
            VenueManageVo venueManageVo = new VenueManageVo();
            BeanUtil.copyProperties(venue, venueManageVo);
            return venueManageVo;
        }).toList();
        BeanUtils.copyProperties(venuePage, venueManageVoPage);
        venueManageVoPage.setRecords(venueManageVoList);
        return venueManageVoPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long venueSave(VenueSaveDto venueSaveDto) {
        Venue venue = new Venue();
        BeanUtil.copyProperties(venueSaveDto, venue);
        venue.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(venue.getId())) {
            venue.setId(uidGenerator.getUid());
            venueMapper.insert(venue);
            return venue.getId();
        }
        venue.setEditTime(DateUtils.now());
        int updateCount = venueMapper.update(venue, Wrappers.lambdaUpdate(Venue.class)
                .eq(Venue::getId, venue.getId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return venue.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean venueStatusUpdate(VenueStatusUpdateDto venueStatusUpdateDto) {
        validateBusinessStatus(venueStatusUpdateDto.getStatus());
        Venue venue = new Venue();
        venue.setStatus(venueStatusUpdateDto.getStatus());
        venue.setEditTime(DateUtils.now());
        int updateCount = venueMapper.update(venue, Wrappers.lambdaUpdate(Venue.class)
                .eq(Venue::getId, venueStatusUpdateDto.getVenueId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return true;
    }

    public IPage<ArtistManageVo> artistPage(ArtistManageDto artistManageDto) {
        IPage<ArtistManageVo> artistManageVoPage =
                new Page<>(artistManageDto.getPageNumber(), artistManageDto.getPageSize());
        IPage<Artist> artistPage = artistMapper.selectPage(PageUtil.getPageParams(artistManageDto.getPageNumber(),
                artistManageDto.getPageSize()), Wrappers.lambdaQuery(Artist.class)
                .eq(Objects.nonNull(artistManageDto.getArtistId()), Artist::getId, artistManageDto.getArtistId())
                .eq(Objects.nonNull(artistManageDto.getArtistType()), Artist::getArtistType, artistManageDto.getArtistType())
                .like(StringUtil.isNotEmpty(artistManageDto.getArtistName()), Artist::getArtistName, artistManageDto.getArtistName())
                .eq(Objects.nonNull(artistManageDto.getStatus()), Artist::getStatus, artistManageDto.getStatus())
                .orderByDesc(Artist::getEditTime));
        if (CollectionUtil.isEmpty(artistPage.getRecords())) {
            return artistManageVoPage;
        }
        List<ArtistManageVo> artistManageVoList = artistPage.getRecords().stream().map(artist -> {
            ArtistManageVo artistManageVo = new ArtistManageVo();
            BeanUtil.copyProperties(artist, artistManageVo);
            return artistManageVo;
        }).toList();
        BeanUtils.copyProperties(artistPage, artistManageVoPage);
        artistManageVoPage.setRecords(artistManageVoList);
        return artistManageVoPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long artistSave(ArtistSaveDto artistSaveDto) {
        Artist artist = new Artist();
        BeanUtil.copyProperties(artistSaveDto, artist);
        artist.setArtistType(Optional.ofNullable(artist.getArtistType()).orElse(1));
        artist.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(artist.getId())) {
            artist.setId(uidGenerator.getUid());
            artistMapper.insert(artist);
            return artist.getId();
        }
        artist.setEditTime(DateUtils.now());
        int updateCount = artistMapper.update(artist, Wrappers.lambdaUpdate(Artist.class)
                .eq(Artist::getId, artist.getId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return artist.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean artistStatusUpdate(ArtistStatusUpdateDto artistStatusUpdateDto) {
        validateBusinessStatus(artistStatusUpdateDto.getStatus());
        Artist artist = new Artist();
        artist.setStatus(artistStatusUpdateDto.getStatus());
        artist.setEditTime(DateUtils.now());
        int updateCount = artistMapper.update(artist, Wrappers.lambdaUpdate(Artist.class)
                .eq(Artist::getId, artistStatusUpdateDto.getArtistId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return true;
    }

    public IPage<ProgramArtistManageVo> programArtistPage(ProgramArtistManageDto programArtistManageDto) {
        IPage<ProgramArtistManageVo> programArtistManageVoPage =
                new Page<>(programArtistManageDto.getPageNumber(), programArtistManageDto.getPageSize());
        IPage<ProgramArtist> programArtistPage = programArtistMapper.selectPage(PageUtil.getPageParams(
                programArtistManageDto.getPageNumber(), programArtistManageDto.getPageSize()), Wrappers.lambdaQuery(ProgramArtist.class)
                .eq(Objects.nonNull(programArtistManageDto.getRelationId()), ProgramArtist::getId, programArtistManageDto.getRelationId())
                .eq(Objects.nonNull(programArtistManageDto.getProgramId()), ProgramArtist::getProgramId, programArtistManageDto.getProgramId())
                .eq(Objects.nonNull(programArtistManageDto.getArtistId()), ProgramArtist::getArtistId, programArtistManageDto.getArtistId())
                .eq(StringUtil.isNotEmpty(programArtistManageDto.getRoleType()), ProgramArtist::getRoleType, programArtistManageDto.getRoleType())
                .eq(Objects.nonNull(programArtistManageDto.getStatus()), ProgramArtist::getStatus, programArtistManageDto.getStatus())
                .orderByAsc(ProgramArtist::getSortOrder)
                .orderByDesc(ProgramArtist::getEditTime));
        if (CollectionUtil.isEmpty(programArtistPage.getRecords())) {
            return programArtistManageVoPage;
        }
        Map<Long, Artist> artistMap = selectArtistMapByIds(programArtistPage.getRecords().stream()
                .map(ProgramArtist::getArtistId).filter(Objects::nonNull).distinct().toList());
        List<ProgramArtistManageVo> programArtistManageVoList = programArtistPage.getRecords().stream()
                .map(programArtist -> {
                    ProgramArtistManageVo programArtistManageVo = new ProgramArtistManageVo();
                    BeanUtil.copyProperties(programArtist, programArtistManageVo);
                    Artist artist = artistMap.get(programArtist.getArtistId());
                    if (Objects.nonNull(artist)) {
                        programArtistManageVo.setArtistName(artist.getArtistName());
                        programArtistManageVo.setAvatar(artist.getAvatar());
                    }
                    return programArtistManageVo;
                }).toList();
        BeanUtils.copyProperties(programArtistPage, programArtistManageVoPage);
        programArtistManageVoPage.setRecords(programArtistManageVoList);
        return programArtistManageVoPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long programArtistSave(ProgramArtistSaveDto programArtistSaveDto) {
        validateProgramExists(programArtistSaveDto.getProgramId());
        validateArtistExists(programArtistSaveDto.getArtistId());
        ProgramArtist programArtist = new ProgramArtist();
        BeanUtil.copyProperties(programArtistSaveDto, programArtist);
        programArtist.setSortOrder(Optional.ofNullable(programArtist.getSortOrder()).orElse(0));
        programArtist.setDisplayFlag(Optional.ofNullable(programArtist.getDisplayFlag()).orElse(BusinessStatus.YES.getCode()));
        programArtist.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(programArtist.getId())) {
            programArtist.setId(uidGenerator.getUid());
            programArtistMapper.insert(programArtist);
            return programArtist.getId();
        }
        programArtist.setEditTime(DateUtils.now());
        int updateCount = programArtistMapper.update(programArtist, Wrappers.lambdaUpdate(ProgramArtist.class)
                .eq(ProgramArtist::getId, programArtist.getId())
                .eq(ProgramArtist::getProgramId, programArtist.getProgramId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return programArtist.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean programArtistStatusUpdate(ProgramArtistStatusUpdateDto programArtistStatusUpdateDto) {
        validateBusinessStatus(programArtistStatusUpdateDto.getStatus());
        ProgramArtist programArtist = new ProgramArtist();
        programArtist.setStatus(programArtistStatusUpdateDto.getStatus());
        programArtist.setEditTime(DateUtils.now());
        int updateCount = programArtistMapper.update(programArtist, Wrappers.lambdaUpdate(ProgramArtist.class)
                .eq(ProgramArtist::getId, programArtistStatusUpdateDto.getRelationId())
                .eq(ProgramArtist::getProgramId, programArtistStatusUpdateDto.getProgramId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return true;
    }

    public IPage<MovieProfileManageVo> movieProfilePage(MovieProfileManageDto movieProfileManageDto) {
        IPage<MovieProfileManageVo> movieProfileManageVoPage =
                new Page<>(movieProfileManageDto.getPageNumber(), movieProfileManageDto.getPageSize());
        IPage<MovieProfile> movieProfilePage = movieProfileMapper.selectPage(PageUtil.getPageParams(
                movieProfileManageDto.getPageNumber(), movieProfileManageDto.getPageSize()), Wrappers.lambdaQuery(MovieProfile.class)
                .eq(Objects.nonNull(movieProfileManageDto.getProfileId()), MovieProfile::getId, movieProfileManageDto.getProfileId())
                .eq(Objects.nonNull(movieProfileManageDto.getMovieId()), MovieProfile::getMovieId, movieProfileManageDto.getMovieId())
                .eq(Objects.nonNull(movieProfileManageDto.getProgramId()), MovieProfile::getProgramId, movieProfileManageDto.getProgramId())
                .eq(Objects.nonNull(movieProfileManageDto.getReleaseStatus()), MovieProfile::getReleaseStatus, movieProfileManageDto.getReleaseStatus())
                .eq(Objects.nonNull(movieProfileManageDto.getStatus()), MovieProfile::getStatus, movieProfileManageDto.getStatus())
                .orderByDesc(MovieProfile::getEditTime));
        if (CollectionUtil.isEmpty(movieProfilePage.getRecords())) {
            return movieProfileManageVoPage;
        }
        List<MovieProfileManageVo> movieProfileManageVoList = movieProfilePage.getRecords().stream().map(movieProfile -> {
            MovieProfileManageVo movieProfileManageVo = new MovieProfileManageVo();
            BeanUtil.copyProperties(movieProfile, movieProfileManageVo);
            return movieProfileManageVo;
        }).toList();
        BeanUtils.copyProperties(movieProfilePage, movieProfileManageVoPage);
        movieProfileManageVoPage.setRecords(movieProfileManageVoList);
        return movieProfileManageVoPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long movieProfileSave(MovieProfileSaveDto movieProfileSaveDto) {
        validateMovieExists(movieProfileSaveDto.getMovieId(), movieProfileSaveDto.getProgramId());
        MovieProfile movieProfile = new MovieProfile();
        BeanUtil.copyProperties(movieProfileSaveDto, movieProfile);
        movieProfile.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(movieProfile.getId())) {
            MovieProfile dbMovieProfile = movieProfileMapper.selectOne(Wrappers.lambdaQuery(MovieProfile.class)
                    .eq(MovieProfile::getMovieId, movieProfile.getMovieId())
                    .eq(MovieProfile::getProgramId, movieProfile.getProgramId()));
            if (Objects.isNull(dbMovieProfile)) {
                movieProfile.setId(uidGenerator.getUid());
                movieProfileMapper.insert(movieProfile);
                return movieProfile.getId();
            }
            movieProfile.setId(dbMovieProfile.getId());
        }
        movieProfile.setEditTime(DateUtils.now());
        int updateCount = movieProfileMapper.update(movieProfile, Wrappers.lambdaUpdate(MovieProfile.class)
                .eq(MovieProfile::getId, movieProfile.getId())
                .eq(MovieProfile::getProgramId, movieProfile.getProgramId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return movieProfile.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean movieProfileStatusUpdate(MovieProfileStatusUpdateDto movieProfileStatusUpdateDto) {
        validateBusinessStatus(movieProfileStatusUpdateDto.getStatus());
        MovieProfile movieProfile = new MovieProfile();
        movieProfile.setStatus(movieProfileStatusUpdateDto.getStatus());
        movieProfile.setEditTime(DateUtils.now());
        int updateCount = movieProfileMapper.update(movieProfile, Wrappers.lambdaUpdate(MovieProfile.class)
                .eq(MovieProfile::getId, movieProfileStatusUpdateDto.getProfileId())
                .eq(MovieProfile::getProgramId, movieProfileStatusUpdateDto.getProgramId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return true;
    }

    public IPage<MovieMediaManageVo> movieMediaPage(MovieMediaManageDto movieMediaManageDto) {
        IPage<MovieMediaManageVo> movieMediaManageVoPage =
                new Page<>(movieMediaManageDto.getPageNumber(), movieMediaManageDto.getPageSize());
        IPage<MovieMedia> movieMediaPage = movieMediaMapper.selectPage(PageUtil.getPageParams(
                movieMediaManageDto.getPageNumber(), movieMediaManageDto.getPageSize()), Wrappers.lambdaQuery(MovieMedia.class)
                .eq(Objects.nonNull(movieMediaManageDto.getMediaId()), MovieMedia::getId, movieMediaManageDto.getMediaId())
                .eq(Objects.nonNull(movieMediaManageDto.getMovieId()), MovieMedia::getMovieId, movieMediaManageDto.getMovieId())
                .eq(Objects.nonNull(movieMediaManageDto.getMediaType()), MovieMedia::getMediaType, movieMediaManageDto.getMediaType())
                .eq(Objects.nonNull(movieMediaManageDto.getAuditStatus()), MovieMedia::getAuditStatus, movieMediaManageDto.getAuditStatus())
                .eq(Objects.nonNull(movieMediaManageDto.getStatus()), MovieMedia::getStatus, movieMediaManageDto.getStatus())
                .orderByAsc(MovieMedia::getSortOrder)
                .orderByDesc(MovieMedia::getEditTime));
        if (CollectionUtil.isEmpty(movieMediaPage.getRecords())) {
            return movieMediaManageVoPage;
        }
        List<MovieMediaManageVo> movieMediaManageVoList = movieMediaPage.getRecords().stream().map(movieMedia -> {
            MovieMediaManageVo movieMediaManageVo = new MovieMediaManageVo();
            BeanUtil.copyProperties(movieMedia, movieMediaManageVo);
            return movieMediaManageVo;
        }).toList();
        BeanUtils.copyProperties(movieMediaPage, movieMediaManageVoPage);
        movieMediaManageVoPage.setRecords(movieMediaManageVoList);
        return movieMediaManageVoPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long movieMediaSave(MovieMediaSaveDto movieMediaSaveDto) {
        validateMovieExists(movieMediaSaveDto.getMovieId());
        MovieMedia movieMedia = new MovieMedia();
        BeanUtil.copyProperties(movieMediaSaveDto, movieMedia);
        movieMedia.setSortOrder(Optional.ofNullable(movieMedia.getSortOrder()).orElse(0));
        movieMedia.setAuditStatus(Optional.ofNullable(movieMedia.getAuditStatus()).orElse(BusinessStatus.YES.getCode()));
        movieMedia.setStatus(BusinessStatus.YES.getCode());
        if (Objects.isNull(movieMedia.getId())) {
            movieMedia.setId(uidGenerator.getUid());
            movieMediaMapper.insert(movieMedia);
            return movieMedia.getId();
        }
        movieMedia.setEditTime(DateUtils.now());
        int updateCount = movieMediaMapper.update(movieMedia, Wrappers.lambdaUpdate(MovieMedia.class)
                .eq(MovieMedia::getId, movieMedia.getId())
                .eq(MovieMedia::getMovieId, movieMedia.getMovieId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return movieMedia.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean movieMediaStatusUpdate(MovieMediaStatusUpdateDto movieMediaStatusUpdateDto) {
        validateBusinessStatus(movieMediaStatusUpdateDto.getStatus());
        MovieMedia movieMedia = new MovieMedia();
        movieMedia.setStatus(movieMediaStatusUpdateDto.getStatus());
        movieMedia.setEditTime(DateUtils.now());
        int updateCount = movieMediaMapper.update(movieMedia, Wrappers.lambdaUpdate(MovieMedia.class)
                .eq(MovieMedia::getId, movieMediaStatusUpdateDto.getMediaId())
                .eq(MovieMedia::getMovieId, movieMediaStatusUpdateDto.getMovieId()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return true;
    }

    private void validateBusinessStatus(Integer status) {
        if (Objects.isNull(status) || Objects.isNull(BusinessStatus.getRc(status))) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private void validateProgramExists(Long programId) {
        Program program = programMapper.selectOne(Wrappers.lambdaQuery(Program.class)
                .eq(Program::getId, programId)
                .eq(Program::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(program)) {
            throw new TidesFrameException(BaseCode.PROGRAM_NOT_EXIST);
        }
    }

    private void validateCinemaExists(Long cinemaId) {
        Cinema cinema = cinemaMapper.selectOne(Wrappers.lambdaQuery(Cinema.class)
                .eq(Cinema::getId, cinemaId)
                .eq(Cinema::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(cinema)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private void validateArtistExists(Long artistId) {
        Artist artist = artistMapper.selectOne(Wrappers.lambdaQuery(Artist.class)
                .eq(Artist::getId, artistId)
                .eq(Artist::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(artist)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private void validateMovieExists(Long movieId, Long programId) {
        Movie movie = movieMapper.selectOne(Wrappers.lambdaQuery(Movie.class)
                .eq(Movie::getId, movieId)
                .eq(Movie::getProgramId, programId)
                .eq(Movie::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(movie)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private void validateMovieExists(Long movieId) {
        Movie movie = movieMapper.selectOne(Wrappers.lambdaQuery(Movie.class)
                .eq(Movie::getId, movieId)
                .eq(Movie::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(movie)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private Hall validateHallExists(Long cinemaId, Long hallId) {
        Hall hall = hallMapper.selectOne(Wrappers.lambdaQuery(Hall.class)
                .eq(Hall::getId, hallId)
                .eq(Hall::getCinemaId, cinemaId)
                .eq(Hall::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(hall)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return hall;
    }

    private void validateSeatPosition(Integer rowCode, Integer colCode) {
        if (Objects.isNull(rowCode) || rowCode <= 0 || Objects.isNull(colCode) || colCode <= 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private void fillHallSeatDefault(HallSeat hallSeat) {
        if (Objects.isNull(hallSeat.getSeatType())) {
            hallSeat.setSeatType(SeatType.GENERAL.getCode());
        }
        if (Objects.isNull(SeatType.getRc(hallSeat.getSeatType()))) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        if (Objects.isNull(hallSeat.getSeatStatus())) {
            hallSeat.setSeatStatus(BusinessStatus.YES.getCode());
        }
        validateBusinessStatus(hallSeat.getSeatStatus());
        if (Objects.isNull(hallSeat.getAisleFlag())) {
            hallSeat.setAisleFlag(BusinessStatus.NO.getCode());
        }
        if (Objects.isNull(hallSeat.getCoupleFlag())) {
            hallSeat.setCoupleFlag(BusinessStatus.NO.getCode());
        }
        if (Objects.isNull(hallSeat.getAccessibleFlag())) {
            hallSeat.setAccessibleFlag(BusinessStatus.NO.getCode());
        }
        if (Objects.isNull(hallSeat.getVipFlag())) {
            hallSeat.setVipFlag(BusinessStatus.NO.getCode());
        }
        if (Objects.isNull(hallSeat.getRepairFlag())) {
            hallSeat.setRepairFlag(BusinessStatus.NO.getCode());
        }
        if (Objects.isNull(hallSeat.getSellableFlag())) {
            hallSeat.setSellableFlag(BusinessStatus.YES.getCode());
        }
        validateBusinessStatus(hallSeat.getAisleFlag());
        validateBusinessStatus(hallSeat.getCoupleFlag());
        validateBusinessStatus(hallSeat.getAccessibleFlag());
        validateBusinessStatus(hallSeat.getVipFlag());
        validateBusinessStatus(hallSeat.getRepairFlag());
        validateBusinessStatus(hallSeat.getSellableFlag());
        if (!StringUtil.isNotEmpty(hallSeat.getSeatNo())) {
            hallSeat.setSeatNo(hallSeat.getRowCode() + "排" + hallSeat.getColCode() + "座");
        }
        if (Objects.isNull(hallSeat.getXCoordinate())) {
            hallSeat.setXCoordinate(hallSeat.getColCode());
        }
        if (Objects.isNull(hallSeat.getYCoordinate())) {
            hallSeat.setYCoordinate(hallSeat.getRowCode());
        }
    }

    private void refreshHallSeatCount(Long cinemaId, Long hallId) {
        Long seatCount = hallSeatMapper.selectCount(Wrappers.lambdaQuery(HallSeat.class)
                .eq(HallSeat::getCinemaId, cinemaId)
                .eq(HallSeat::getHallId, hallId)
                .eq(HallSeat::getSeatStatus, BusinessStatus.YES.getCode())
                .eq(HallSeat::getSellableFlag, BusinessStatus.YES.getCode())
                .eq(HallSeat::getAisleFlag, BusinessStatus.NO.getCode())
                .eq(HallSeat::getRepairFlag, BusinessStatus.NO.getCode())
                .eq(HallSeat::getStatus, BusinessStatus.YES.getCode()));
        Hall hall = new Hall();
        hall.setSeatCount(seatCount.intValue());
        hall.setEditTime(DateUtils.now());
        hallMapper.update(hall, Wrappers.lambdaUpdate(Hall.class)
                .eq(Hall::getId, hallId)
                .eq(Hall::getCinemaId, cinemaId));
    }

    private String seatPositionKey(Integer rowCode, Integer colCode) {
        return rowCode + "_" + colCode;
    }

    private Map<Long, Cinema> selectCinemaMapByIds(Collection<Long> cinemaIdList) {
        if (CollectionUtil.isEmpty(cinemaIdList)) {
            return new HashMap<>();
        }
        return cinemaMapper.selectBatchIds(cinemaIdList).stream()
                .collect(Collectors.toMap(Cinema::getId, cinema -> cinema, (v1, v2) -> v2));
    }

    private Map<Long, Artist> selectArtistMapByIds(Collection<Long> artistIdList) {
        if (CollectionUtil.isEmpty(artistIdList)) {
            return new HashMap<>();
        }
        return artistMapper.selectBatchIds(artistIdList).stream()
                .collect(Collectors.toMap(Artist::getId, artist -> artist, (v1, v2) -> v2));
    }

    private Map<Long, Hall> selectHallMapByIds(Collection<Long> hallIdList) {
        if (CollectionUtil.isEmpty(hallIdList)) {
            return new HashMap<>();
        }
        return hallMapper.selectBatchIds(hallIdList).stream()
                .collect(Collectors.toMap(Hall::getId, hall -> hall, (v1, v2) -> v2));
    }

    private record MovieSalesAggregate(Long paidOrderCount, Long paidTicketCount) {
    }

    private record MovieScreeningAggregate(Long screeningCount, Long soldOutScreeningCount) {
    }

}
