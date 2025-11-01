package com.tides.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tides.BusinessThreadPool;
import com.tides.RedisStreamPushHandler;
import com.tides.client.BaseDataClient;
import com.tides.client.OrderClient;
import com.tides.client.UserClient;
import com.tides.common.ApiResponse;
import com.tides.core.RedisKeyManage;
import com.tides.dto.AccountOrderCountDto;
import com.tides.dto.AreaGetDto;
import com.tides.dto.AreaSelectDto;
import com.tides.dto.ProgramAddDto;
import com.tides.dto.ProgramDataPreheatDto;
import com.tides.dto.ProgramGetDto;
import com.tides.dto.ProgramInvalidDto;
import com.tides.dto.ProgramListDto;
import com.tides.dto.ProgramOperateDataDto;
import com.tides.dto.ProgramPageListDto;
import com.tides.dto.ProgramRecommendListDto;
import com.tides.dto.ProgramResetExecuteDto;
import com.tides.dto.ProgramSearchDto;
import com.tides.dto.ReduceRemainNumberDto;
import com.tides.dto.TicketCategoryCountDto;
import com.tides.dto.TicketUserListDto;
import com.tides.entity.Program;
import com.tides.entity.ProgramCategory;
import com.tides.entity.ProgramGroup;
import com.tides.entity.ProgramJoinShowTime;
import com.tides.entity.ProgramShowTime;
import com.tides.entity.Seat;
import com.tides.entity.TicketCategory;
import com.tides.entity.TicketCategoryAggregate;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.enums.CompositeCheckType;
import com.tides.enums.ProgramOrderVersion;
import com.tides.enums.SellStatus;
import com.tides.exception.TidesFrameException;
import com.tides.initialize.impl.composite.CompositeContainer;
import com.tides.mapper.ProgramCategoryMapper;
import com.tides.mapper.ProgramGroupMapper;
import com.tides.mapper.ProgramMapper;
import com.tides.mapper.ProgramShowTimeMapper;
import com.tides.mapper.SeatMapper;
import com.tides.mapper.TicketCategoryMapper;
import com.tides.page.PageUtil;
import com.tides.page.PageVo;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.repeatexecutelimit.annotion.RepeatExecuteLimit;
import com.tides.service.cache.local.LocalCacheProgram;
import com.tides.service.cache.local.LocalCacheProgramCategory;
import com.tides.service.cache.local.LocalCacheProgramGroup;
import com.tides.service.cache.local.LocalCacheProgramShowTime;
import com.tides.service.cache.local.LocalCacheTicketCategory;
import com.tides.service.constant.ProgramTimeType;
import com.tides.service.es.ProgramEs;
import com.tides.service.lua.ProgramDelCacheData;
import com.tides.service.tool.SearchKeywordUtil;
import com.tides.service.tool.TokenExpireManager;
import com.tides.servicelock.LockType;
import com.tides.servicelock.annotion.ServiceLock;
import com.tides.threadlocal.BaseParameterHolder;
import com.tides.util.DateUtils;
import com.tides.util.ServiceLockTool;
import com.tides.util.StringUtil;
import com.tides.vo.AccountOrderCountVo;
import com.tides.vo.AreaVo;
import com.tides.vo.ProgramGroupVo;
import com.tides.vo.ProgramHomeVo;
import com.tides.vo.ProgramListVo;
import com.tides.vo.ProgramSimpleInfoVo;
import com.tides.vo.ProgramVo;
import com.tides.vo.TicketCategoryVo;
import com.tides.vo.TicketUserVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tides.constant.Constant.CODE;
import static com.tides.constant.Constant.USER_ID;
import static com.tides.core.DistributedLockConstants.GET_PROGRAM_LOCK;
import static com.tides.core.DistributedLockConstants.PROGRAM_GROUP_LOCK;
import static com.tides.core.DistributedLockConstants.PROGRAM_LOCK;
import static com.tides.core.RepeatExecuteLimitConstants.PAY_OR_CANCEL_PROGRAM_ORDER;
import static com.tides.core.RepeatExecuteLimitConstants.REDUCE_REMAIN_NUMBER;
import static com.tides.util.DateUtils.FORMAT_DATE;

/**
 * @description: 节目 service
 * @author: 19continue
 **/
@Slf4j
@Service
public class ProgramService extends ServiceImpl<ProgramMapper, Program> {

    private static final int SEARCH_FALLBACK_MAX_SIZE = 500;

    private static final Long MOVIE_PARENT_PROGRAM_CATEGORY_ID = 22L;

    private static final Long NATIONAL_AREA_ID = 1L;

    private static final Long JIAXING_AREA_ID = 385L;

    private static final Long NANHU_AREA_ID = 3248L;
    
    @Autowired
    private UidGenerator uidGenerator;
    
    @Autowired
    private ProgramMapper programMapper;
    
    @Autowired
    private ProgramGroupMapper programGroupMapper;
    
    @Autowired
    private ProgramShowTimeMapper programShowTimeMapper;
    
    @Autowired
    private ProgramCategoryMapper programCategoryMapper; 
    
    @Autowired
    private TicketCategoryMapper ticketCategoryMapper;

    @Autowired
    private MovieService movieService;
    
    @Autowired
    private SeatMapper seatMapper;
    
    @Autowired
    private BaseDataClient baseDataClient;
    
    @Autowired
    private UserClient userClient;
    
    @Autowired
    private OrderClient orderClient;
    
    @Autowired
    private RedisCache redisCache;
    
    @Lazy
    @Autowired
    private ProgramService programService;
    
    @Autowired
    private ProgramShowTimeService programShowTimeService;
    
    @Autowired
    private TicketCategoryService ticketCategoryService;
    
    @Autowired
    private ProgramCategoryService programCategoryService;
    
    @Autowired
    private ProgramEs programEs;
    
    @Autowired
    private ServiceLockTool serviceLockTool;
    
    @Autowired
    private RedisStreamPushHandler redisStreamPushHandler;
    
    @Autowired
    private LocalCacheProgram localCacheProgram;
    
    @Autowired
    private LocalCacheProgramGroup localCacheProgramGroup;
    
    @Autowired
    private LocalCacheProgramCategory localCacheProgramCategory;
    
    @Autowired
    private LocalCacheProgramShowTime localCacheProgramShowTime;
    
    @Autowired
    private LocalCacheTicketCategory localCacheTicketCategory;
    
    @Autowired
    private CompositeContainer compositeContainer;
    
    @Autowired
    private TokenExpireManager tokenExpireManager;
    
    @Autowired
    private ProgramDelCacheData programDelCacheData;
    
    @Autowired
    private ProgramOrderService programOrderService;
    
    @Autowired
    private SeatService seatService;
    
    /**
     * 添加节目
     * @param programAddDto 添加节目数据的入参
     * @return 添加节目后的id
     * */
    public Long add(ProgramAddDto programAddDto){
        Program program = new Program();
        BeanUtil.copyProperties(programAddDto,program);
        program.setId(uidGenerator.getUid());
        programMapper.insert(program);
        return program.getId();
    }
    
    /**
     * 搜索
     * @param programSearchDto 搜索节目数据的入参
     * @return 执行后的结果
     * */
    public PageVo<ProgramListVo> search(ProgramSearchDto programSearchDto) {
        //将入参的参数进行具体的组装
        normalizeProgramPageArea(programSearchDto);
        normalizeSearchContent(programSearchDto);
        setQueryTime(programSearchDto);
        PageVo<ProgramListVo> pageVo = programEs.search(programSearchDto);
        if (CollectionUtil.isNotEmpty(pageVo.getList()) && !SearchKeywordUtil.hasLooseClue(programSearchDto.getContent())) {
            return pageVo;
        }
        PageVo<ProgramListVo> dbPageVo = dbSearchPage(programSearchDto);
        if (CollectionUtil.isNotEmpty(dbPageVo.getList()) && dbPageVo.getTotalSize() > pageVo.getTotalSize()) {
            return dbPageVo;
        }
        PageVo<ProgramListVo> looseDbPageVo = looseDbSearchPage(programSearchDto);
        if (CollectionUtil.isNotEmpty(looseDbPageVo.getList())
                && looseDbPageVo.getTotalSize() > Math.max(pageVo.getTotalSize(), dbPageVo.getTotalSize())) {
            return looseDbPageVo;
        }
        if (CollectionUtil.isNotEmpty(pageVo.getList())) {
            return pageVo;
        }
        return CollectionUtil.isNotEmpty(dbPageVo.getList()) ? dbPageVo : looseDbPageVo;
    }

    private boolean normalizeSearchContent(ProgramSearchDto programSearchDto) {
        if (Objects.isNull(programSearchDto) || StringUtil.isEmpty(programSearchDto.getContent())) {
            return false;
        }
        String content = programSearchDto.getContent().trim();
        if (StringUtil.isEmpty(content)) {
            programSearchDto.setContent(null);
            return false;
        }
        programSearchDto.setContent(content);
        return true;
    }
    
    /**
     * 查询主页信息
     * @param programListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public List<ProgramHomeVo> selectHomeList(ProgramListDto programListDto) {
        normalizeProgramListArea(programListDto);
        
        List<ProgramHomeVo> programHomeVoList = programEs.selectHomeList(programListDto);
        if (CollectionUtil.isEmpty(programHomeVoList)) {
            return dbSelectHomeList(programListDto);
        }
        Set<Long> selectedCategoryIds = programHomeVoList.stream()
                .map(ProgramHomeVo::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Long> missCategoryIds = programListDto.getParentProgramCategoryIds().stream()
                .filter(parentProgramCategoryId -> !selectedCategoryIds.contains(parentProgramCategoryId))
                .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(missCategoryIds)) {
            return programHomeVoList;
        }
        ProgramListDto missProgramListDto = new ProgramListDto();
        BeanUtil.copyProperties(programListDto, missProgramListDto);
        missProgramListDto.setParentProgramCategoryIds(missCategoryIds);
        programHomeVoList.addAll(dbSelectHomeList(missProgramListDto));
        return programHomeVoList;
    }
    
    /**
     * 查询主页信息（数据库查询）
     * @param programPageListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    private List<ProgramHomeVo> dbSelectHomeList(ProgramListDto programPageListDto){
        List<ProgramHomeVo> programHomeVoList = new ArrayList<>();
        Map<Long, String> programCategoryMap = selectProgramCategoryMap(programPageListDto.getParentProgramCategoryIds());
        
        List<Program> programList = programMapper.selectHomeList(programPageListDto);
        if (CollectionUtil.isEmpty(programList)) {
            return programHomeVoList;
        }
        
        List<Long> programIdList = programList.stream().map(Program::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ProgramShowTime> programShowTimeLambdaQueryWrapper = Wrappers.lambdaQuery(ProgramShowTime.class)
                .in(ProgramShowTime::getProgramId, programIdList);
        List<ProgramShowTime> programShowTimeList = programShowTimeMapper.selectList(programShowTimeLambdaQueryWrapper);
        Map<Long, List<ProgramShowTime>> programShowTimeMap = 
                programShowTimeList.stream().collect(Collectors.groupingBy(ProgramShowTime::getProgramId));
        
        Map<Long, TicketCategoryAggregate> ticketCategorieMap = selectTicketCategorieMap(programIdList);
        
        Map<Long, List<Program>> programMap = programList.stream()
                .collect(Collectors.groupingBy(Program::getParentProgramCategoryId));
        
        for (Entry<Long, List<Program>> programEntry : programMap.entrySet()) {
            Long key = programEntry.getKey();
            List<Program> value = programEntry.getValue();
            List<ProgramListVo> programListVoList = new ArrayList<>();
            for (Program program : value) {
                ProgramListVo programListVo = new ProgramListVo();
                BeanUtil.copyProperties(program,programListVo);
                
                programListVo.setShowTime(Optional.ofNullable(programShowTimeMap.get(program.getId()))
                        .filter(list -> !list.isEmpty())
                        .map(list -> list.get(0))
                        .map(ProgramShowTime::getShowTime)
                        .orElse(null));
                programListVo.setShowDayTime(Optional.ofNullable(programShowTimeMap.get(program.getId()))
                        .filter(list -> !list.isEmpty())
                        .map(list -> list.get(0))
                        .map(ProgramShowTime::getShowDayTime)
                        .orElse(null));
                programListVo.setShowWeekTime(Optional.ofNullable(programShowTimeMap.get(program.getId()))
                        .filter(list -> !list.isEmpty())
                        .map(list -> list.get(0))
                        .map(ProgramShowTime::getShowWeekTime)
                        .orElse(null));
                
                programListVo.setMaxPrice(Optional.ofNullable(ticketCategorieMap.get(program.getId()))
                        .map(TicketCategoryAggregate::getMaxPrice).orElse(null));
                programListVo.setMinPrice(Optional.ofNullable(ticketCategorieMap.get(program.getId()))
                        .map(TicketCategoryAggregate::getMinPrice).orElse(null));
                programListVoList.add(programListVo);
            }
            ProgramHomeVo programHomeVo = new ProgramHomeVo();
            programHomeVo.setCategoryName(programCategoryMap.get(key));
            programHomeVo.setCategoryId(key);
            programHomeVo.setProgramListVoList(programListVoList);
            programHomeVoList.add(programHomeVo);
        }
        return programHomeVoList;
    }
    
    /**
     * 组装节目参数
     * @param programPageListDto 节目数据的入参
     * */
    public void setQueryTime(ProgramPageListDto programPageListDto){
        switch (programPageListDto.getTimeType()) {
            case ProgramTimeType.TODAY:
                programPageListDto.setStartDateTime(DateUtils.now(FORMAT_DATE));
                programPageListDto.setEndDateTime(DateUtils.now(FORMAT_DATE));
                break;
            case ProgramTimeType.TOMORROW:
                programPageListDto.setStartDateTime(DateUtils.now(FORMAT_DATE));
                programPageListDto.setEndDateTime(DateUtils.addDay(DateUtils.now(FORMAT_DATE),1));
                break;
            case ProgramTimeType.WEEK:
                programPageListDto.setStartDateTime(DateUtils.now(FORMAT_DATE));
                programPageListDto.setEndDateTime(DateUtils.addWeek(DateUtils.now(FORMAT_DATE),1));
                break;
            case ProgramTimeType.MONTH:
                programPageListDto.setStartDateTime(DateUtils.now(FORMAT_DATE));
                programPageListDto.setEndDateTime(DateUtils.addMonth(DateUtils.now(FORMAT_DATE),1));
                break;
            case ProgramTimeType.CALENDAR:
                if (Objects.isNull(programPageListDto.getStartDateTime())) {
                    throw new TidesFrameException(BaseCode.START_DATE_TIME_NOT_EXIST);
                }
                if (Objects.isNull(programPageListDto.getEndDateTime())) {
                    throw new TidesFrameException(BaseCode.END_DATE_TIME_NOT_EXIST);
                }
                break;
            default:
                programPageListDto.setStartDateTime(null);
                programPageListDto.setEndDateTime(null);
                break;
        }
    }
    
    /**
     * 查询分类列表（数据库查询）
     * @param programPageListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public PageVo<ProgramListVo> selectPage(ProgramPageListDto programPageListDto) {
        normalizeProgramPageArea(programPageListDto);
        setQueryTime(programPageListDto);
        PageVo<ProgramListVo> pageVo = programEs.selectPage(programPageListDto);
        if (Objects.equals(programPageListDto.getParentProgramCategoryId(), MOVIE_PARENT_PROGRAM_CATEGORY_ID)) {
            PageVo<ProgramListVo> dbPageVo = dbSelectPage(programPageListDto);
            if (CollectionUtil.isNotEmpty(dbPageVo.getList()) && dbPageVo.getTotalSize() > pageVo.getTotalSize()) {
                return dbPageVo;
            }
        }
        if (CollectionUtil.isNotEmpty(pageVo.getList())) {
            return pageVo;
        }
        return dbSelectPage(programPageListDto);
    }
    
    /**
     * 推荐列表
     * @param programRecommendListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public List<ProgramListVo> recommendList(ProgramRecommendListDto programRecommendListDto){
        normalizeProgramRecommendArea(programRecommendListDto);
        compositeContainer.execute(CompositeCheckType.PROGRAM_RECOMMEND_CHECK.getValue(),programRecommendListDto);
        List<ProgramListVo> programListVoList = programEs.recommendList(programRecommendListDto);
        if (CollectionUtil.isNotEmpty(programListVoList)) {
            return programListVoList;
        }
        return dbRecommendList(programRecommendListDto);
    }

    private List<ProgramListVo> dbRecommendList(ProgramRecommendListDto programRecommendListDto) {
        LambdaQueryWrapper<Program> programLambdaQueryWrapper =
                Wrappers.lambdaQuery(Program.class)
                        .eq(Program::getStatus, BusinessStatus.YES.getCode())
                        .eq(Program::getProgramStatus, BusinessStatus.YES.getCode());
        if (CollectionUtil.isNotEmpty(programRecommendListDto.getAreaIds())) {
            programLambdaQueryWrapper.in(Program::getAreaId, programRecommendListDto.getAreaIds());
        } else if (Objects.nonNull(programRecommendListDto.getAreaId())) {
            programLambdaQueryWrapper.eq(Program::getAreaId, programRecommendListDto.getAreaId());
        }
        if (Objects.nonNull(programRecommendListDto.getParentProgramCategoryId())) {
            programLambdaQueryWrapper.eq(Program::getParentProgramCategoryId,
                    programRecommendListDto.getParentProgramCategoryId());
        }
        if (Objects.nonNull(programRecommendListDto.getProgramId())) {
            programLambdaQueryWrapper.ne(Program::getId, programRecommendListDto.getProgramId());
        }
        programLambdaQueryWrapper.last("limit 10");
        List<Program> programList = programMapper.selectList(programLambdaQueryWrapper);
        if (CollectionUtil.isEmpty(programList)) {
            return new ArrayList<>();
        }
        List<Long> programIdList = programList.stream().map(Program::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ProgramShowTime> programShowTimeLambdaQueryWrapper =
                Wrappers.lambdaQuery(ProgramShowTime.class)
                        .in(ProgramShowTime::getProgramId, programIdList);
        Map<Long, ProgramShowTime> programShowTimeMap = programShowTimeMapper
                .selectList(programShowTimeLambdaQueryWrapper)
                .stream()
                .collect(Collectors.toMap(ProgramShowTime::getProgramId,
                        programShowTime -> programShowTime, (v1, v2) -> v1));
        Map<Long, TicketCategoryAggregate> ticketCategorieMap = selectTicketCategorieMap(programIdList);
        Set<Long> programCategoryIdList = programList.stream()
                .map(Program::getProgramCategoryId)
                .collect(Collectors.toSet());
        Map<Long, String> programCategoryMap = selectProgramCategoryMap(programCategoryIdList);
        Set<Long> parentProgramCategoryIdList = programList.stream()
                .map(Program::getParentProgramCategoryId)
                .collect(Collectors.toSet());
        Map<Long, String> parentProgramCategoryMap = selectProgramCategoryMap(parentProgramCategoryIdList);
        return programList.stream().map(program -> {
            ProgramListVo programListVo = new ProgramListVo();
            BeanUtil.copyProperties(program, programListVo);
            ProgramShowTime programShowTime = programShowTimeMap.get(program.getId());
            if (Objects.nonNull(programShowTime)) {
                programListVo.setShowTime(programShowTime.getShowTime());
                programListVo.setShowDayTime(programShowTime.getShowDayTime());
                programListVo.setShowWeekTime(programShowTime.getShowWeekTime());
            }
            programListVo.setProgramCategoryName(programCategoryMap.get(program.getProgramCategoryId()));
            programListVo.setParentProgramCategoryName(parentProgramCategoryMap.get(program.getParentProgramCategoryId()));
            programListVo.setMaxPrice(Optional.ofNullable(ticketCategorieMap.get(program.getId()))
                    .map(TicketCategoryAggregate::getMaxPrice).orElse(null));
            programListVo.setMinPrice(Optional.ofNullable(ticketCategorieMap.get(program.getId()))
                    .map(TicketCategoryAggregate::getMinPrice).orElse(null));
            return programListVo;
        }).collect(Collectors.toList());
    }
    
    /**
     * 查询分类信息（数据库查询）
     * @param programPageListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public PageVo<ProgramListVo> dbSelectPage(ProgramPageListDto programPageListDto) {
        IPage<ProgramJoinShowTime> iPage = 
                programMapper.selectPage(PageUtil.getPageParams(programPageListDto), programPageListDto);
        return buildProgramListPage(iPage);
    }

    private PageVo<ProgramListVo> dbSearchPage(ProgramSearchDto programSearchDto) {
        IPage<ProgramJoinShowTime> iPage =
                programMapper.selectSearchPage(PageUtil.getPageParams(programSearchDto), programSearchDto);
        return buildProgramListPage(iPage);
    }

    private PageVo<ProgramListVo> looseDbSearchPage(ProgramSearchDto programSearchDto) {
        if (!SearchKeywordUtil.validContent(programSearchDto.getContent())) {
            return new PageVo<>(programSearchDto.getPageNumber(), programSearchDto.getPageSize(), 0, new ArrayList<>());
        }
        ProgramSearchDto fallbackDto = new ProgramSearchDto();
        BeanUtil.copyProperties(programSearchDto, fallbackDto);
        fallbackDto.setContent(null);
        fallbackDto.setPageNumber(1);
        fallbackDto.setPageSize(SEARCH_FALLBACK_MAX_SIZE);
        IPage<ProgramJoinShowTime> iPage =
                programMapper.selectSearchPage(PageUtil.getPageParams(fallbackDto), fallbackDto);
        PageVo<ProgramListVo> candidatePage = buildProgramListPage(iPage);
        if (CollectionUtil.isEmpty(candidatePage.getList())) {
            return new PageVo<>(programSearchDto.getPageNumber(), programSearchDto.getPageSize(), 0, new ArrayList<>());
        }
        List<ProgramListVo> matchedList = candidatePage.getList().stream()
                .filter(item -> programSearchScore(item, programSearchDto.getContent()) > 0)
                .sorted(Comparator.comparingInt((ProgramListVo item) ->
                        programSearchScore(item, programSearchDto.getContent())).reversed())
                .toList();
        int pageNumber = programSearchDto.getPageNumber();
        int pageSize = programSearchDto.getPageSize();
        int fromIndex = Math.min((pageNumber - 1) * pageSize, matchedList.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedList.size());
        return new PageVo<>(pageNumber, pageSize, matchedList.size(), matchedList.subList(fromIndex, toIndex));
    }

    private int programSearchScore(ProgramListVo programListVo, String content) {
        int score = 0;
        score += SearchKeywordUtil.fieldScore(programListVo.getTitle(), content, 80);
        score += SearchKeywordUtil.fieldScore(programListVo.getActor(), content, 70);
        score += SearchKeywordUtil.fieldScore(programListVo.getPlace(), content, 30);
        return score;
    }

    private PageVo<ProgramListVo> buildProgramListPage(IPage<ProgramJoinShowTime> iPage) {
        if (CollectionUtil.isEmpty(iPage.getRecords())) {
            return new PageVo<>(iPage.getCurrent(), iPage.getSize(), iPage.getTotal(), new ArrayList<>());
        }
        Set<Long> programCategoryIdList = 
                iPage.getRecords().stream().map(Program::getProgramCategoryId).collect(Collectors.toSet());
        Map<Long, String> programCategoryMap = selectProgramCategoryMap(programCategoryIdList);
        
        List<Long> programIdList = iPage.getRecords().stream().map(Program::getId).collect(Collectors.toList());
        Map<Long, TicketCategoryAggregate> ticketCategorieMap = selectTicketCategorieMap(programIdList);
        
        Map<Long,String> tempAreaMap = new HashMap<>(64);
        AreaSelectDto areaSelectDto = new AreaSelectDto();
        areaSelectDto.setIdList(iPage.getRecords().stream().map(Program::getAreaId).distinct().collect(Collectors.toList()));
        ApiResponse<List<AreaVo>> areaResponse = baseDataClient.selectByIdList(areaSelectDto);
        if (Objects.equals(areaResponse.getCode(), ApiResponse.ok().getCode())) {
            if (CollectionUtil.isNotEmpty(areaResponse.getData())) {
                tempAreaMap = areaResponse.getData().stream()
                        .collect(Collectors.toMap(AreaVo::getId,AreaVo::getName,(v1,v2) -> v2));
            }
        }else {
            log.error("base-data selectByIdList rpc error areaResponse:{}", JSON.toJSONString(areaResponse));
        }
        Map<Long,String> areaMap = tempAreaMap;
        
        return PageUtil.convertPage(iPage, programJoinShowTime -> {
            ProgramListVo programListVo = new ProgramListVo();
            BeanUtil.copyProperties(programJoinShowTime, programListVo);
            
            programListVo.setAreaName(areaMap.get(programJoinShowTime.getAreaId()));
            programListVo.setProgramCategoryName(programCategoryMap.get(programJoinShowTime.getProgramCategoryId()));
            programListVo.setMinPrice(Optional.ofNullable(ticketCategorieMap.get(programJoinShowTime.getId()))
                    .map(TicketCategoryAggregate::getMinPrice).orElse(null));
            programListVo.setMaxPrice(Optional.ofNullable(ticketCategorieMap.get(programJoinShowTime.getId()))
                    .map(TicketCategoryAggregate::getMaxPrice).orElse(null));
            return programListVo;
        });
    }
    
    /**
     * 查询节目详情
     * @param programGetDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public ProgramVo detail(ProgramGetDto programGetDto) {
        compositeContainer.execute(CompositeCheckType.PROGRAM_DETAIL_CHECK.getValue(),programGetDto);
        return getDetailV2(programGetDto);
    }
    
    /**
     * 查询节目详情V1
     * @param programGetDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public ProgramVo detailV1(ProgramGetDto programGetDto) {
        compositeContainer.execute(CompositeCheckType.PROGRAM_DETAIL_CHECK.getValue(),programGetDto);
        return getDetail(programGetDto);
    }
    
    /**
     * 查询节目详情V2
     * @param programGetDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public ProgramVo detailV2(ProgramGetDto programGetDto) {
        compositeContainer.execute(CompositeCheckType.PROGRAM_DETAIL_CHECK.getValue(),programGetDto);
        return getDetailV2(programGetDto);
    }
    
    /**
     * 查询节目详情执行
     * @param programGetDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public ProgramVo getDetail(ProgramGetDto programGetDto) {
        ProgramShowTime programShowTime = programShowTimeService.selectProgramShowTimeByProgramId(programGetDto.getId());
        ProgramVo programVo = programService.getById(programGetDto.getId(),DateUtils.countBetweenSecond(DateUtils.now(),
                programShowTime.getShowTime()), TimeUnit.SECONDS);
        programVo.setShowTime(programShowTime.getShowTime());
        programVo.setShowDayTime(programShowTime.getShowDayTime());
        programVo.setShowWeekTime(programShowTime.getShowWeekTime());
        
        ProgramGroupVo programGroupVo = programService.getProgramGroup(programVo.getProgramGroupId());
        programVo.setProgramGroupVo(programGroupVo);
        
        preloadTicketUserList(programVo.getHighHeat());
        
        preloadAccountOrderCount(programVo.getId());
        
        ProgramCategory programCategory = getProgramCategory(programVo.getProgramCategoryId());
        if (Objects.nonNull(programCategory)) {
            programVo.setProgramCategoryName(programCategory.getName());
        }
        ProgramCategory parentProgramCategory = getProgramCategory(programVo.getParentProgramCategoryId());
        if (Objects.nonNull(parentProgramCategory)) {
            programVo.setParentProgramCategoryName(parentProgramCategory.getName());
        }
        
        List<TicketCategoryVo> ticketCategoryVoList =
                ticketCategoryService.selectTicketCategoryListByProgramId(programVo.getId(),
                        DateUtils.countBetweenSecond(DateUtils.now(),programShowTime.getShowTime()), TimeUnit.SECONDS);
        programVo.setTicketCategoryVoList(ticketCategoryVoList);
        
        return programVo;
    }
    
    /**
     * 查询节目详情V2执行
     * @param programGetDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public ProgramVo getDetailV2(ProgramGetDto programGetDto) {
        ProgramShowTime programShowTime =
                programShowTimeService.selectProgramShowTimeByProgramIdMultipleCache(programGetDto.getId());
        
        ProgramVo programVo = programService.getByIdMultipleCache(programGetDto.getId(),programShowTime.getShowTime());
        
        programVo.setShowTime(programShowTime.getShowTime());
        programVo.setShowDayTime(programShowTime.getShowDayTime());
        programVo.setShowWeekTime(programShowTime.getShowWeekTime());
        
        ProgramGroupVo programGroupVo = programService.getProgramGroupMultipleCache(programVo.getProgramGroupId());
        programVo.setProgramGroupVo(programGroupVo);
        
        preloadTicketUserList(programVo.getHighHeat());
        
        preloadAccountOrderCount(programVo.getId());
        
        ProgramCategory programCategory = getProgramCategoryMultipleCache(programVo.getProgramCategoryId());
        if (Objects.nonNull(programCategory)) {
            programVo.setProgramCategoryName(programCategory.getName());
        }
        ProgramCategory parentProgramCategory = getProgramCategoryMultipleCache(programVo.getParentProgramCategoryId());
        if (Objects.nonNull(parentProgramCategory)) {
            programVo.setParentProgramCategoryName(parentProgramCategory.getName());
        }
        
        List<TicketCategoryVo> ticketCategoryVoList = ticketCategoryService
                .selectTicketCategoryListByProgramIdMultipleCache(programVo.getId(),programShowTime.getShowTime());
        programVo.setTicketCategoryVoList(ticketCategoryVoList);
        
        return programVo;
    }
    
    /**
     * 查询节目表详情执行（多级）
     * @param programId 节目id
     * @param showTime 节目演出时间
     * @return 执行后的结果
     * */
    public ProgramVo getByIdMultipleCache(Long programId, Date showTime){
        return localCacheProgram.getCache(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, programId).getRelKey(),
                key -> {
                    log.info("查询节目详情 从本地缓存没有查询到 节目id : {}",programId);
                    ProgramVo programVo = getById(programId,DateUtils.countBetweenSecond(DateUtils.now(),showTime),
                            TimeUnit.SECONDS);
                    programVo.setShowTime(showTime);
                    return programVo;
                });
    }
    
    public ProgramVo simpleGetByIdMultipleCache(Long programId){
        ProgramVo programVoCache = localCacheProgram.getCache(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 
                programId).getRelKey());
        if (Objects.nonNull(programVoCache)) {
            return programVoCache;
        }
        return redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, programId), ProgramVo.class);
    }
    
    public ProgramVo simpleGetProgramAndShowMultipleCache(Long programId){
        ProgramShowTime programShowTime =
                programShowTimeService.simpleSelectProgramShowTimeByProgramIdMultipleCache(programId);
        if (Objects.isNull(programShowTime)) {
            throw new TidesFrameException(BaseCode.PROGRAM_SHOW_TIME_NOT_EXIST);
        }
        
        ProgramVo programVo = simpleGetByIdMultipleCache(programId);
        if (Objects.isNull(programVo)) {
            throw new TidesFrameException(BaseCode.PROGRAM_NOT_EXIST);
        }
        
        programVo.setShowTime(programShowTime.getShowTime());
        programVo.setShowDayTime(programShowTime.getShowDayTime());
        programVo.setShowWeekTime(programShowTime.getShowWeekTime());
        
        return programVo;
    }
    
    @ServiceLock(lockType= LockType.Read,name = PROGRAM_LOCK,keys = {"#programId"})
    public ProgramVo getById(Long programId,Long expireTime,TimeUnit timeUnit) {
        ProgramVo programVo = 
                redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, programId), ProgramVo.class);
        if (Objects.nonNull(programVo)) {
            return programVo;
        }
        log.info("查询节目详情 从Redis缓存没有查询到 节目id : {}",programId);
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, GET_PROGRAM_LOCK, new String[]{String.valueOf(programId)});
        lock.lock();
        try {
            return redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM,programId)
                    ,ProgramVo.class,
                    () -> createProgramVo(programId)
                    ,expireTime,
                    timeUnit);
        }finally {
            lock.unlock();
        }
    }
    
    public ProgramGroupVo getProgramGroupMultipleCache(Long programGroupId){
        return localCacheProgramGroup.getCache(
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_GROUP, programGroupId).getRelKey(),
                key -> getProgramGroup(programGroupId));
    }
    @ServiceLock(lockType= LockType.Read,name = PROGRAM_GROUP_LOCK,keys = {"#programGroupId"})
    public ProgramGroupVo getProgramGroup(Long programGroupId) {
        ProgramGroupVo programGroupVo =
                redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_GROUP, programGroupId), ProgramGroupVo.class);
        if (Objects.nonNull(programGroupVo)) {
            return programGroupVo;
        }
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, GET_PROGRAM_LOCK, new String[]{String.valueOf(programGroupId)});
        lock.lock();
        try {
            programGroupVo = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_GROUP, programGroupId), 
                    ProgramGroupVo.class);
            if (Objects.isNull(programGroupVo)) {
                programGroupVo = createProgramGroupVo(programGroupId);
                redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_GROUP, programGroupId),programGroupVo,
                        DateUtils.countBetweenSecond(DateUtils.now(),programGroupVo.getRecentShowTime()),TimeUnit.SECONDS);
            }
            return programGroupVo;
        }finally {
            lock.unlock();
        }
    }
    
    public Map<Long, String> selectProgramCategoryMap(Collection<Long> programCategoryIdList){
        LambdaQueryWrapper<ProgramCategory> pcLambdaQueryWrapper = Wrappers.lambdaQuery(ProgramCategory.class)
                .in(ProgramCategory::getId, programCategoryIdList);
        List<ProgramCategory> programCategoryList = programCategoryMapper.selectList(pcLambdaQueryWrapper);
        return programCategoryList
                .stream()
                .collect(Collectors.toMap(ProgramCategory::getId, ProgramCategory::getName, (v1, v2) -> v2));
    }
    
    public Map<Long, TicketCategoryAggregate> selectTicketCategorieMap(List<Long> programIdList){
        List<TicketCategoryAggregate> ticketCategorieList = ticketCategoryMapper.selectAggregateList(programIdList);
        return ticketCategorieList
                .stream()
                .collect(Collectors.toMap(TicketCategoryAggregate::getProgramId, 
                        ticketCategory -> ticketCategory, (v1, v2) -> v2));
    }
    
    @RepeatExecuteLimit(name = REDUCE_REMAIN_NUMBER,keys = {"#reduceRemainNumberDto.programId","#reduceRemainNumberDto.screeningId","#reduceRemainNumberDto.seatIdList"})
    @Transactional(rollbackFor = Exception.class)
    public Boolean operateSeatLockAndTicketCategoryRemainNumber(ReduceRemainNumberDto reduceRemainNumberDto){
        if (Objects.nonNull(reduceRemainNumberDto.getScreeningId())) {
            return movieService.operateSeatLockAndTicketCategoryRemainNumber(reduceRemainNumberDto);
        }
        List<TicketCategoryCountDto> ticketCategoryCountDtoList = reduceRemainNumberDto.getTicketCategoryCountDtoList();
        List<Long> seatIdList = reduceRemainNumberDto.getSeatIdList();
        LambdaQueryWrapper<Seat> seatLambdaQueryWrapper = 
                Wrappers.lambdaQuery(Seat.class)
                        .eq(Seat::getProgramId,reduceRemainNumberDto.getProgramId())
                        .in(Seat::getId, seatIdList);
        //查询座位，进行相关验证
        List<Seat> seatList = seatMapper.selectList(seatLambdaQueryWrapper);
        if (CollectionUtil.isEmpty(seatList)) {
            throw new TidesFrameException(BaseCode.SEAT_NOT_EXIST);
        }
        if (seatList.size() != seatIdList.size()) {
            throw new TidesFrameException(BaseCode.SEAT_UPDATE_REL_COUNT_NOT_EQUAL_PRESET_COUNT);
        }
        for (Seat seat : seatList) {
            if (!Objects.equals(seat.getSellStatus(), SellStatus.NO_SOLD.getCode())) {
                throw new TidesFrameException(BaseCode.SEAT_IS_NOT_NOT_SOLD);
            }
        }
        //修改座位状态
        LambdaUpdateWrapper<Seat> seatLambdaUpdateWrapper = 
                Wrappers.lambdaUpdate(Seat.class)
                        .eq(Seat::getProgramId,reduceRemainNumberDto.getProgramId())
                        .in(Seat::getId, seatIdList);
        Seat updateSeat = new Seat();
        updateSeat.setSellStatus(reduceRemainNumberDto.getSellStatus());
        seatMapper.update(updateSeat,seatLambdaUpdateWrapper);
        
        int updateRemainNumberCount = 0;
        for (TicketCategoryCountDto ticketCategoryCountDto : ticketCategoryCountDtoList) {
            //修改余票
            updateRemainNumberCount = updateRemainNumberCount + ticketCategoryMapper.reduceRemainNumber(
                    ticketCategoryCountDto.getCount(), ticketCategoryCountDto.getTicketCategoryId(),
                    reduceRemainNumberDto.getProgramId());
        }
        if (updateRemainNumberCount != ticketCategoryCountDtoList.size()) {
            throw new TidesFrameException(BaseCode.UPDATE_TICKET_CATEGORY_COUNT_NOT_CORRECT);
        }
        return true;
    }
    
    @RepeatExecuteLimit(name = PAY_OR_CANCEL_PROGRAM_ORDER,keys = {"#programOperateDataDto.programId","#programOperateDataDto.screeningId","#programOperateDataDto.seatIdList"})
    @Transactional(rollbackFor = Exception.class)
    public Boolean operateProgramData(ProgramOperateDataDto programOperateDataDto){
        if (Objects.nonNull(programOperateDataDto.getScreeningId())) {
            return movieService.operateProgramData(programOperateDataDto);
        }
        List<Long> seatIdList = programOperateDataDto.getSeatIdList();
        LambdaQueryWrapper<Seat> seatLambdaQueryWrapper =
                Wrappers.lambdaQuery(Seat.class)
                        .eq(Seat::getProgramId,programOperateDataDto.getProgramId())
                        .in(Seat::getId, seatIdList);
        List<Seat> seatList = seatMapper.selectList(seatLambdaQueryWrapper);
        if (CollectionUtil.isEmpty(seatList)) {
            throw new TidesFrameException(BaseCode.SEAT_NOT_EXIST);
        }
        if (seatList.size() != seatIdList.size()) {
            throw new TidesFrameException(BaseCode.SEAT_UPDATE_REL_COUNT_NOT_EQUAL_PRESET_COUNT);
        }
        //座位的操作状态只能是售卖或者未售卖
        if (!Objects.equals(programOperateDataDto.getSellStatus(),SellStatus.SOLD.getCode()) && 
                !Objects.equals(programOperateDataDto.getSellStatus(),SellStatus.NO_SOLD.getCode())) {
            throw new TidesFrameException(BaseCode.SEAT_OPERATE_IS_NOT_NOT_SOLD_OR_SOLD);
        }
        Integer orderVersion = programOperateDataDto.getOrderVersion();
        //v1/v2/v3 在支付时才把数据库座位改为已售并扣减库存
        if (!ProgramOrderVersion.isV4Version(orderVersion)) {
            if (Objects.equals(programOperateDataDto.getSellStatus(), SellStatus.NO_SOLD.getCode())) {
                Seat updateSeat = new Seat();
                updateSeat.setSellStatus(SellStatus.NO_SOLD.getCode());
                seatMapper.update(updateSeat, Wrappers.lambdaUpdate(Seat.class)
                        .eq(Seat::getProgramId,programOperateDataDto.getProgramId())
                        .in(Seat::getId, seatIdList));
                List<TicketCategoryCountDto> ticketCategoryCountDtoList = programOperateDataDto.getTicketCategoryCountDtoList();
                int updateRemainNumberCount = 0;
                for (TicketCategoryCountDto ticketCategoryCountDto : ticketCategoryCountDtoList) {
                    updateRemainNumberCount = updateRemainNumberCount + ticketCategoryMapper.increaseRemainNumber(
                            ticketCategoryCountDto.getCount(), ticketCategoryCountDto.getTicketCategoryId(),
                            programOperateDataDto.getProgramId());
                }
                if (updateRemainNumberCount != ticketCategoryCountDtoList.size()) {
                    throw new TidesFrameException(BaseCode.UPDATE_TICKET_CATEGORY_COUNT_NOT_CORRECT);
                }
                return true;
            }
            for (Seat seat : seatList) {
                if (Objects.equals(seat.getSellStatus(), SellStatus.SOLD.getCode())) {
                    throw new TidesFrameException(BaseCode.SEAT_SOLD);
                }
            }
            LambdaUpdateWrapper<Seat> seatLambdaUpdateWrapper =
                    Wrappers.lambdaUpdate(Seat.class)
                            .eq(Seat::getProgramId,programOperateDataDto.getProgramId())
                            .in(Seat::getId, seatIdList);
            Seat updateSeat = new Seat();
            updateSeat.setSellStatus(SellStatus.SOLD.getCode());
            seatMapper.update(updateSeat,seatLambdaUpdateWrapper);
            List<TicketCategoryCountDto> ticketCategoryCountDtoList = programOperateDataDto.getTicketCategoryCountDtoList();
            int updateRemainNumberCount =
                    ticketCategoryMapper.batchUpdateRemainNumber(ticketCategoryCountDtoList,programOperateDataDto.getProgramId());
            if (updateRemainNumberCount != ticketCategoryCountDtoList.size()) {
                throw new TidesFrameException(BaseCode.UPDATE_TICKET_CATEGORY_COUNT_NOT_CORRECT);
            }
        }else {
            //v4/v41 在异步创建订单时已锁定数据库座位并扣减库存
            //验证座位现在的状态，只能是锁定中
            for (Seat seat : seatList) {
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
            
            Seat updateSeat = new Seat();
            //订单支付成功的操作
            if (Objects.equals(programOperateDataDto.getSellStatus(),SellStatus.SOLD.getCode())) {
                updateSeat.setSellStatus(SellStatus.SOLD.getCode());
                LambdaUpdateWrapper<Seat> seatLambdaUpdateWrapper =
                        Wrappers.lambdaUpdate(Seat.class)
                                .eq(Seat::getProgramId,programOperateDataDto.getProgramId())
                                .in(Seat::getId, seatIdList);
                seatMapper.update(updateSeat,seatLambdaUpdateWrapper);
            } else if (Objects.equals(programOperateDataDto.getSellStatus(),SellStatus.NO_SOLD.getCode())) {
                //订单取消的操作  
                updateSeat.setSellStatus(SellStatus.NO_SOLD.getCode());
                LambdaUpdateWrapper<Seat> seatLambdaUpdateWrapper =
                        Wrappers.lambdaUpdate(Seat.class)
                                .eq(Seat::getProgramId,programOperateDataDto.getProgramId())
                                .in(Seat::getId, seatIdList);
                seatMapper.update(updateSeat,seatLambdaUpdateWrapper);
                List<TicketCategoryCountDto> ticketCategoryCountDtoList = programOperateDataDto.getTicketCategoryCountDtoList();
                int updateRemainNumberCount = 0;
                //把库存增加回去
                for (TicketCategoryCountDto ticketCategoryCountDto : ticketCategoryCountDtoList) {
                    updateRemainNumberCount = updateRemainNumberCount + ticketCategoryMapper.increaseRemainNumber(
                            ticketCategoryCountDto.getCount(), ticketCategoryCountDto.getTicketCategoryId(),
                            programOperateDataDto.getProgramId());
                }
                if (updateRemainNumberCount != ticketCategoryCountDtoList.size()) {
                    throw new TidesFrameException(BaseCode.UPDATE_TICKET_CATEGORY_COUNT_NOT_CORRECT);
                }
            }
        }
        return true;
    }

    private void normalizeProgramListArea(ProgramListDto programListDto) {
        if (Objects.isNull(programListDto)) {
            return;
        }
        programListDto.setAreaIds(resolveProgramAreaIds(programListDto.getAreaId(), programListDto.getAreaIds()));
    }

    private void normalizeProgramPageArea(ProgramPageListDto programPageListDto) {
        if (Objects.isNull(programPageListDto)) {
            return;
        }
        programPageListDto.setAreaIds(resolveProgramAreaIds(programPageListDto.getAreaId(), programPageListDto.getAreaIds()));
    }

    private void normalizeProgramRecommendArea(ProgramRecommendListDto programRecommendListDto) {
        if (Objects.isNull(programRecommendListDto)) {
            return;
        }
        programRecommendListDto.setAreaIds(resolveProgramAreaIds(programRecommendListDto.getAreaId(),
                programRecommendListDto.getAreaIds()));
    }

    private List<Long> resolveProgramAreaIds(Long areaId, List<Long> areaIds) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        if (CollectionUtil.isNotEmpty(areaIds)) {
            areaIds.stream().filter(Objects::nonNull).forEach(result::add);
        }
        if (Objects.nonNull(areaId)) {
            result.add(areaId);
        }
        if (result.removeIf(id -> Objects.equals(id, NATIONAL_AREA_ID) || Objects.equals(id, 0L)) &&
                CollectionUtil.isEmpty(result)) {
            return null;
        }
        if (CollectionUtil.isEmpty(result)) {
            return null;
        }
        new ArrayList<>(result).forEach(id -> appendParentCityArea(id, result));
        return new ArrayList<>(result);
    }

    private void appendParentCityArea(Long areaId, Set<Long> result) {
        if (Objects.isNull(areaId)) {
            return;
        }
        AreaVo areaVo = getAreaVo(areaId);
        if (Objects.nonNull(areaVo) && Objects.equals(areaVo.getType(), 3) &&
                Objects.nonNull(areaVo.getParentId()) && !Objects.equals(areaVo.getParentId(), 0L)) {
            result.add(areaVo.getParentId());
            return;
        }
        if (Objects.equals(areaId, NANHU_AREA_ID)) {
            result.add(JIAXING_AREA_ID);
        }
    }

    private AreaVo getAreaVo(Long areaId) {
        AreaGetDto areaGetDto = new AreaGetDto();
        areaGetDto.setId(areaId);
        try {
            ApiResponse<AreaVo> areaResponse = baseDataClient.getById(areaGetDto);
            if (Objects.equals(areaResponse.getCode(), ApiResponse.ok().getCode())) {
                return areaResponse.getData();
            }
            log.warn("base-data rpc getById error when resolve area, areaResponse:{}",
                    JSON.toJSONString(areaResponse));
        } catch (Exception exception) {
            log.warn("base-data getById rpc error when resolve areaId:{}", areaId, exception);
        }
        return null;
    }
    
    private ProgramVo createProgramVo(Long programId){
        ProgramVo programVo = new ProgramVo();
        Program program = 
                Optional.ofNullable(programMapper.selectById(programId))
                        .orElseThrow(() -> new TidesFrameException(BaseCode.PROGRAM_NOT_EXIST));
        BeanUtil.copyProperties(program,programVo);
        AreaGetDto areaGetDto = new AreaGetDto();
        areaGetDto.setId(program.getAreaId());
        ApiResponse<AreaVo> areaResponse = baseDataClient.getById(areaGetDto);
        if (Objects.equals(areaResponse.getCode(), ApiResponse.ok().getCode())) {
            if (Objects.nonNull(areaResponse.getData())) {
                programVo.setAreaName(areaResponse.getData().getName());
            }
        }else {
            log.error("base-data rpc getById error areaResponse:{}", JSON.toJSONString(areaResponse));
        }
        return programVo;
    }
    
    private ProgramGroupVo createProgramGroupVo(Long programGroupId){
        ProgramGroupVo programGroupVo = new ProgramGroupVo();
        ProgramGroup programGroup =
                Optional.ofNullable(programGroupMapper.selectById(programGroupId))
                        .orElseThrow(() -> new TidesFrameException(BaseCode.PROGRAM_GROUP_NOT_EXIST));
        programGroupVo.setId(programGroup.getId());
        List<ProgramSimpleInfoVo> programSimpleInfoVoList =
                JSON.parseArray(programGroup.getProgramJson(), ProgramSimpleInfoVo.class);
        fillProgramGroupSimpleInfo(programSimpleInfoVoList);
        programGroupVo.setProgramSimpleInfoVoList(programSimpleInfoVoList);
        programGroupVo.setRecentShowTime(programGroup.getRecentShowTime());
        return programGroupVo;
    }

    private void fillProgramGroupSimpleInfo(List<ProgramSimpleInfoVo> programSimpleInfoVoList){
        if (CollectionUtil.isEmpty(programSimpleInfoVoList)) {
            return;
        }
        for (ProgramSimpleInfoVo programSimpleInfoVo : programSimpleInfoVoList) {
            Long programId = programSimpleInfoVo.getProgramId();
            if (Objects.isNull(programId)) {
                continue;
            }
            Program program = programMapper.selectById(programId);
            if (Objects.nonNull(program)) {
                programSimpleInfoVo.setTitle(program.getTitle());
                programSimpleInfoVo.setPlace(program.getPlace());
                programSimpleInfoVo.setAreaId(program.getAreaId());
            }
            ProgramShowTime programShowTime = programShowTimeMapper.selectOne(Wrappers.lambdaQuery(ProgramShowTime.class)
                    .eq(ProgramShowTime::getProgramId, programId)
                    .orderByAsc(ProgramShowTime::getShowTime)
                    .last("LIMIT 1"));
            if (Objects.nonNull(programShowTime)) {
                programSimpleInfoVo.setShowTime(programShowTime.getShowTime());
                programSimpleInfoVo.setShowWeekTime(programShowTime.getShowWeekTime());
            }
        }
    }
    
    public List<Long> getAllProgramIdList(){
        LambdaQueryWrapper<Program> programLambdaQueryWrapper =
                Wrappers.lambdaQuery(Program.class).eq(Program::getProgramStatus, BusinessStatus.YES.getCode())
                        .select(Program::getId);
        List<Program> programs = programMapper.selectList(programLambdaQueryWrapper);
        return programs.stream().map(Program::getId).collect(Collectors.toList());
    }
    
    public ProgramVo getDetailFromDb(Long programId) {
        ProgramVo programVo = createProgramVo(programId);
        
        ProgramCategory programCategory = getProgramCategory(programVo.getProgramCategoryId());
        if (Objects.nonNull(programCategory)) {
            programVo.setProgramCategoryName(programCategory.getName());
        }
        ProgramCategory parentProgramCategory = getProgramCategory(programVo.getParentProgramCategoryId());
        if (Objects.nonNull(parentProgramCategory)) {
            programVo.setParentProgramCategoryName(parentProgramCategory.getName());
        }
        
        LambdaQueryWrapper<ProgramShowTime> programShowTimeLambdaQueryWrapper =
                Wrappers.lambdaQuery(ProgramShowTime.class)
                        .eq(ProgramShowTime::getProgramId, programId)
                        .orderByAsc(ProgramShowTime::getShowTime)
                        .last("LIMIT 1");
        ProgramShowTime programShowTime = Optional.ofNullable(programShowTimeMapper.selectOne(programShowTimeLambdaQueryWrapper))
                .orElseThrow(() -> new TidesFrameException(BaseCode.PROGRAM_SHOW_TIME_NOT_EXIST));
        
        programVo.setShowTime(programShowTime.getShowTime());
        programVo.setShowDayTime(programShowTime.getShowDayTime());
        programVo.setShowWeekTime(programShowTime.getShowWeekTime());
        
        return programVo;
    }
    
    private void preloadTicketUserList(Integer highHeat){
        if (Objects.equals(highHeat, BusinessStatus.NO.getCode())) {
            return;
        }
        String userId = BaseParameterHolder.getParameter(USER_ID);
        String code = BaseParameterHolder.getParameter(CODE);
        if (StringUtil.isEmpty(userId) || StringUtil.isEmpty(code)) {
            return;
        }
        Boolean userLogin =
                redisCache.hasKey(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN, code, userId));
        if (!userLogin) {
            return;
        }
        BusinessThreadPool.execute(() -> {
            try {
                if (!redisCache.hasKey(RedisKeyBuild.createRedisKey(RedisKeyManage.TICKET_USER_LIST,userId))) {
                    TicketUserListDto ticketUserListDto = new TicketUserListDto();
                    ticketUserListDto.setUserId(Long.parseLong(userId));
                    ApiResponse<List<TicketUserVo>> apiResponse = userClient.list(ticketUserListDto);
                    if (Objects.equals(apiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
                        Optional.ofNullable(apiResponse.getData()).filter(CollectionUtil::isNotEmpty)
                                .ifPresent(ticketUserVoList -> redisCache.set(RedisKeyBuild.createRedisKey(
                                        RedisKeyManage.TICKET_USER_LIST,userId),ticketUserVoList));
                    }else {
                        log.warn("userClient.select 调用失败 apiResponse : {}",JSON.toJSONString(apiResponse));
                    }
                }
                
            }catch (Exception e) {
                log.error("预热加载购票人列表失败",e);
            }
        });
    }
    
    private void preloadAccountOrderCount(Long programId){
        String userId = BaseParameterHolder.getParameter(USER_ID);
        String code = BaseParameterHolder.getParameter(CODE);
        if (StringUtil.isEmpty(userId) || StringUtil.isEmpty(code)) {
            return;
        }
        Boolean userLogin =
                redisCache.hasKey(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN, code, userId));
        if (!userLogin) {
            return;
        }
        BusinessThreadPool.execute(() -> {
            try {
                if (!redisCache.hasKey(RedisKeyBuild.createRedisKey(RedisKeyManage.ACCOUNT_ORDER_COUNT,userId,programId))) {
                    AccountOrderCountDto accountOrderCountDto = new AccountOrderCountDto();
                    accountOrderCountDto.setUserId(Long.parseLong(userId));
                    accountOrderCountDto.setProgramId(programId);
                    ApiResponse<AccountOrderCountVo> apiResponse = orderClient.accountOrderCount(accountOrderCountDto);
                    if (Objects.equals(apiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
                        Optional.ofNullable(apiResponse.getData())
                                .ifPresent(accountOrderCountVo -> redisCache.set(
                                        RedisKeyBuild.createRedisKey(RedisKeyManage.ACCOUNT_ORDER_COUNT,userId,programId),
                                        accountOrderCountVo.getCount(), tokenExpireManager.getTokenExpireTime() + 1,
                                        TimeUnit.MINUTES));
                    }else {
                        log.warn("orderClient.accountOrderCount 调用失败 apiResponse : {}",JSON.toJSONString(apiResponse));
                    }
                }
            }catch (Exception e) {
                log.error("预热加载账户订单数量失败",e);
            }
        });
    }
    
    public ProgramCategory getProgramCategoryMultipleCache(Long programCategoryId){
        return localCacheProgramCategory.get(String.valueOf(programCategoryId),
                key -> getProgramCategory(programCategoryId));
    }
    
    public ProgramCategory getProgramCategory(Long programCategoryId){
        return programCategoryService.getProgramCategory(programCategoryId);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Boolean resetExecute(ProgramResetExecuteDto programResetExecuteDto) {
        Long programId = programResetExecuteDto.getProgramId();
        //查出该节目下锁定和已售卖的座位
        LambdaQueryWrapper<Seat> seatQueryWrapper =
                Wrappers.lambdaQuery(Seat.class).eq(Seat::getProgramId, programId)
                        .in(Seat::getSellStatus,SellStatus.LOCK.getCode(),SellStatus.SOLD.getCode());
        List<Seat> seatList = seatMapper.selectList(seatQueryWrapper);
        if (CollectionUtil.isNotEmpty(seatList)) {
            //执行到这里说明有锁定和已售卖的座位，那么就把该节目下的座位都重置一遍
            LambdaUpdateWrapper<Seat> seatUpdateWrapper =
                    Wrappers.lambdaUpdate(Seat.class).eq(Seat::getProgramId, programId);
            Seat seatUpdate = new Seat();
            seatUpdate.setSellStatus(SellStatus.NO_SOLD.getCode());
            seatMapper.update(seatUpdate,seatUpdateWrapper);
        }
        //查询该节目下的票档
        LambdaQueryWrapper<TicketCategory> ticketCategoryQueryWrapper =
                Wrappers.lambdaQuery(TicketCategory.class).eq(TicketCategory::getProgramId, programId);
        List<TicketCategory> ticketCategories = ticketCategoryMapper.selectList(ticketCategoryQueryWrapper);
        if (CollectionUtil.isNotEmpty(ticketCategories)) {
            for (TicketCategory ticketCategory : ticketCategories) {
                Long remainNumber = ticketCategory.getRemainNumber();
                Long totalNumber = ticketCategory.getTotalNumber();
                //如果总数和剩余数不一致，则进行重置
                if (!(remainNumber.equals(totalNumber))) {
                    TicketCategory ticketCategoryUpdate = new TicketCategory();
                    ticketCategoryUpdate.setRemainNumber(totalNumber);
                    
                    LambdaUpdateWrapper<TicketCategory> ticketCategoryUpdateWrapper =
                            Wrappers.lambdaUpdate(TicketCategory.class)
                                    .eq(TicketCategory::getProgramId, programId)
                                    .eq(TicketCategory::getId,ticketCategory.getId());
                    ticketCategoryMapper.update(ticketCategoryUpdate,ticketCategoryUpdateWrapper);
                }
            }
        }
        //删除缓存相关数据
        delRedisData(programId);
        //删除本地缓存数据
        delLocalCache(programId);
        return true;
    }
    
    public void delRedisData(Long programId){
        Program program = Optional.ofNullable(programMapper.selectById(programId))
                .orElseThrow(() -> new TidesFrameException(BaseCode.PROGRAM_NOT_EXIST));
        List<String> keys = new ArrayList<>();
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM,programId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_GROUP,program.getProgramGroupId()).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SHOW_TIME,programId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH, programId,"*").getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH, programId,"*").getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_SOLD_RESOLUTION_HASH, programId,"*").getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_TICKET_CATEGORY_LIST, programId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_TICKET_REMAIN_NUMBER_HASH_RESOLUTION, programId,"*").getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_RECORD, programId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_RECORD_FINISH, programId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.DISCARD_ORDER, programId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.ACCOUNT_ORDER_COUNT_ALL).getRelKey());
        programDelCacheData.del(keys,new String[]{});
    }
    
    public Boolean invalid(final ProgramInvalidDto programInvalidDto) {
        Program program = new Program();
        program.setId(programInvalidDto.getId());
        program.setProgramStatus(BusinessStatus.NO.getCode());
        int result = programMapper.updateById(program);
        if (result > 0) {
            delRedisData(programInvalidDto.getId());
            redisStreamPushHandler.push(String.valueOf(programInvalidDto.getId()));
            programEs.deleteByProgramId(programInvalidDto.getId());
            return true;
        }else {
            return false;
        }
    }
    
    public ProgramVo localDetail(final ProgramGetDto programGetDto) {
        return localCacheProgram.getCache(String.valueOf(programGetDto.getId()));
    }
    
    public void delLocalCache(Long programId){
        log.info("删除本地缓存 programId : {}",programId);
        localCacheProgram.del(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, programId).getRelKey());
        localCacheProgramGroup.del(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_GROUP, programId).getRelKey());
        localCacheProgramShowTime.del(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SHOW_TIME, programId).getRelKey());
        localCacheTicketCategory.del(programId);
    }
    
    public Boolean dataPreheat(ProgramDataPreheatDto programDataPreheatDto){
        ProgramResetExecuteDto programResetExecuteDto = new ProgramResetExecuteDto();
        programResetExecuteDto.setProgramId(programDataPreheatDto.getProgramId());
        //先把数据库中的座位和库存数据重置，然后删除本地缓存和redis缓存
        programService.resetExecute(programResetExecuteDto);
        
        ProgramGetDto programGetDto = new ProgramGetDto();
        programGetDto.setId(programDataPreheatDto.getProgramId());
        //再将节目相关的数据预热到缓存中，包括本地缓存和redis缓存
        ProgramVo programVo = getDetailV2(programGetDto);
        if (Objects.isNull(programVo)) {
            return false;
        }
        //再将座位和库存数据预热到redis缓存中
        Date showDayTime = programVo.getShowDayTime();
        List<TicketCategoryVo> ticketCategoryVoList = programVo.getTicketCategoryVoList();
        for (TicketCategoryVo ticketCategoryVo : ticketCategoryVoList) {
            seatService.selectSeatResolution(programDataPreheatDto.getProgramId(), 
                    ticketCategoryVo.getId(), DateUtils.countBetweenSecond(DateUtils.now(), showDayTime), TimeUnit.SECONDS);
            ticketCategoryService.getRedisRemainNumberResolution(programDataPreheatDto.getProgramId(), ticketCategoryVo.getId());
        }
        return true;
    }
}
