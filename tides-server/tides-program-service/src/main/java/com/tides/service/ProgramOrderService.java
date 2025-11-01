package com.tides.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidu.fsg.uid.UidGenerator;
import com.tides.BusinessThreadPool;
import com.tides.client.OrderClient;
import com.tides.common.ApiResponse;
import com.tides.core.RedisKeyManage;
import com.tides.domain.OrderCreateMq;
import com.tides.domain.PurchaseSeat;
import com.tides.dto.DelayOrderCancelDto;
import com.tides.dto.OrderCreateDto;
import com.tides.dto.OrderTicketUserCreateDto;
import com.tides.dto.ProgramOrderCreateDto;
import com.tides.dto.SeatDto;
import com.tides.entity.MovieScreening;
import com.tides.entity.ProgramRecordTask;
import com.tides.entity.ProgramShowTime;
import com.tides.enums.BaseCode;
import com.tides.enums.OrderStatus;
import com.tides.enums.RecordType;
import com.tides.enums.SellStatus;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.ProgramRecordTaskMapper;
import com.tides.redis.RedisKeyBuild;
import com.tides.redis.RedisCache;
import com.tides.service.delaysend.DelayOrderCancelSend;
import com.tides.service.domain.CreateOrderTemporaryData;
import com.tides.service.kafka.CreateOrderMqDomain;
import com.tides.service.kafka.CreateOrderSend;
import com.tides.service.lua.ProgramCacheCreateOrderData;
import com.tides.service.lua.ProgramCacheCreateOrderResolutionOperate;
import com.tides.service.lua.ProgramCacheResolutionOperate;
import com.tides.service.tool.SeatMatch;
import com.tides.util.DateUtils;
import com.tides.util.StringUtil;
import com.tides.vo.MovieScreeningVo;
import com.tides.vo.ProgramVo;
import com.tides.vo.SeatVo;
import com.tides.vo.TicketCategoryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tides.constant.Constant.GLIDE_LINE;

/**
 * @description: 节目订单 service
 * @author: 19continue
 **/
@Slf4j
@Service
public class ProgramOrderService {

    @Autowired
    private OrderClient orderClient;

    @Autowired
    private UidGenerator uidGenerator;

    @Autowired
    private ProgramCacheResolutionOperate programCacheResolutionOperate;

    @Autowired
    ProgramCacheCreateOrderResolutionOperate programCacheCreateOrderResolutionOperate;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private DelayOrderCancelSend delayOrderCancelSend;

    @Autowired
    private CreateOrderSend createOrderSend;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramShowTimeService programShowTimeService;

    @Autowired
    private TicketCategoryService ticketCategoryService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private ProgramRecordTaskMapper programRecordTaskMapper;

    public List<TicketCategoryVo> getTicketCategoryList(ProgramOrderCreateDto programOrderCreateDto, Date showTime){
        List<TicketCategoryVo> getTicketCategoryVoList = new ArrayList<>();
        List<TicketCategoryVo> ticketCategoryVoList =
                isMovieOrder(programOrderCreateDto) ?
                        movieService.selectTicketCategoryListByScreeningId(programOrderCreateDto.getScreeningId()) :
                        ticketCategoryService.selectTicketCategoryListByProgramIdMultipleCache(
                                programOrderCreateDto.getProgramId(), showTime);
        Map<Long, TicketCategoryVo> ticketCategoryVoMap =
                ticketCategoryVoList.stream()
                        .collect(Collectors.toMap(TicketCategoryVo::getId, ticketCategoryVo -> ticketCategoryVo));
        List<SeatDto> seatDtoList = programOrderCreateDto.getSeatDtoList();
        if (CollectionUtil.isNotEmpty(seatDtoList)) {
            for (SeatDto seatDto : seatDtoList) {
                TicketCategoryVo ticketCategoryVo = ticketCategoryVoMap.get(seatDto.getTicketCategoryId());
                if (Objects.nonNull(ticketCategoryVo)) {
                    getTicketCategoryVoList.add(ticketCategoryVo);
                }else {
                    throw new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2);
                }
            }
        } else {
            TicketCategoryVo ticketCategoryVo = ticketCategoryVoMap.get(programOrderCreateDto.getTicketCategoryId());
            if (Objects.nonNull(ticketCategoryVo)) {
                getTicketCategoryVoList.add(ticketCategoryVo);
            }else {
                throw new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2);
            }
        }
        return getTicketCategoryVoList;
    }

    public String create(ProgramOrderCreateDto programOrderCreateDto,Integer orderVersion) {
        Date showTime = getShowTime(programOrderCreateDto);
        List<TicketCategoryVo> getTicketCategoryList =
                getTicketCategoryList(programOrderCreateDto, showTime);
        List<SeatVo> purchaseSeatList = new ArrayList<>();
        List<SeatDto> seatDtoList = programOrderCreateDto.getSeatDtoList();
        List<SeatVo> seatVoList = new ArrayList<>();
        Map<String, Long> ticketCategoryRemainNumber = new HashMap<>(16);
        for (TicketCategoryVo ticketCategory : getTicketCategoryList) {
            List<SeatVo> allSeatVoList =
                    selectSeatResolution(programOrderCreateDto, ticketCategory.getId(),
                            DateUtils.countBetweenSecond(DateUtils.now(), showTime), TimeUnit.SECONDS);
            seatVoList.addAll(allSeatVoList.stream().
                    filter(seatVo -> seatVo.getSellStatus().equals(SellStatus.NO_SOLD.getCode())).toList());
            ticketCategoryRemainNumber.putAll(getRedisRemainNumberResolution(programOrderCreateDto, ticketCategory.getId()));
        }
        if (CollectionUtil.isNotEmpty(seatDtoList)) {
            Map<Long, Long> seatTicketCategoryDtoCount = seatDtoList.stream()
                    .collect(Collectors.groupingBy(SeatDto::getTicketCategoryId, Collectors.counting()));
            for (Entry<Long, Long> entry : seatTicketCategoryDtoCount.entrySet()) {
                Long ticketCategoryId = entry.getKey();
                Long purchaseCount = entry.getValue();
                Long remainNumber = Optional.ofNullable(ticketCategoryRemainNumber.get(String.valueOf(ticketCategoryId)))
                        .orElseThrow(() -> new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2));
                if (purchaseCount > remainNumber) {
                    throw new TidesFrameException(BaseCode.TICKET_REMAIN_NUMBER_NOT_SUFFICIENT);
                }
            }
            Map<Long, SeatVo> seatVoMap = seatVoList.stream()
                    .collect(Collectors.toMap(SeatVo::getId, seat -> seat, (v1, v2) -> v2));
            for (SeatDto seatDto : seatDtoList) {
                SeatVo seatVo = seatVoMap.get(seatDto.getId());
                if (Objects.isNull(seatVo) ||
                        !Objects.equals(seatDto.getTicketCategoryId(), seatVo.getTicketCategoryId()) ||
                        !Objects.equals(seatDto.getRowCode(), seatVo.getRowCode()) ||
                        !Objects.equals(seatDto.getColCode(), seatVo.getColCode())) {
                    throw new TidesFrameException(BaseCode.SEAT_IS_NOT_NOT_SOLD);
                }
                if (seatDto.getPrice().compareTo(seatVo.getPrice()) != 0) {
                    throw new TidesFrameException(BaseCode.PRICE_ERROR);
                }
                purchaseSeatList.add(seatVo);
            }
        }else {
            Long ticketCategoryId = programOrderCreateDto.getTicketCategoryId();
            Integer ticketCount = programOrderCreateDto.getTicketCount();
            Long remainNumber = Optional.ofNullable(ticketCategoryRemainNumber.get(String.valueOf(ticketCategoryId)))
                    .orElseThrow(() -> new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2));
            if (ticketCount > remainNumber) {
                throw new TidesFrameException(BaseCode.TICKET_REMAIN_NUMBER_NOT_SUFFICIENT);
            }
            purchaseSeatList = SeatMatch.findAdjacentSeatVos(seatVoList.stream().filter(seatVo ->
                    Objects.equals(seatVo.getTicketCategoryId(), ticketCategoryId)).collect(Collectors.toList()), ticketCount);
            if (purchaseSeatList.size() < ticketCount) {
                throw new TidesFrameException(BaseCode.SEAT_OCCUPY);
            }
        }
        updateProgramCacheDataResolution(programOrderCreateDto, purchaseSeatList, OrderStatus.NO_PAY);
        return doCreate(programOrderCreateDto,purchaseSeatList,orderVersion);
    }


    public String createNew(ProgramOrderCreateDto programOrderCreateDto,Integer orderVersion) {
        CreateOrderIdempotent createOrderIdempotent = prepareCreateOrderIdempotent(programOrderCreateDto);
        if (createOrderIdempotent.exists) {
            return String.valueOf(createOrderIdempotent.orderNumber);
        }
        try {
            CreateOrderTemporaryData createOrderTemporaryData = createOrderOperateProgramCacheResolution(programOrderCreateDto);
            List<SeatVo> purchaseSeatList = createOrderTemporaryData.getPurchaseSeatList().stream().map(purchaseSeat -> {
                SeatVo seatVo = new SeatVo();
                BeanUtils.copyProperties(purchaseSeat,seatVo);
                return seatVo;
            }).collect(Collectors.toList());
            return doCreate(programOrderCreateDto,purchaseSeatList,orderVersion,createOrderIdempotent.orderNumber);
        } catch (RuntimeException ex) {
            clearCreateOrderIdempotent(createOrderIdempotent);
            throw ex;
        }
    }

    public String createNewAsync(ProgramOrderCreateDto programOrderCreateDto,Integer orderVersion) {
        CreateOrderIdempotent createOrderIdempotent = prepareCreateOrderIdempotent(programOrderCreateDto);
        if (createOrderIdempotent.exists) {
            return String.valueOf(createOrderIdempotent.orderNumber);
        }
        try {
            //操作redis
            CreateOrderTemporaryData createOrderTemporaryData = createOrderOperateProgramCacheResolution(programOrderCreateDto);
            //发送kafka
            return doCreateV2(programOrderCreateDto,createOrderTemporaryData,orderVersion,createOrderIdempotent.orderNumber);
        } catch (RuntimeException ex) {
            clearCreateOrderIdempotent(createOrderIdempotent);
            throw ex;
        }
    }
    
    public String createNewAsyncFast(ProgramOrderCreateDto programOrderCreateDto,Integer orderVersion) {
        CreateOrderIdempotent createOrderIdempotent = prepareCreateOrderIdempotent(programOrderCreateDto);
        if (createOrderIdempotent.exists) {
            return String.valueOf(createOrderIdempotent.orderNumber);
        }
        try {
            CreateOrderTemporaryData createOrderTemporaryData = createOrderOperateProgramCacheResolution(programOrderCreateDto);
            return doCreateV2Fast(programOrderCreateDto, createOrderTemporaryData, orderVersion, createOrderIdempotent);
        } catch (RuntimeException ex) {
            clearCreateOrderIdempotent(createOrderIdempotent);
            throw ex;
        }
    }

    private CreateOrderIdempotent prepareCreateOrderIdempotent(ProgramOrderCreateDto programOrderCreateDto) {
        Long orderNumber = uidGenerator.getOrderNumber(programOrderCreateDto.getUserId());
        if (StringUtil.isEmpty(programOrderCreateDto.getClientRequestId())) {
            return new CreateOrderIdempotent(orderNumber, false, null);
        }
        RedisKeyBuild redisKeyBuild = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_ORDER_CREATE_IDEMPOTENT,
                programOrderCreateDto.getUserId(), programOrderCreateDto.getClientRequestId());
        Long existedOrderNumber = redisCache.get(redisKeyBuild, Long.class);
        if (Objects.nonNull(existedOrderNumber)) {
            return new CreateOrderIdempotent(existedOrderNumber, true, redisKeyBuild);
        }
        boolean setResult = redisCache.setIfAbsent(redisKeyBuild, orderNumber);
        if (setResult) {
            redisCache.expire(redisKeyBuild, 30, TimeUnit.MINUTES);
            return new CreateOrderIdempotent(orderNumber, false, redisKeyBuild);
        }
        existedOrderNumber = redisCache.get(redisKeyBuild, Long.class);
        if (Objects.nonNull(existedOrderNumber)) {
            return new CreateOrderIdempotent(existedOrderNumber, true, redisKeyBuild);
        }
        throw new TidesFrameException(BaseCode.ORDER_EXIST);
    }

    private void clearCreateOrderIdempotent(CreateOrderIdempotent createOrderIdempotent) {
        if (Objects.nonNull(createOrderIdempotent) && Objects.nonNull(createOrderIdempotent.redisKeyBuild) &&
                !createOrderIdempotent.exists) {
            redisCache.del(createOrderIdempotent.redisKeyBuild);
        }
    }

    public CreateOrderTemporaryData createOrderOperateProgramCacheResolution(ProgramOrderCreateDto programOrderCreateDto){
        Date showTime = getShowTime(programOrderCreateDto);
        //查询对应的票档类型
        List<TicketCategoryVo> getTicketCategoryList =
                getTicketCategoryList(programOrderCreateDto, showTime);
        //遍历得到的票档
        for (TicketCategoryVo ticketCategory : getTicketCategoryList) {
            //从缓存中查询座位，如果缓存不存在，则从数据库查询后再放入缓存
            selectSeatResolution(programOrderCreateDto, ticketCategory.getId(),
                    DateUtils.countBetweenSecond(DateUtils.now(), showTime), TimeUnit.SECONDS);
            //从缓存中查询余票数量，如果缓存不存在，则从数据库查询后再放入缓存
            getRedisRemainNumberResolution(programOrderCreateDto, ticketCategory.getId());
        }
        Long inventoryId = inventoryId(programOrderCreateDto);
        List<SeatDto> seatDtoList = programOrderCreateDto.getSeatDtoList();
        List<String> keys = new ArrayList<>();
        String[] data = new String[7];
        String orderType;
        //更新票档数据集合
        JSONArray jsonArray = new JSONArray();
        //添加座位数据集合
        JSONArray addSeatDatajsonArray = new JSONArray();
        if (CollectionUtil.isNotEmpty(seatDtoList)) {
            orderType = "1";
            Map<Long, List<SeatDto>> seatTicketCategoryDtoCount = seatDtoList.stream()
                    .collect(Collectors.groupingBy(SeatDto::getTicketCategoryId, LinkedHashMap::new, Collectors.toList()));
            for (Entry<Long, List<SeatDto>> entry : seatTicketCategoryDtoCount.entrySet()) {
                Long ticketCategoryId = entry.getKey();
                int ticketCount = entry.getValue().size();
                //这里是计算更新票档数据
                JSONObject jsonObject = new JSONObject();
                //票档数量的key
                jsonObject.put("programTicketRemainNumberHashKey", ticketRemainNumberHashKey(
                        programOrderCreateDto, ticketCategoryId));
                //票档id
                jsonObject.put("ticketCategoryId",ticketCategoryId);
                //扣减余票数量
                jsonObject.put("ticketCount",ticketCount);
                jsonArray.add(jsonObject);

                JSONObject seatDatajsonObject = new JSONObject();
                //未售卖座位的hash的key
                seatDatajsonObject.put("seatNoSoldHashKey", seatNoSoldHashKey(programOrderCreateDto, ticketCategoryId));
                //座位数据
                seatDatajsonObject.put("seatDataList",JSON.toJSONString(entry.getValue()));
                addSeatDatajsonArray.add(seatDatajsonObject);
            }
        }else {
            orderType = "2";
            Long ticketCategoryId = programOrderCreateDto.getTicketCategoryId();
            Integer ticketCount = programOrderCreateDto.getTicketCount();
            JSONObject jsonObject = new JSONObject();
            //票档数量的key
            jsonObject.put("programTicketRemainNumberHashKey", ticketRemainNumberHashKey(
                    programOrderCreateDto, ticketCategoryId));
            //票档id
            jsonObject.put("ticketCategoryId",ticketCategoryId);
            //扣减余票数量
            jsonObject.put("ticketCount",ticketCount);
            //未售卖座位的hash的key
            jsonObject.put("seatNoSoldHashKey", seatNoSoldHashKey(programOrderCreateDto, ticketCategoryId));
            jsonArray.add(jsonObject);
        }
        //未售卖座位hash的key(占位符形式)
        keys.add(RedisKeyBuild.getRedisKey(seatNoSoldKey(programOrderCreateDto)));
        //锁定座位hash的key(占位符形式)
        keys.add(RedisKeyBuild.getRedisKey(seatLockKey(programOrderCreateDto)));
        //记录的key(占位符形式)
        keys.add(RedisKeyBuild.getRedisKey(recordKey(programOrderCreateDto)));
        //记录的标识
        Long identifierId = uidGenerator.getUid();
        //把记录的标识id放进去
        //记录的类型
        data[0] = orderType;
        data[1] = JSON.toJSONString(jsonArray);
        data[2] = JSON.toJSONString(addSeatDatajsonArray);
        //购票人id集合
        data[3] = JSON.toJSONString(buildLuaTicketUserIdList(programOrderCreateDto));
        data[4] = String.valueOf(inventoryId);
        data[5] = RecordType.REDUCE.getValue() + GLIDE_LINE + identifierId + GLIDE_LINE + programOrderCreateDto.getUserId();
        data[6] = RecordType.REDUCE.getValue();
        //执行lua脚本
        ProgramCacheCreateOrderData programCacheCreateOrderData =
                programCacheCreateOrderResolutionOperate.programCacheOperate(keys, data);
        if (!Objects.equals(programCacheCreateOrderData.getCode(), BaseCode.SUCCESS.getCode())) {
            throw new TidesFrameException(Objects.requireNonNull(BaseCode.getRc(programCacheCreateOrderData.getCode())));
        }
        List<PurchaseSeat> purchaseSeatList = programCacheCreateOrderData.getPurchaseSeatList();
        alignPurchaseSeatTicketUserIds(programOrderCreateDto, purchaseSeatList);
        return new CreateOrderTemporaryData(identifierId,purchaseSeatList);
    }

    private List<String> buildLuaTicketUserIdList(ProgramOrderCreateDto programOrderCreateDto) {
        return programOrderCreateDto.getTicketUserIdList().stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    private void alignPurchaseSeatTicketUserIds(ProgramOrderCreateDto programOrderCreateDto,
                                                List<PurchaseSeat> purchaseSeatList) {
        List<Long> ticketUserIdList = programOrderCreateDto.getTicketUserIdList();
        if (CollectionUtil.isEmpty(ticketUserIdList) || CollectionUtil.isEmpty(purchaseSeatList)) {
            return;
        }
        if (ticketUserIdList.size() != purchaseSeatList.size()) {
            log.error("ticket user count and locked seat count mismatch, userId: {}, programId: {}, screeningId: {}, ticketUserCount: {}, seatCount: {}",
                    programOrderCreateDto.getUserId(), programOrderCreateDto.getProgramId(),
                    programOrderCreateDto.getScreeningId(), ticketUserIdList.size(), purchaseSeatList.size());
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        for (int i = 0; i < purchaseSeatList.size(); i++) {
            purchaseSeatList.get(i).setTicketUserId(ticketUserIdList.get(i));
        }
    }

    private String doCreate(ProgramOrderCreateDto programOrderCreateDto,List<SeatVo> purchaseSeatList,Integer orderVersion){
        OrderCreateDto orderCreateDto = buildCreateOrderParam(programOrderCreateDto, purchaseSeatList, orderVersion,
                uidGenerator.getOrderNumber(programOrderCreateDto.getUserId()));

        String createdOrderNumber = createOrderByRpc(orderCreateDto,purchaseSeatList);

        DelayOrderCancelDto delayOrderCancelDto = new DelayOrderCancelDto();
        delayOrderCancelDto.setProgramId(programOrderCreateDto.getProgramId());
        delayOrderCancelDto.setOrderNumber(orderCreateDto.getOrderNumber());
        delayOrderCancelSend.sendMessage(delayOrderCancelDto);

        return createdOrderNumber;
    }

    private String doCreate(ProgramOrderCreateDto programOrderCreateDto,List<SeatVo> purchaseSeatList,Integer orderVersion,
                            Long orderNumber){
        OrderCreateDto orderCreateDto = buildCreateOrderParam(programOrderCreateDto, purchaseSeatList, orderVersion,
                orderNumber);

        String createdOrderNumber = createOrderByRpc(orderCreateDto,purchaseSeatList);

        DelayOrderCancelDto delayOrderCancelDto = new DelayOrderCancelDto();
        delayOrderCancelDto.setProgramId(programOrderCreateDto.getProgramId());
        delayOrderCancelDto.setOrderNumber(orderCreateDto.getOrderNumber());
        delayOrderCancelSend.sendMessage(delayOrderCancelDto);

        return createdOrderNumber;
    }

    private String doCreateV2(ProgramOrderCreateDto programOrderCreateDto,
                              CreateOrderTemporaryData createOrderTemporaryData,
                              Integer orderVersion,
                              Long orderNumber){
        OrderCreateDto orderCreateDto = buildCreateOrderParamV2(programOrderCreateDto,
                createOrderTemporaryData.getPurchaseSeatList(),orderVersion,orderNumber);
        OrderCreateMq orderCreateMq = new OrderCreateMq();
        BeanUtils.copyProperties(orderCreateDto,orderCreateMq);
        orderCreateMq.setIdentifierId(createOrderTemporaryData.getIdentifierId());
        //电影场次使用 MOVIE_SCREENING_RECORD，避免进入普通演出 PROGRAM_RECORD 对账任务
        if (!isMovieOrder(programOrderCreateDto)) {
            BusinessThreadPool.execute(() -> createProgramRecordTask(orderCreateMq.getProgramId()));
        }
        //创建订单
        String createdOrderNumber = createOrderByMq(orderCreateMq,createOrderTemporaryData.getPurchaseSeatList());
        DelayOrderCancelDto delayOrderCancelDto = new DelayOrderCancelDto();
        delayOrderCancelDto.setProgramId(orderCreateDto.getProgramId());
        delayOrderCancelDto.setOrderNumber(orderCreateDto.getOrderNumber());
        delayOrderCancelSend.sendMessage(delayOrderCancelDto);

        return createdOrderNumber;
    }
    
    private String doCreateV2Fast(ProgramOrderCreateDto programOrderCreateDto,
                                  CreateOrderTemporaryData createOrderTemporaryData,
                                  Integer orderVersion,
                                  CreateOrderIdempotent createOrderIdempotent){
        OrderCreateDto orderCreateDto = buildCreateOrderParamV2(programOrderCreateDto,
                createOrderTemporaryData.getPurchaseSeatList(), orderVersion, createOrderIdempotent.orderNumber);
        OrderCreateMq orderCreateMq = new OrderCreateMq();
        BeanUtils.copyProperties(orderCreateDto, orderCreateMq);
        orderCreateMq.setIdentifierId(createOrderTemporaryData.getIdentifierId());
        createOrderSend.sendMessageQuietly(JSON.toJSONString(orderCreateMq), sendResult -> {
            log.debug("创建订单快速入口 kafka 发送成功 topic : {}", sendResult.getRecordMetadata().topic());
            if (!isMovieOrder(programOrderCreateDto)) {
                BusinessThreadPool.execute(() -> createProgramRecordTask(orderCreateMq.getProgramId()));
            }
            DelayOrderCancelDto delayOrderCancelDto = new DelayOrderCancelDto();
            delayOrderCancelDto.setProgramId(orderCreateDto.getProgramId());
            delayOrderCancelDto.setOrderNumber(orderCreateDto.getOrderNumber());
            delayOrderCancelSend.sendMessage(delayOrderCancelDto);
        }, ex -> {
            log.error("创建订单快速入口 kafka 发送失败，开始回滚缓存 orderNumber : {}",
                    orderCreateMq.getOrderNumber(), ex);
            rollbackFastCreateOrder(orderCreateMq, createOrderTemporaryData.getPurchaseSeatList(),
                    createOrderIdempotent);
        });
        return String.valueOf(orderCreateDto.getOrderNumber());
    }
    
    private void rollbackFastCreateOrder(OrderCreateMq orderCreateMq,
                                         List<PurchaseSeat> purchaseSeatList,
                                         CreateOrderIdempotent createOrderIdempotent) {
        try {
            List<SeatVo> purchaseSeatVoList = purchaseSeatList.stream().map(purchaseSeat -> {
                SeatVo seatVo = new SeatVo();
                BeanUtils.copyProperties(purchaseSeat, seatVo);
                return seatVo;
            }).collect(Collectors.toList());
            updateProgramCacheDataResolution(orderCreateMq.getProgramId(), orderCreateMq.getScreeningId(),
                    purchaseSeatVoList, OrderStatus.CANCEL);
        } catch (Exception exception) {
            log.error("创建订单快速入口回滚缓存失败 orderNumber : {}", orderCreateMq.getOrderNumber(), exception);
        } finally {
            clearCreateOrderIdempotent(createOrderIdempotent);
        }
    }

    public void createProgramRecordTask(Long programId){
        ProgramRecordTask programRecordTask = new ProgramRecordTask();
        programRecordTask.setId(uidGenerator.getUid());
        programRecordTask.setProgramId(programId);
        programRecordTask.setCreateTime(DateUtils.now());
        programRecordTask.setEditTime(DateUtils.now());
        programRecordTaskMapper.insert(programRecordTask);
    }

    private OrderCreateDto buildCreateOrderParam(ProgramOrderCreateDto programOrderCreateDto,
                                                 List<SeatVo> purchaseSeatList,
                                                 Integer orderVersion,
                                                 Long orderNumber){
        ProgramVo programVo = getOrderProgramVo(programOrderCreateDto);
        OrderCreateDto orderCreateDto = new OrderCreateDto();
        orderCreateDto.setOrderNumber(orderNumber);
        orderCreateDto.setProgramId(programOrderCreateDto.getProgramId());
        orderCreateDto.setScreeningId(programOrderCreateDto.getScreeningId());
        orderCreateDto.setProgramItemPicture(programVo.getItemPicture());
        orderCreateDto.setUserId(programOrderCreateDto.getUserId());
        orderCreateDto.setProgramTitle(programVo.getTitle());
        orderCreateDto.setProgramPlace(programVo.getPlace());
        orderCreateDto.setProgramShowTime(programVo.getShowTime());
        orderCreateDto.setProgramPermitChooseSeat(programVo.getPermitChooseSeat());
        fillMovieOrderSnapshot(programOrderCreateDto, orderCreateDto);
        BigDecimal databaseOrderPrice =
                purchaseSeatList.stream().map(SeatVo::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        orderCreateDto.setOrderPrice(databaseOrderPrice);
        orderCreateDto.setCreateOrderTime(DateUtils.now());
        orderCreateDto.setOrderVersion(orderVersion);

        List<Long> ticketUserIdList = programOrderCreateDto.getTicketUserIdList();
        List<OrderTicketUserCreateDto> orderTicketUserCreateDtoList = new ArrayList<>();
        for (int i = 0; i < ticketUserIdList.size(); i++) {
            Long ticketUserId = ticketUserIdList.get(i);
            OrderTicketUserCreateDto orderTicketUserCreateDto = new OrderTicketUserCreateDto();
            orderTicketUserCreateDto.setOrderNumber(orderCreateDto.getOrderNumber());
            orderTicketUserCreateDto.setProgramId(programOrderCreateDto.getProgramId());
            orderTicketUserCreateDto.setScreeningId(programOrderCreateDto.getScreeningId());
            orderTicketUserCreateDto.setUserId(programOrderCreateDto.getUserId());
            orderTicketUserCreateDto.setTicketUserId(ticketUserId);
            SeatVo seatVo =
                    Optional.ofNullable(purchaseSeatList.get(i))
                            .orElseThrow(() -> new TidesFrameException(BaseCode.SEAT_NOT_EXIST));
            orderTicketUserCreateDto.setSeatId(seatVo.getId());
            orderTicketUserCreateDto.setSeatInfo(seatVo.getRowCode()+"排"+seatVo.getColCode()+"列");
            orderTicketUserCreateDto.setTicketCategoryId(seatVo.getTicketCategoryId());
            orderTicketUserCreateDto.setOrderPrice(seatVo.getPrice());
            orderTicketUserCreateDto.setCreateOrderTime(DateUtils.now());
            orderTicketUserCreateDtoList.add(orderTicketUserCreateDto);
        }

        orderCreateDto.setOrderTicketUserCreateDtoList(orderTicketUserCreateDtoList);

        return orderCreateDto;
    }

    private OrderCreateDto buildCreateOrderParamV2(ProgramOrderCreateDto programOrderCreateDto,
                                                   List<PurchaseSeat> purchaseSeatList,
                                                   Integer orderVersion,
                                                   Long orderNumber){
        Long programId = programOrderCreateDto.getProgramId();
        Long userId = programOrderCreateDto.getUserId();
        ProgramVo programVo = getOrderProgramVo(programOrderCreateDto);
        OrderCreateDto orderCreateDto = new OrderCreateDto();
        orderCreateDto.setOrderNumber(orderNumber);
        orderCreateDto.setProgramId(programId);
        orderCreateDto.setScreeningId(programOrderCreateDto.getScreeningId());
        orderCreateDto.setProgramItemPicture(programVo.getItemPicture());
        orderCreateDto.setUserId(userId);
        orderCreateDto.setProgramTitle(programVo.getTitle());
        orderCreateDto.setProgramPlace(programVo.getPlace());
        orderCreateDto.setProgramShowTime(programVo.getShowTime());
        orderCreateDto.setProgramPermitChooseSeat(programVo.getPermitChooseSeat());
        fillMovieOrderSnapshot(programOrderCreateDto, orderCreateDto);
        BigDecimal databaseOrderPrice =
                purchaseSeatList.stream().map(PurchaseSeat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        orderCreateDto.setOrderPrice(databaseOrderPrice);
        orderCreateDto.setCreateOrderTime(DateUtils.now());
        orderCreateDto.setOrderVersion(orderVersion);

        List<OrderTicketUserCreateDto> orderTicketUserCreateDtoList = new ArrayList<>();
        for (PurchaseSeat purchaseSeat : purchaseSeatList) {
            OrderTicketUserCreateDto orderTicketUserCreateDto = new OrderTicketUserCreateDto();
            orderTicketUserCreateDto.setOrderNumber(orderCreateDto.getOrderNumber());
            orderTicketUserCreateDto.setProgramId(programId);
            orderTicketUserCreateDto.setScreeningId(programOrderCreateDto.getScreeningId());
            orderTicketUserCreateDto.setUserId(userId);
            orderTicketUserCreateDto.setTicketUserId(purchaseSeat.getTicketUserId());
            orderTicketUserCreateDto.setSeatId(purchaseSeat.getId());
            orderTicketUserCreateDto.setSeatInfo(purchaseSeat.getRowCode()+"排"+purchaseSeat.getColCode()+"列");
            orderTicketUserCreateDto.setTicketCategoryId(purchaseSeat.getTicketCategoryId());
            orderTicketUserCreateDto.setOrderPrice(purchaseSeat.getPrice());
            orderTicketUserCreateDto.setCreateOrderTime(DateUtils.now());
            orderTicketUserCreateDtoList.add(orderTicketUserCreateDto);
        }
        orderCreateDto.setOrderTicketUserCreateDtoList(orderTicketUserCreateDtoList);
        return orderCreateDto;
    }

    private String createOrderByRpc(OrderCreateDto orderCreateDto,List<SeatVo> purchaseSeatList){
        ApiResponse<String> createOrderResponse = orderClient.create(orderCreateDto);
        if (!Objects.equals(createOrderResponse.getCode(), BaseCode.SUCCESS.getCode())) {
            log.error("创建订单失败 需人工处理 orderCreateDto : {}",JSON.toJSONString(orderCreateDto));
            updateProgramCacheDataResolution(orderCreateDto.getProgramId(), orderCreateDto.getScreeningId(),
                    purchaseSeatList,OrderStatus.CANCEL);
            throw new TidesFrameException(createOrderResponse);
        }
        return createOrderResponse.getData();
    }

    private String createOrderByMq(OrderCreateMq orderCreateMq,List<PurchaseSeat> purchaseSeatList){
        CreateOrderMqDomain createOrderMqDomain = new CreateOrderMqDomain();
        CountDownLatch latch = new CountDownLatch(1);
        createOrderMqDomain.orderNumber = String.valueOf(orderCreateMq.getOrderNumber());
        createOrderSend.sendMessage(JSON.toJSONString(orderCreateMq),sendResult -> {
            log.info("创建订单kafka发送消息成功 topic : {}",sendResult.getRecordMetadata().topic());
            latch.countDown();
        },ex -> {
            log.error("创建订单kafka发送消息失败 error",ex);
            List<SeatVo> purchaseSeatVoList = purchaseSeatList.stream().map(purchaseSeat -> {
                SeatVo seatVo = new SeatVo();
                BeanUtils.copyProperties(purchaseSeat,seatVo);
                return seatVo;
            }).collect(Collectors.toList());
            updateProgramCacheDataResolution(orderCreateMq.getProgramId(), orderCreateMq.getScreeningId(),
                    purchaseSeatVoList,OrderStatus.CANCEL);
            createOrderMqDomain.tidesFrameException = new TidesFrameException(ex);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("createOrderByMq InterruptedException",e);
            throw new TidesFrameException(e);
        }
        if (Objects.nonNull(createOrderMqDomain.tidesFrameException)) {
            throw createOrderMqDomain.tidesFrameException;
        }
        return createOrderMqDomain.orderNumber;
    }

    private void updateProgramCacheDataResolution(ProgramOrderCreateDto programOrderCreateDto,
                                                  List<SeatVo> seatVoList,
                                                  OrderStatus orderStatus) {
        updateProgramCacheDataResolution(programOrderCreateDto.getProgramId(), programOrderCreateDto.getScreeningId(),
                seatVoList, orderStatus);
    }

    private void updateProgramCacheDataResolution(Long programId, Long screeningId,
                                                  List<SeatVo> seatVoList,
                                                  OrderStatus orderStatus){
        if (!(Objects.equals(orderStatus.getCode(), OrderStatus.NO_PAY.getCode()) ||
                Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode()))) {
            throw new TidesFrameException(BaseCode.OPERATE_ORDER_STATUS_NOT_PERMIT);
        }
        Long inventoryId = Objects.nonNull(screeningId) ? screeningId : programId;
        RedisKeyManage ticketRemainNumberKey = Objects.nonNull(screeningId) ?
                RedisKeyManage.MOVIE_SCREENING_TICKET_REMAIN_NUMBER_HASH_RESOLUTION :
                RedisKeyManage.PROGRAM_TICKET_REMAIN_NUMBER_HASH_RESOLUTION;
        RedisKeyManage seatNoSoldKey = Objects.nonNull(screeningId) ?
                RedisKeyManage.MOVIE_SCREENING_SEAT_NO_SOLD_RESOLUTION_HASH :
                RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH;
        RedisKeyManage seatLockKey = Objects.nonNull(screeningId) ?
                RedisKeyManage.MOVIE_SCREENING_SEAT_LOCK_RESOLUTION_HASH :
                RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH;
        List<String> keys = new ArrayList<>();
        keys.add(RedisKeyBuild.createRedisKey(ticketRemainNumberKey, inventoryId, "slot").getRelKey());

        String[] data = new String[3];
        Map<Long, Long> ticketCategoryCountMap =
                seatVoList.stream().collect(Collectors.groupingBy(SeatVo::getTicketCategoryId, Collectors.counting()));
        JSONArray jsonArray = new JSONArray();
        ticketCategoryCountMap.forEach((k,v) -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("programTicketRemainNumberHashKey",RedisKeyBuild.createRedisKey(
                    ticketRemainNumberKey, inventoryId, k).getRelKey());
            jsonObject.put("ticketCategoryId",String.valueOf(k));
            if (Objects.equals(orderStatus.getCode(), OrderStatus.NO_PAY.getCode())) {
                jsonObject.put("count","-" + v);
            } else if (Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode())) {
                jsonObject.put("count",v);
            }
            jsonArray.add(jsonObject);
        });
        Map<Long, List<SeatVo>> seatVoMap =
                seatVoList.stream().collect(Collectors.groupingBy(SeatVo::getTicketCategoryId));
        JSONArray delSeatIdjsonArray = new JSONArray();
        JSONArray addSeatDatajsonArray = new JSONArray();
        seatVoMap.forEach((k,v) -> {
            JSONObject delSeatIdjsonObject = new JSONObject();
            JSONObject seatDatajsonObject = new JSONObject();
            String seatHashKeyDel = "";
            String seatHashKeyAdd = "";
            if (Objects.equals(orderStatus.getCode(), OrderStatus.NO_PAY.getCode())) {
                seatHashKeyDel = (RedisKeyBuild.createRedisKey(seatNoSoldKey, inventoryId, k).getRelKey());
                seatHashKeyAdd = (RedisKeyBuild.createRedisKey(seatLockKey, inventoryId, k).getRelKey());
                for (SeatVo seatVo : v) {
                    seatVo.setSellStatus(SellStatus.LOCK.getCode());
                }
            } else if (Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode())) {
                seatHashKeyDel = (RedisKeyBuild.createRedisKey(seatLockKey, inventoryId, k).getRelKey());
                seatHashKeyAdd = (RedisKeyBuild.createRedisKey(seatNoSoldKey, inventoryId, k).getRelKey());
                for (SeatVo seatVo : v) {
                    seatVo.setSellStatus(SellStatus.NO_SOLD.getCode());
                }
            }
            delSeatIdjsonObject.put("seatHashKeyDel",seatHashKeyDel);
            delSeatIdjsonObject.put("seatIdList",v.stream().map(SeatVo::getId).map(String::valueOf).collect(Collectors.toList()));
            delSeatIdjsonArray.add(delSeatIdjsonObject);
            seatDatajsonObject.put("seatHashKeyAdd",seatHashKeyAdd);
            List<String> seatDataList = new ArrayList<>();
            for (SeatVo seatVo : v) {
                seatDataList.add(String.valueOf(seatVo.getId()));
                seatDataList.add(JSON.toJSONString(seatVo));
            }
            seatDatajsonObject.put("seatDataList",seatDataList);
            addSeatDatajsonArray.add(seatDatajsonObject);
        });

        data[0] = JSON.toJSONString(jsonArray);
        data[1] = JSON.toJSONString(delSeatIdjsonArray);
        data[2] = JSON.toJSONString(addSeatDatajsonArray);
        programCacheResolutionOperate.programCacheOperate(keys,data);
    }

    private boolean isMovieOrder(ProgramOrderCreateDto programOrderCreateDto) {
        return Objects.nonNull(programOrderCreateDto.getScreeningId());
    }

    private Long inventoryId(ProgramOrderCreateDto programOrderCreateDto) {
        return isMovieOrder(programOrderCreateDto) ? programOrderCreateDto.getScreeningId() :
                programOrderCreateDto.getProgramId();
    }

    private Date getShowTime(ProgramOrderCreateDto programOrderCreateDto) {
        if (isMovieOrder(programOrderCreateDto)) {
            return movieService.selectSellingScreening(programOrderCreateDto.getScreeningId()).getShowTime();
        }
        ProgramShowTime programShowTime =
                programShowTimeService.selectProgramShowTimeByProgramIdMultipleCache(programOrderCreateDto.getProgramId());
        return programShowTime.getShowTime();
    }

    private List<SeatVo> selectSeatResolution(ProgramOrderCreateDto programOrderCreateDto, Long ticketCategoryId,
                                              Long expireTime, TimeUnit timeUnit) {
        if (isMovieOrder(programOrderCreateDto)) {
            return movieService.selectSeatResolution(programOrderCreateDto.getScreeningId(), ticketCategoryId,
                    expireTime, timeUnit);
        }
        return seatService.selectSeatResolution(programOrderCreateDto.getProgramId(), ticketCategoryId,
                expireTime, timeUnit);
    }

    private Map<String, Long> getRedisRemainNumberResolution(ProgramOrderCreateDto programOrderCreateDto,
                                                             Long ticketCategoryId) {
        if (isMovieOrder(programOrderCreateDto)) {
            return movieService.getRemainNumberResolution(programOrderCreateDto.getScreeningId(), ticketCategoryId);
        }
        return ticketCategoryService.getRedisRemainNumberResolution(programOrderCreateDto.getProgramId(),
                ticketCategoryId);
    }

    private String ticketRemainNumberHashKey(ProgramOrderCreateDto programOrderCreateDto, Long ticketCategoryId) {
        return RedisKeyBuild.createRedisKey(ticketRemainNumberKey(programOrderCreateDto),
                inventoryId(programOrderCreateDto), ticketCategoryId).getRelKey();
    }

    private String seatNoSoldHashKey(ProgramOrderCreateDto programOrderCreateDto, Long ticketCategoryId) {
        return RedisKeyBuild.createRedisKey(seatNoSoldKey(programOrderCreateDto),
                inventoryId(programOrderCreateDto), ticketCategoryId).getRelKey();
    }

    private RedisKeyManage ticketRemainNumberKey(ProgramOrderCreateDto programOrderCreateDto) {
        return isMovieOrder(programOrderCreateDto) ?
                RedisKeyManage.MOVIE_SCREENING_TICKET_REMAIN_NUMBER_HASH_RESOLUTION :
                RedisKeyManage.PROGRAM_TICKET_REMAIN_NUMBER_HASH_RESOLUTION;
    }

    private RedisKeyManage seatNoSoldKey(ProgramOrderCreateDto programOrderCreateDto) {
        return isMovieOrder(programOrderCreateDto) ?
                RedisKeyManage.MOVIE_SCREENING_SEAT_NO_SOLD_RESOLUTION_HASH :
                RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH;
    }

    private RedisKeyManage seatLockKey(ProgramOrderCreateDto programOrderCreateDto) {
        return isMovieOrder(programOrderCreateDto) ?
                RedisKeyManage.MOVIE_SCREENING_SEAT_LOCK_RESOLUTION_HASH :
                RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH;
    }

    private RedisKeyManage recordKey(ProgramOrderCreateDto programOrderCreateDto) {
        return isMovieOrder(programOrderCreateDto) ?
                RedisKeyManage.MOVIE_SCREENING_RECORD :
                RedisKeyManage.PROGRAM_RECORD;
    }

    private void fillMovieOrderSnapshot(ProgramOrderCreateDto programOrderCreateDto, OrderCreateDto orderCreateDto) {
        if (!isMovieOrder(programOrderCreateDto)) {
            return;
        }
        MovieScreeningVo movieScreeningVo = movieService.screeningInfo(programOrderCreateDto.getScreeningId());
        orderCreateDto.setProgramShowTime(movieScreeningVo.getShowTime());
        String place = Stream.of(movieScreeningVo.getCinemaName(), movieScreeningVo.getHallName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        if (!place.isEmpty()) {
            orderCreateDto.setProgramPlace(place);
        }
    }

    private ProgramVo getOrderProgramVo(ProgramOrderCreateDto programOrderCreateDto) {
        if (!isMovieOrder(programOrderCreateDto)) {
            return programService.simpleGetProgramAndShowMultipleCache(programOrderCreateDto.getProgramId());
        }
        MovieScreening movieScreening = movieService.selectSellingScreening(programOrderCreateDto.getScreeningId());
        if (!Objects.equals(movieScreening.getProgramId(), programOrderCreateDto.getProgramId())) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        ProgramVo programVo = programService.simpleGetByIdMultipleCache(programOrderCreateDto.getProgramId());
        if (Objects.isNull(programVo)) {
            Long expireTime = Math.max(1L, DateUtils.countBetweenSecond(DateUtils.now(), movieScreening.getShowTime()));
            programVo = programService.getById(programOrderCreateDto.getProgramId(), expireTime, TimeUnit.SECONDS);
        }
        ProgramVo orderProgramVo = new ProgramVo();
        BeanUtils.copyProperties(programVo, orderProgramVo);
        orderProgramVo.setShowTime(movieScreening.getShowTime());
        orderProgramVo.setShowDayTime(movieScreening.getShowDayTime());
        orderProgramVo.setShowWeekTime(movieScreening.getShowWeekTime());
        return orderProgramVo;
    }

    private static class CreateOrderIdempotent {
        private final Long orderNumber;

        private final boolean exists;

        private final RedisKeyBuild redisKeyBuild;

        private CreateOrderIdempotent(Long orderNumber, boolean exists, RedisKeyBuild redisKeyBuild) {
            this.orderNumber = orderNumber;
            this.exists = exists;
            this.redisKeyBuild = redisKeyBuild;
        }
    }
}
