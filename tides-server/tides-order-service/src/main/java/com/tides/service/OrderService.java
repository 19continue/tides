package com.tides.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tides.client.PayClient;
import com.tides.client.ProgramClient;
import com.tides.client.UserClient;
import com.tides.common.ApiResponse;
import com.tides.core.RedisKeyManage;
import com.tides.domain.DiscardOrder;
import com.tides.domain.OrderCreateDomain;
import com.tides.domain.OrderCreateMq;
import com.tides.domain.SeatIdAndTicketUserIdDomain;
import com.tides.dto.AccountOrderCountDto;
import com.tides.dto.MovieOrderSalesCountDto;
import com.tides.dto.NotifyDto;
import com.tides.dto.OrderCancelDto;
import com.tides.dto.OrderCreateDto;
import com.tides.dto.OrderGetDto;
import com.tides.dto.OrderListDto;
import com.tides.dto.OrderPayCheckDto;
import com.tides.dto.OrderPayDto;
import com.tides.dto.OrderRefundApplyDto;
import com.tides.dto.OrderSimpleListDto;
import com.tides.dto.OrderSimulatePayDto;
import com.tides.dto.OrderTicketVerifyDto;
import com.tides.dto.OrderTicketUserCreateDto;
import com.tides.dto.PayDto;
import com.tides.dto.ProgramOperateDataDto;
import com.tides.dto.ReduceRemainNumberDto;
import com.tides.dto.RefundDto;
import com.tides.dto.TicketCategoryCountDto;
import com.tides.dto.TradeCheckDto;
import com.tides.dto.UserGetAndTicketUserListDto;
import com.tides.entity.Order;
import com.tides.entity.OrderProgram;
import com.tides.entity.OrderTicketUser;
import com.tides.entity.OrderTicketUserAggregate;
import com.tides.entity.OrderTicketUserRecord;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.enums.DiscardOrderReason;
import com.tides.enums.OrderStatus;
import com.tides.enums.PayBillStatus;
import com.tides.enums.PayChannel;
import com.tides.enums.ProgramOrderVersion;
import com.tides.enums.RecordType;
import com.tides.enums.SellStatus;
import com.tides.enums.TicketStatus;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.OrderMapper;
import com.tides.mapper.OrderProgramMapper;
import com.tides.mapper.OrderTicketUserMapper;
import com.tides.mapper.OrderTicketUserRecordMapper;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.repeatexecutelimit.annotion.RepeatExecuteLimit;
import com.tides.request.CustomizeRequestWrapper;
import com.tides.service.delaysend.DelayOperateProgramDataSend;
import com.tides.service.properties.OrderProperties;
import com.tides.servicelock.LockType;
import com.tides.servicelock.annotion.ServiceLock;
import com.tides.util.DateUtils;
import com.tides.util.ServiceLockTool;
import com.tides.util.StringUtil;
import com.tides.vo.AccountOrderCountVo;
import com.tides.vo.MovieOrderSalesCountVo;
import com.tides.vo.NotifyVo;
import com.tides.vo.OrderGetVo;
import com.tides.vo.OrderListVo;
import com.tides.vo.OrderPayCheckVo;
import com.tides.vo.OrderTicketInfoVo;
import com.tides.vo.OrderTicketUserVo;
import com.tides.vo.SeatVo;
import com.tides.vo.TicketUserInfoVo;
import com.tides.vo.TicketUserVo;
import com.tides.vo.TradeCheckVo;
import com.tides.vo.UserAndTicketUserInfoVo;
import com.tides.vo.UserGetAndTicketUserListVo;
import com.tides.vo.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tides.constant.Constant.ALIPAY_NOTIFY_SUCCESS_RESULT;
import static com.tides.constant.Constant.GLIDE_LINE;
import static com.tides.core.DistributedLockConstants.UPDATE_ORDER_STATUS_LOCK;
import static com.tides.core.RepeatExecuteLimitConstants.CANCEL_PROGRAM_ORDER;
import static com.tides.core.RepeatExecuteLimitConstants.CREATE_PROGRAM_ORDER_MQ;

/**
 * @description: 订单 service
 * @author: 19continue
 **/
@Slf4j
@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    private static final String INVENTORY_SOURCE_ORDER_CREATE_LOCK = "ORDER_CREATE_LOCK";

    private static final String INVENTORY_SOURCE_ORDER_PAY = "ORDER_PAY";

    private static final String INVENTORY_SOURCE_ORDER_CANCEL = "ORDER_CANCEL";

    private static final String INVENTORY_SOURCE_ORDER_REFUND = "ORDER_REFUND";
    
    @Autowired
    private UidGenerator uidGenerator;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private OrderTicketUserMapper orderTicketUserMapper;
    
    @Autowired
    private OrderTicketUserService orderTicketUserService;
    
    @Autowired
    private OrderTicketUserRecordService orderTicketUserRecordService;
    
    @Autowired
    private OrderProgramCacheResolutionOperate orderProgramCacheResolutionOperate;
    
    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private PayClient payClient;
    
    @Autowired
    private UserClient userClient;
    
    @Autowired
    private OrderProperties orderProperties;
    
    @Lazy
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ServiceLockTool serviceLockTool;
    
    @Autowired
    private ProgramClient programClient;
    
    @Autowired
    private OrderTicketUserRecordMapper orderTicketUserRecordMapper;
    
    @Autowired
    private OrderProgramMapper orderProgramMapper;
    
    @Autowired
    private DelayOperateProgramDataSend delayOperateProgramDataSend;

    @Value("${ticket-rush-simulator.pay-token:tides-demo-local}")
    private String simulatorPayToken;

    @Transactional(rollbackFor = Exception.class)
    public String create(OrderCreateDto orderCreateDto) {
        OrderCreateDomain orderCreateDomain = new OrderCreateDomain();
        BeanUtils.copyProperties(orderCreateDto, orderCreateDomain);
        return doCreate(orderCreateDomain);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public String createByMq(OrderCreateMq orderCreateMq) {
        OrderCreateDomain orderCreateDomain = new OrderCreateDomain();
        BeanUtils.copyProperties(orderCreateMq, orderCreateDomain);
        return doCreate(orderCreateDomain);
    }

    @Transactional(rollbackFor = Exception.class)
    public String doCreate(OrderCreateDomain orderCreateDomain) {
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper =
                Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNumber, orderCreateDomain.getOrderNumber());
        //如果订单存在了，那么直接拒绝
        Order oldOrder = orderMapper.selectOne(orderLambdaQueryWrapper);
        if (Objects.nonNull(oldOrder)) {
            throw new TidesFrameException(BaseCode.ORDER_EXIST);
        }
        Order order = new Order();
        BeanUtil.copyProperties(orderCreateDomain,order);
        order.setId(uidGenerator.getUid());
        order.setDistributionMode("电子票");
        order.setTakeTicketMode("请使用购票人身份证直接入场");
        //购票人订单对象
        List<OrderTicketUser> orderTicketUserList = new ArrayList<>();
        //购票人订单记录对象
        List<OrderTicketUserRecord> orderTicketUserRecordList = new ArrayList<>();
        for (OrderTicketUserCreateDto orderTicketUserCreateDto : orderCreateDomain.getOrderTicketUserCreateDtoList()) {
            OrderTicketUser orderTicketUser = new OrderTicketUser();
            BeanUtil.copyProperties(orderTicketUserCreateDto,orderTicketUser);
            orderTicketUser.setId(uidGenerator.getUid());
            orderTicketUser.setTicketStatus(TicketStatus.WAIT_ISSUE.getCode());
            orderTicketUserList.add(orderTicketUser);

            OrderTicketUserRecord orderTicketUserRecord = new OrderTicketUserRecord();
            BeanUtil.copyProperties(orderTicketUserCreateDto,orderTicketUserRecord);
            orderTicketUserRecord.setIdentifierId(orderCreateDomain.getIdentifierId());
            orderTicketUserRecord.setTicketUserOrderId(orderTicketUser.getId());
            orderTicketUserRecord.setRecordTypeCode(RecordType.REDUCE.getCode());
            orderTicketUserRecord.setRecordTypeValue(RecordType.REDUCE.getValue());
            orderTicketUserRecordList.add(orderTicketUserRecord);
        }
        //插入主订单
        orderMapper.insert(order);
        //插入购票人订单
        orderTicketUserService.saveBatch(orderTicketUserList);
        //插入购票人订单记录
        orderTicketUserRecordService.saveBatch(orderTicketUserRecordList);
        //插入订单节目
        OrderProgram orderProgram = new OrderProgram();
        orderProgram.setId(uidGenerator.getUid());
        orderProgram.setProgramId(order.getProgramId());
        orderProgram.setScreeningId(order.getScreeningId());
        orderProgram.setOrderNumber(order.getOrderNumber());
        orderProgram.setIdentifierId(order.getIdentifierId());
        orderProgramMapper.insert(orderProgram);
        //用户下此节目的订单数量加1操作
        redisCache.incrBy(RedisKeyBuild.createRedisKey(
                RedisKeyManage.ACCOUNT_ORDER_COUNT,orderCreateDomain.getUserId(),
                orderCreateDomain.getProgramId()),orderCreateDomain.getOrderTicketUserCreateDtoList().size());
        return String.valueOf(order.getOrderNumber());
    }
    
    /**
     * 订单取消，以订单编号加锁
     * */
    @RepeatExecuteLimit(name = CANCEL_PROGRAM_ORDER,keys = {"#orderCancelDto.orderNumber"})
    @ServiceLock(name = UPDATE_ORDER_STATUS_LOCK,keys = {"#orderCancelDto.orderNumber"})
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(OrderCancelDto orderCancelDto){
        updateOrderRelatedData(orderCancelDto.getOrderNumber(),OrderStatus.CANCEL);
        return true;
    }
    
    public String pay(OrderPayDto orderPayDto) {
        Long orderNumber = orderPayDto.getOrderNumber();
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper =
                Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNumber, orderNumber);
        Order order = orderMapper.selectOne(orderLambdaQueryWrapper);
        if (Objects.isNull(order)) {
            throw new TidesFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        if (Objects.equals(order.getOrderStatus(), OrderStatus.CANCEL.getCode())) {
            throw new TidesFrameException(BaseCode.ORDER_CANCEL);
        }
        if (Objects.equals(order.getOrderStatus(), OrderStatus.PAY.getCode())) {
            throw new TidesFrameException(BaseCode.ORDER_PAY);
        }
        if (Objects.equals(order.getOrderStatus(), OrderStatus.REFUND.getCode())) {
            throw new TidesFrameException(BaseCode.ORDER_REFUND);
        }
        if (orderPayDto.getPrice().compareTo(order.getOrderPrice()) != 0) {
            throw new TidesFrameException(BaseCode.PAY_PRICE_NOT_EQUAL_ORDER_PRICE);
        }
        PayDto payDto = getPayDto(orderPayDto, orderNumber);
        ApiResponse<String> payResponse = payClient.commonPay(payDto);
        if (!Objects.equals(payResponse.getCode(), BaseCode.SUCCESS.getCode())) {
            throw new TidesFrameException(payResponse);
        }
        return payResponse.getData();
    }

    /**
     * 演示压测模拟支付成功。
     * 该方法不访问支付宝或支付服务，只复用真实支付成功后的订单、电子票和库存推进逻辑。
     */
    @ServiceLock(name = UPDATE_ORDER_STATUS_LOCK, keys = {"#orderSimulatePayDto.orderNumber"})
    @Transactional(rollbackFor = Exception.class)
    public boolean simulatePay(String requestToken, OrderSimulatePayDto orderSimulatePayDto) {
        if (!Objects.equals(simulatorPayToken, requestToken)) {
            throw new TidesFrameException(BaseCode.API_CALL_PASSWORD_ERROR);
        }
        Long orderNumber = orderSimulatePayDto.getOrderNumber();
        Order order = orderMapper.selectOne(Wrappers.lambdaQuery(Order.class)
                .eq(Order::getOrderNumber, orderNumber));
        if (Objects.isNull(order)) {
            throw new TidesFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        if (Objects.equals(order.getOrderStatus(), OrderStatus.PAY.getCode())) {
            return true;
        }
        updateOrderRelatedData(orderNumber, OrderStatus.PAY);
        return true;
    }
    
    private PayDto getPayDto(OrderPayDto orderPayDto, Long orderNumber) {
        PayDto payDto = new PayDto();
        payDto.setOrderNumber(String.valueOf(orderNumber));
        payDto.setPayBillType(orderPayDto.getPayBillType());
        payDto.setSubject(orderPayDto.getSubject());
        payDto.setChannel(orderPayDto.getChannel());
        payDto.setPlatform(orderPayDto.getPlatform());
        payDto.setPrice(orderPayDto.getPrice());
        payDto.setNotifyUrl(orderProperties.getOrderPayNotifyUrl());
        payDto.setReturnUrl(orderProperties.getOrderPayReturnUrl());
        return payDto;
    }
    
    /**
     * 支付后订单检查，以订单编号加锁，防止多次更新
     * */
    @ServiceLock(name = UPDATE_ORDER_STATUS_LOCK,keys = {"#orderPayCheckDto.orderNumber"})
    public OrderPayCheckVo payCheck(OrderPayCheckDto orderPayCheckDto){
        OrderPayCheckVo orderPayCheckVo = new OrderPayCheckVo();
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper =
                Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNumber, orderPayCheckDto.getOrderNumber());
        Order order = orderMapper.selectOne(orderLambdaQueryWrapper);
        if (Objects.isNull(order)) {
            throw new TidesFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        BeanUtil.copyProperties(order,orderPayCheckVo);
        if (Objects.equals(order.getOrderStatus(), OrderStatus.CANCEL.getCode())) {
            RefundDto refundDto = new RefundDto();
            refundDto.setOrderNumber(String.valueOf(order.getOrderNumber()));
            refundDto.setAmount(order.getOrderPrice());
            refundDto.setChannel("alipay");
            refundDto.setReason("延迟订单关闭");
            ApiResponse<String> response = payClient.refund(refundDto);
            if (response.getCode().equals(BaseCode.SUCCESS.getCode())) {
                Order updateOrder = new Order();
                updateOrder.setEditTime(DateUtils.now());
                updateOrder.setOrderStatus(OrderStatus.REFUND.getCode());
                orderMapper.update(updateOrder,Wrappers.lambdaUpdate(Order.class).eq(Order::getOrderNumber, order.getOrderNumber()));
            }else {
                log.error("pay服务退款失败 dto : {} response : {}",JSON.toJSONString(refundDto),JSON.toJSONString(response));
            }
            orderPayCheckVo.setOrderStatus(OrderStatus.REFUND.getCode());
            orderPayCheckVo.setCancelOrderTime(DateUtils.now());
            return orderPayCheckVo;
        }
        
        TradeCheckDto tradeCheckDto = new TradeCheckDto();
        tradeCheckDto.setOutTradeNo(String.valueOf(orderPayCheckDto.getOrderNumber()));
        tradeCheckDto.setChannel(Optional.ofNullable(PayChannel.getRc(orderPayCheckDto.getPayChannelType()))
                .map(PayChannel::getValue).orElseThrow(() -> new TidesFrameException(BaseCode.PAY_CHANNEL_NOT_EXIST)));
        ApiResponse<TradeCheckVo> tradeCheckVoApiResponse = payClient.tradeCheck(tradeCheckDto);
        if (!Objects.equals(tradeCheckVoApiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
            throw new TidesFrameException(tradeCheckVoApiResponse);
        }
        TradeCheckVo tradeCheckVo = Optional.ofNullable(tradeCheckVoApiResponse.getData())
                .orElseThrow(() -> new TidesFrameException(BaseCode.PAY_BILL_NOT_EXIST));
        if (tradeCheckVo.isSuccess()) {
            Integer payBillStatus = tradeCheckVo.getPayBillStatus();
            Integer orderStatus = order.getOrderStatus();
            if (!Objects.equals(orderStatus, payBillStatus)) {
                orderPayCheckVo.setOrderStatus(payBillStatus);
                try {
                    if (Objects.equals(payBillStatus, PayBillStatus.PAY.getCode())) {
                        orderPayCheckVo.setPayOrderTime(DateUtils.now());
                        orderService.updateOrderRelatedData(order.getOrderNumber(),OrderStatus.PAY);
                    }else if (Objects.equals(payBillStatus, PayBillStatus.CANCEL.getCode())) {
                        orderPayCheckVo.setCancelOrderTime(DateUtils.now());
                        orderService.updateOrderRelatedData(order.getOrderNumber(),OrderStatus.CANCEL);
                    }
                }catch (Exception e) {
                    log.warn("updateOrderRelatedData warn message",e);
                }
            }
        }else {
            throw new TidesFrameException(BaseCode.PAY_TRADE_CHECK_ERROR);
        }
        return orderPayCheckVo;
    }
    
    
    public String alipayNotify(HttpServletRequest request){
        
        Map<String, String> params = new HashMap<>(256);
        if (request instanceof final CustomizeRequestWrapper customizeRequestWrapper) {
            String requestBody = customizeRequestWrapper.getRequestBody();
            params = StringUtil.convertQueryStringToMap(requestBody);
        }
        log.info("收到支付宝回调通知 params : {}",JSON.toJSONString(params));
        String outTradeNo = params.get("out_trade_no");
        if (StringUtil.isEmpty(outTradeNo)) {
            return "failure";
        }
        
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, UPDATE_ORDER_STATUS_LOCK,
                new String[]{outTradeNo});
        lock.lock();
        try {
            Order order = orderMapper.selectOne(Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNumber, Long.parseLong(outTradeNo)));
            if (Objects.isNull(order)) {
                throw new TidesFrameException(BaseCode.ORDER_NOT_EXIST);
            }
            if (Objects.equals(order.getOrderStatus(), OrderStatus.CANCEL.getCode())) {
                RefundDto refundDto = new RefundDto();
                refundDto.setOrderNumber(outTradeNo);
                refundDto.setAmount(order.getOrderPrice());
                refundDto.setChannel("alipay");
                refundDto.setReason("延迟订单关闭");
                ApiResponse<String> response = payClient.refund(refundDto);
                if (response.getCode().equals(BaseCode.SUCCESS.getCode())) {
                    Order updateOrder = new Order();
                    updateOrder.setEditTime(DateUtils.now());
                    updateOrder.setOrderStatus(OrderStatus.REFUND.getCode());
                    orderMapper.update(updateOrder,Wrappers.lambdaUpdate(Order.class).eq(Order::getOrderNumber, outTradeNo));
                }else {
                    log.error("pay服务退款失败 dto : {} response : {}",JSON.toJSONString(refundDto),JSON.toJSONString(response));
                }
                return ALIPAY_NOTIFY_SUCCESS_RESULT;
            }
          
            
            NotifyDto notifyDto = new NotifyDto();
            notifyDto.setChannel(PayChannel.ALIPAY.getValue());
            notifyDto.setParams(params);
            ApiResponse<NotifyVo> notifyResponse = payClient.notify(notifyDto);
            if (!Objects.equals(notifyResponse.getCode(), BaseCode.SUCCESS.getCode())) {
                throw new TidesFrameException(notifyResponse);
            }
            if (ALIPAY_NOTIFY_SUCCESS_RESULT.equals(notifyResponse.getData().getPayResult())) {
                try {
                    orderService.updateOrderRelatedData(Long.parseLong(notifyResponse.getData().getOutTradeNo())
                            ,OrderStatus.PAY);
                }catch (Exception e) {
                    log.warn("updateOrderRelatedData warn message",e);
                }
            }
            return notifyResponse.getData().getPayResult();
        }finally {
            lock.unlock();
        }
        
    }
    
    /**
     * 更新订单和购票人订单状态以及操作缓存数据
     * */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderRelatedData(Long orderNumber,OrderStatus orderStatus){
        //如果不是取消或者支付操作，则直接抛出异常提示
        if (!(Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode()) ||
                Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode()))) {
            throw new TidesFrameException(  BaseCode.OPERATE_ORDER_STATUS_NOT_PERMIT);
        }
        //查询订单
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper =
                Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNumber, orderNumber);
        Order order = orderMapper.selectOne(orderLambdaQueryWrapper);
        //检查订单的状态 已取消、已支付、已退单的状态不再执行
        checkOrderStatus(order);
        //查询该订单下的购票人订单列表
        LambdaQueryWrapper<OrderTicketUser> orderTicketUserLambdaQueryWrapper =
                Wrappers.lambdaQuery(OrderTicketUser.class).eq(OrderTicketUser::getOrderNumber, order.getOrderNumber());
        List<OrderTicketUser> orderTicketUserList = orderTicketUserMapper.selectList(orderTicketUserLambdaQueryWrapper);
        if (CollectionUtil.isEmpty(orderTicketUserList)) {
            throw new TidesFrameException(BaseCode.TICKET_USER_ORDER_NOT_EXIST);
        }
        //将订单更新为取消或者支付状态
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setOrderStatus(orderStatus.getCode());
        //将购票人订单更新为取消或者支付状态
        OrderTicketUser updateOrderTicketUser = new OrderTicketUser();
        updateOrderTicketUser.setOrderStatus(orderStatus.getCode());

        //如果是支付的话，那么记录的类型就是改变状态
        //记录类型code
        Integer recordTypeCode = RecordType.CHANGE_STATUS.getCode();
        //记录类型值
        String recordTypeValue = RecordType.CHANGE_STATUS.getValue();
        //支付状态的操作
        if (Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode())) {
            updateOrder.setPayOrderTime(DateUtils.now());
            updateOrderTicketUser.setPayOrderTime(DateUtils.now());
            updateOrderTicketUser.setTicketStatus(TicketStatus.ISSUED.getCode());
            updateOrderTicketUser.setTicketIssueTime(updateOrder.getPayOrderTime());
        } else if (Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode())) {
            //取消状态的操作
            updateOrder.setCancelOrderTime(DateUtils.now());
            updateOrderTicketUser.setCancelOrderTime(DateUtils.now());
            updateOrderTicketUser.setTicketStatus(TicketStatus.VOID.getCode());
            //如果是取消的话，那么记录的类型就是增加余票
            recordTypeCode = RecordType.INCREASE.getCode();
            recordTypeValue = RecordType.INCREASE.getValue();
        }
        //更新订单
        LambdaUpdateWrapper<Order> orderLambdaUpdateWrapper =
                Wrappers.lambdaUpdate(Order.class).eq(Order::getOrderNumber, order.getOrderNumber());
        int updateOrderResult = orderMapper.update(updateOrder,orderLambdaUpdateWrapper);
        //更新购票人订单
        int updateTicketUserOrderResult = updateOrderTicketUserList(order, orderStatus, orderTicketUserList, updateOrderTicketUser);
        if (updateOrderResult <= 0 || updateTicketUserOrderResult <= 0) {
            throw new TidesFrameException(BaseCode.ORDER_CANAL_ERROR);
        }
        List<SeatIdAndTicketUserIdDomain> seatIdAndTicketUserIdDomainList = new ArrayList<>();
        List<OrderTicketUserRecord> orderTicketUserRecordList = new ArrayList<>();
        for (OrderTicketUser orderTicketUser : orderTicketUserList) {
            //购票人订单记录
            OrderTicketUserRecord orderTicketUserRecord = new OrderTicketUserRecord();
            BeanUtils.copyProperties(orderTicketUser,orderTicketUserRecord);
            orderTicketUserRecord.setId(uidGenerator.getUid());
            orderTicketUserRecord.setIdentifierId(order.getIdentifierId());
            orderTicketUserRecord.setTicketUserOrderId(orderTicketUser.getId());
            orderTicketUserRecord.setRecordTypeCode(recordTypeCode);
            orderTicketUserRecord.setRecordTypeValue(recordTypeValue);
            orderTicketUserRecordList.add(orderTicketUserRecord);
            //购票人订单id和座位id
            seatIdAndTicketUserIdDomainList.add(new SeatIdAndTicketUserIdDomain(orderTicketUser.getSeatId(),
                    orderTicketUser.getTicketUserId()));
        }
        //添加购票人订单记录流水
        orderTicketUserRecordService.saveBatch(orderTicketUserRecordList);
        
        //如果是取消操作，那么把用户下该节目的订单数量要-1
        if (Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode())) {
            redisCache.incrBy(RedisKeyBuild.createRedisKey(
                    RedisKeyManage.ACCOUNT_ORDER_COUNT,order.getUserId(),order.getProgramId()),-updateTicketUserOrderResult);
        }
        Long programId = order.getProgramId();
        //将购票人订单集合转换成map结构，key：票档id value：购票人订单
        Map<Long, List<OrderTicketUser>> orderTicketUserSeatList = 
                orderTicketUserList.stream().collect(Collectors.groupingBy(OrderTicketUser::getTicketCategoryId));
        Map<Long,List<Long>> seatMap = new HashMap<>(orderTicketUserSeatList.size());
        //根据orderTicketUserSeatList得到seatMap
        //seatMap结构 key：票档id  value：座位id集合
        orderTicketUserSeatList.forEach((k,v) -> {
            seatMap.put(k,v.stream().map(OrderTicketUser::getSeatId).collect(Collectors.toList()));
        });
        //更新缓存和节目库相关数据
        updateProgramRelatedDataResolution(programId,order.getScreeningId(),seatMap,orderStatus,order.getIdentifierId(),order.getUserId(),
                seatIdAndTicketUserIdDomainList,order.getOrderVersion(), order.getOrderNumber());
    }

    private int updateOrderTicketUserList(Order order, OrderStatus orderStatus, List<OrderTicketUser> orderTicketUserList,
                                          OrderTicketUser updateOrderTicketUser) {
        if (Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode())) {
            int updateTicketUserOrderResult = 0;
            for (OrderTicketUser orderTicketUser : orderTicketUserList) {
                OrderTicketUser ticketUser = new OrderTicketUser();
                BeanUtils.copyProperties(updateOrderTicketUser, ticketUser);
                ticketUser.setTicketCode(createTicketCode(order.getOrderNumber(), orderTicketUser.getId()));
                updateTicketUserOrderResult += orderTicketUserMapper.update(ticketUser, Wrappers.lambdaUpdate(OrderTicketUser.class)
                        .eq(OrderTicketUser::getOrderNumber, order.getOrderNumber())
                        .eq(OrderTicketUser::getId, orderTicketUser.getId()));
            }
            return updateTicketUserOrderResult;
        }
        LambdaUpdateWrapper<OrderTicketUser> orderTicketUserLambdaUpdateWrapper =
                Wrappers.lambdaUpdate(OrderTicketUser.class).eq(OrderTicketUser::getOrderNumber, order.getOrderNumber());
        return orderTicketUserMapper.update(updateOrderTicketUser, orderTicketUserLambdaUpdateWrapper);
    }

    private String createTicketCode(Long orderNumber, Long orderTicketUserId) {
        return "ET" + orderNumber + GLIDE_LINE + orderTicketUserId;
    }
    
    public void checkOrderStatus(Order order){
        if (Objects.isNull(order)) {
            throw new TidesFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        if (Objects.equals(order.getOrderStatus(), OrderStatus.CANCEL.getCode())) {
            throw new TidesFrameException(BaseCode.ORDER_CANCEL);
        }
        if (Objects.equals(order.getOrderStatus(), OrderStatus.PAY.getCode())) {
            throw new TidesFrameException(BaseCode.ORDER_PAY);
        }
        if (Objects.equals(order.getOrderStatus(), OrderStatus.REFUND.getCode())) {
            throw new TidesFrameException(BaseCode.ORDER_REFUND);
        }
    }
    
    public void updateProgramRelatedDataResolution(Long programId, Long screeningId,
                                                   Map<Long,List<Long>> seatMap, OrderStatus orderStatus,Long identifierId, Long userId,
                                                   List<SeatIdAndTicketUserIdDomain> seatIdAndTicketUserIdDomainList,
                                                   Integer orderVersion, Long orderNumber){
        Long inventoryId = Objects.nonNull(screeningId) ? screeningId : programId;
        RedisKeyManage seatLockKey = Objects.nonNull(screeningId) ?
                RedisKeyManage.MOVIE_SCREENING_SEAT_LOCK_RESOLUTION_HASH :
                RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH;
        RedisKeyManage seatNoSoldKey = Objects.nonNull(screeningId) ?
                RedisKeyManage.MOVIE_SCREENING_SEAT_NO_SOLD_RESOLUTION_HASH :
                RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH;
        RedisKeyManage seatSoldKey = Objects.nonNull(screeningId) ?
                RedisKeyManage.MOVIE_SCREENING_SEAT_SOLD_RESOLUTION_HASH :
                RedisKeyManage.PROGRAM_SEAT_SOLD_RESOLUTION_HASH;
        RedisKeyManage ticketRemainNumberKey = Objects.nonNull(screeningId) ?
                RedisKeyManage.MOVIE_SCREENING_TICKET_REMAIN_NUMBER_HASH_RESOLUTION :
                RedisKeyManage.PROGRAM_TICKET_REMAIN_NUMBER_HASH_RESOLUTION;
        RedisKeyManage recordKey = Objects.nonNull(screeningId) ?
                RedisKeyManage.MOVIE_SCREENING_RECORD :
                RedisKeyManage.PROGRAM_RECORD;
        boolean refund = Objects.equals(orderStatus.getCode(), OrderStatus.REFUND.getCode());
        boolean returnToNoSold = Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode()) || refund;
        RedisKeyManage sourceSeatKey = refund ? seatSoldKey : seatLockKey;
        RedisKeyManage targetSeatKey = returnToNoSold ? seatNoSoldKey : seatSoldKey;
        Integer sourceSellStatus = refund ? SellStatus.SOLD.getCode() : SellStatus.LOCK.getCode();
        Map<Long, List<SeatVo>> seatVoMap = new HashMap<>(seatMap.size());
        seatMap.forEach((k,v) -> {
            seatVoMap.put(k,redisCache.multiGetForHash(
                    RedisKeyBuild.createRedisKey(sourceSeatKey, inventoryId, k),
                    v.stream().map(String::valueOf).collect(Collectors.toList()), SeatVo.class));
        });
        if (CollectionUtil.isEmpty(seatVoMap)) {
            throw new TidesFrameException(BaseCode.LOCK_SEAT_LIST_EMPTY);
        }
        JSONArray jsonArray = new JSONArray();
        JSONArray addSeatDatajsonArray = new JSONArray();
        List<TicketCategoryCountDto> ticketCategoryCountDtoList = new ArrayList<>(seatVoMap.size());
        JSONArray unLockSeatIdjsonArray = new JSONArray();
        List<Long> unLockSeatIdList = new ArrayList<>();
        seatVoMap.forEach((k,v) -> {
            JSONObject unLockSeatIdjsonObject = new JSONObject();
            unLockSeatIdjsonObject.put("programSeatLockHashKey", RedisKeyBuild.createRedisKey(
                    sourceSeatKey, inventoryId, k).getRelKey());
            unLockSeatIdjsonObject.put("unLockSeatIdList",v.stream()
                    .map(SeatVo::getId).map(String::valueOf).collect(Collectors.toList()));
            unLockSeatIdjsonArray.add(unLockSeatIdjsonObject);
            JSONObject seatDatajsonObject = new JSONObject();
            String seatHashKeyAdd = RedisKeyBuild.createRedisKey(
                    targetSeatKey, inventoryId, k).getRelKey();
            if (Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode()) || refund) {
                seatHashKeyAdd = RedisKeyBuild.createRedisKey(
                        seatNoSoldKey, inventoryId, k).getRelKey();
                for (SeatVo seatVo : v) {
                    //座位状态要改成未售卖
                    seatVo.setSellStatus(SellStatus.NO_SOLD.getCode());
                }
            }else if (Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode())) {
                seatHashKeyAdd = RedisKeyBuild.createRedisKey(
                        seatSoldKey, inventoryId, k).getRelKey();
                for (SeatVo seatVo : v) {
                    seatVo.setSellStatus(SellStatus.SOLD.getCode());
                }
            }
            seatDatajsonObject.put("seatHashKeyAdd",seatHashKeyAdd);
            List<String> seatDataList = new ArrayList<>();
            for (SeatVo seatVo : v) {
                seatDataList.add(String.valueOf(seatVo.getId()));
                seatDataList.add(JSON.toJSONString(seatVo));
            }
            seatDatajsonObject.put("seatDataList",seatDataList);
            addSeatDatajsonArray.add(seatDatajsonObject);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("programTicketRemainNumberHashKey",RedisKeyBuild.createRedisKey(
                    ticketRemainNumberKey, inventoryId, k).getRelKey());
            jsonObject.put("ticketCategoryId",String.valueOf(k));
            jsonObject.put("count",v.size());
            jsonArray.add(jsonObject);
            TicketCategoryCountDto ticketCategoryCountDto = new TicketCategoryCountDto();
            ticketCategoryCountDto.setTicketCategoryId(k);
            ticketCategoryCountDto.setCount((long) v.size());
            ticketCategoryCountDtoList.add(ticketCategoryCountDto);
            
            unLockSeatIdList.addAll(v.stream().map(SeatVo::getId).toList());
        });
        List<String> keys = new ArrayList<>();
        keys.add(RedisKeyBuild.getRedisKey(recordKey));
        
        String recordTye = returnToNoSold ? RecordType.INCREASE.getValue() : RecordType.CHANGE_STATUS.getValue();
        Object[] data = new String[9];
        data[0] = JSON.toJSONString(unLockSeatIdjsonArray);
        data[1] = JSON.toJSONString(addSeatDatajsonArray);
        data[2] = JSON.toJSONString(jsonArray);
        data[3] = JSON.toJSONString(seatIdAndTicketUserIdDomainList);
        data[4] = String.valueOf(orderStatus.getCode());
        data[5] = String.valueOf(inventoryId);
        data[6] = recordTye + GLIDE_LINE + identifierId + GLIDE_LINE + userId;
        data[7] = recordTye;
        data[8] = String.valueOf(sourceSellStatus);
        
        ProgramOperateDataDto programOperateDataDto = new ProgramOperateDataDto();
        programOperateDataDto.setProgramId(programId);
        programOperateDataDto.setScreeningId(screeningId);
        programOperateDataDto.setSeatIdList(unLockSeatIdList);
        programOperateDataDto.setTicketCategoryCountDtoList(ticketCategoryCountDtoList);
        programOperateDataDto.setOrderVersion(orderVersion);
        programOperateDataDto.setBizNo(Objects.isNull(orderNumber) ? null : String.valueOf(orderNumber));
        programOperateDataDto.setOperatorId(userId);
        programOperateDataDto.setSourceType(getInventorySourceType(orderStatus));
        //v4/v41 创建订单时已通过 MQ 锁定数据库座位并扣减库存
        if (!ProgramOrderVersion.isV4Version(orderVersion)){
            if (refund) {
                programOperateDataDto.setSellStatus(SellStatus.NO_SOLD.getCode());
                operateProgramData(programOperateDataDto);
            }
            orderProgramCacheResolutionOperate.programCacheReverseOperate(keys,data);
            if (Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode())) {
                programOperateDataDto.setSellStatus(SellStatus.SOLD.getCode());
                delayOperateProgramDataSend.sendMessage(JSON.toJSONString(programOperateDataDto));
            }
        }else {
            //如果创建订单版本是v4/v41 更新节目服务的相关数据
            if (Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode()) ||
                    Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode()) || refund) {
                programOperateDataDto.setSellStatus(Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode()) ? SellStatus.SOLD.getCode() : SellStatus.NO_SOLD.getCode());
                operateProgramData(programOperateDataDto);
            }
            orderProgramCacheResolutionOperate.programCacheReverseOperate(keys,data);
        }
    }

    private void operateProgramData(ProgramOperateDataDto programOperateDataDto) {
        ApiResponse<Boolean> programApiResponse = programClient.operateProgramData(programOperateDataDto);
        if (!Objects.equals(programApiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
            throw new TidesFrameException(programApiResponse);
        }
    }

    private String getInventorySourceType(OrderStatus orderStatus) {
        if (Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode())) {
            return INVENTORY_SOURCE_ORDER_PAY;
        }
        if (Objects.equals(orderStatus.getCode(), OrderStatus.REFUND.getCode())) {
            return INVENTORY_SOURCE_ORDER_REFUND;
        }
        return INVENTORY_SOURCE_ORDER_CANCEL;
    }
    
    public List<OrderListVo> selectList(OrderListDto orderListDto) {
        List<OrderListVo> orderListVos = new ArrayList<>();
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper = 
                Wrappers.lambdaQuery(Order.class)
                        .eq(Order::getUserId, orderListDto.getUserId())
                        .orderByDesc(Order::getCreateOrderTime);
        List<Order> orderList = orderMapper.selectList(orderLambdaQueryWrapper);
        if (CollectionUtil.isEmpty(orderList)) {
            return orderListVos;
        }
        orderListVos = BeanUtil.copyToList(orderList, OrderListVo.class);
        List<OrderTicketUserAggregate> orderTicketUserAggregateList = 
                orderTicketUserMapper.selectOrderTicketUserAggregate(orderList.stream().map(Order::getOrderNumber).
                        collect(Collectors.toList()));
        Map<Long, Integer> orderTicketUserAggregateMap = orderTicketUserAggregateList.stream()
                .collect(Collectors.toMap(OrderTicketUserAggregate::getOrderNumber, 
                        OrderTicketUserAggregate::getOrderTicketUserCount, (v1, v2) -> v2));
        for (OrderListVo orderListVo : orderListVos) {
            orderListVo.setTicketCount(orderTicketUserAggregateMap.get(orderListVo.getOrderNumber()));
        }
        return orderListVos;
    }
    
    public OrderGetVo get(OrderGetDto orderGetDto) {
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper =
                Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNumber, orderGetDto.getOrderNumber());
        Order order = orderMapper.selectOne(orderLambdaQueryWrapper);
        if (Objects.isNull(order)) {
            throw new TidesFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        LambdaQueryWrapper<OrderTicketUser> orderTicketUserLambdaQueryWrapper = 
                Wrappers.lambdaQuery(OrderTicketUser.class).eq(OrderTicketUser::getOrderNumber, order.getOrderNumber());
        List<OrderTicketUser> orderTicketUserList = orderTicketUserMapper.selectList(orderTicketUserLambdaQueryWrapper);
        if (CollectionUtil.isEmpty(orderTicketUserList)) {
            throw new TidesFrameException(BaseCode.TICKET_USER_ORDER_NOT_EXIST);
        }

        OrderGetVo orderGetVo = new OrderGetVo();
        BeanUtil.copyProperties(order,orderGetVo);
        
        List<OrderTicketInfoVo> orderTicketInfoVoList = new ArrayList<>();
        Map<BigDecimal, List<OrderTicketUser>> orderTicketUserMap = 
                orderTicketUserList.stream().collect(Collectors.groupingBy(OrderTicketUser::getOrderPrice));
        orderTicketUserMap.forEach((k,v) -> {
            OrderTicketInfoVo orderTicketInfoVo = new OrderTicketInfoVo();
            String seatInfo = "暂无座位信息";
            if (order.getProgramPermitChooseSeat().equals(BusinessStatus.YES.getCode())) {
                seatInfo = v.stream().map(OrderTicketUser::getSeatInfo).collect(Collectors.joining(","));
            }
            orderTicketInfoVo.setSeatInfo(seatInfo);
            orderTicketInfoVo.setPrice(v.get(0).getOrderPrice());
            orderTicketInfoVo.setQuantity(v.size());
            orderTicketInfoVo.setRelPrice(v.stream().map(OrderTicketUser::getOrderPrice)
                    .reduce(BigDecimal.ZERO,BigDecimal::add));
            orderTicketInfoVoList.add(orderTicketInfoVo);
        });
        
        orderGetVo.setOrderTicketInfoVoList(orderTicketInfoVoList);
        List<OrderTicketUserVo> orderTicketUserVoList = BeanUtil.copyToList(orderTicketUserList, OrderTicketUserVo.class);
        for (OrderTicketUserVo orderTicketUserVo : orderTicketUserVoList) {
            orderTicketUserVo.setTicketStatusName(TicketStatus.getMsg(orderTicketUserVo.getTicketStatus()));
        }
        orderGetVo.setOrderTicketUserVoList(orderTicketUserVoList);
        
        UserGetAndTicketUserListDto userGetAndTicketUserListDto = new UserGetAndTicketUserListDto();
        userGetAndTicketUserListDto.setUserId(order.getUserId());
        ApiResponse<UserGetAndTicketUserListVo> userGetAndTicketUserApiResponse = 
                userClient.getUserAndTicketUserList(userGetAndTicketUserListDto);
        
        if (!Objects.equals(userGetAndTicketUserApiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
            throw new TidesFrameException(userGetAndTicketUserApiResponse);
            
        }
        UserGetAndTicketUserListVo userAndTicketUserListVo =
                Optional.ofNullable(userGetAndTicketUserApiResponse.getData())
                        .orElseThrow(() -> new TidesFrameException(BaseCode.RPC_RESULT_DATA_EMPTY));
        if (Objects.isNull(userAndTicketUserListVo.getUserVo())) {
            throw new TidesFrameException(BaseCode.USER_EMPTY);
        }
        if (CollectionUtil.isEmpty(userAndTicketUserListVo.getTicketUserVoList())) {
            throw new TidesFrameException(BaseCode.TICKET_USER_EMPTY);
        }
        List<TicketUserVo> filterTicketUserVoList = new ArrayList<>();
        Map<Long, TicketUserVo> ticketUserVoMap = userAndTicketUserListVo.getTicketUserVoList()
                .stream().collect(Collectors.toMap(TicketUserVo::getId, ticketUserVo -> ticketUserVo, (v1, v2) -> v2));
        for (OrderTicketUser orderTicketUser : orderTicketUserList) {
            filterTicketUserVoList.add(ticketUserVoMap.get(orderTicketUser.getTicketUserId()));
        }
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtil.copyProperties(userAndTicketUserListVo.getUserVo(),userInfoVo);
        UserAndTicketUserInfoVo userAndTicketUserInfoVo = new UserAndTicketUserInfoVo();
        userAndTicketUserInfoVo.setUserInfoVo(userInfoVo);
        userAndTicketUserInfoVo.setTicketUserInfoVoList(BeanUtil.copyToList(filterTicketUserVoList, TicketUserInfoVo.class));
        orderGetVo.setUserAndTicketUserInfoVo(userAndTicketUserInfoVo);
        
        return orderGetVo;
    }
    
    public AccountOrderCountVo accountOrderCount(AccountOrderCountDto accountOrderCountDto) {
        AccountOrderCountVo accountOrderCountVo = new AccountOrderCountVo();
        accountOrderCountVo.setCount(orderMapper.accountOrderCount(accountOrderCountDto.getUserId(),
                accountOrderCountDto.getProgramId()));
        return accountOrderCountVo;
    }

    public List<MovieOrderSalesCountVo> movieSalesCount(MovieOrderSalesCountDto movieOrderSalesCountDto) {
        if (Objects.isNull(movieOrderSalesCountDto) ||
                (CollectionUtil.isEmpty(movieOrderSalesCountDto.getProgramIdList()) &&
                        CollectionUtil.isEmpty(movieOrderSalesCountDto.getScreeningIdList()))) {
            return new ArrayList<>();
        }
        List<OrderProgram> orderProgramList = orderProgramMapper.selectList(
                Wrappers.lambdaQuery(OrderProgram.class)
                        .in(CollectionUtil.isNotEmpty(movieOrderSalesCountDto.getProgramIdList()),
                                OrderProgram::getProgramId, movieOrderSalesCountDto.getProgramIdList())
                        .in(CollectionUtil.isNotEmpty(movieOrderSalesCountDto.getScreeningIdList()),
                                OrderProgram::getScreeningId, movieOrderSalesCountDto.getScreeningIdList())
                        .eq(OrderProgram::getStatus, BusinessStatus.YES.getCode()));
        if (CollectionUtil.isEmpty(orderProgramList)) {
            return new ArrayList<>();
        }
        List<Long> orderNumberList = orderProgramList.stream()
                .map(OrderProgram::getOrderNumber)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtil.isEmpty(orderNumberList)) {
            return new ArrayList<>();
        }
        return orderTicketUserMapper.selectMovieSalesCountByOrderNumbers(orderNumberList);
    }
    
    
    @RepeatExecuteLimit(name = CREATE_PROGRAM_ORDER_MQ,keys = {"#orderCreateMq.orderNumber"})
    @Transactional(rollbackFor = Exception.class)
    public String createMq(OrderCreateMq orderCreateMq){
        List<OrderTicketUserCreateDto> orderTicketUserCreateDtoList = orderCreateMq.getOrderTicketUserCreateDtoList();
        //使用 Stream API 按 ticketCategoryId 分组并计数
        Map<Long, Long> countMap = orderTicketUserCreateDtoList.stream()
                .collect(Collectors.groupingBy(OrderTicketUserCreateDto::getTicketCategoryId, Collectors.counting()));
        
        //将统计结果转换为列表，存入 TicketCountDto 对象中
        List<TicketCategoryCountDto> ticketCountList = countMap.entrySet().stream()
                .map(entry -> new TicketCategoryCountDto(entry.getKey(), entry.getValue()))
                .toList();
        //修改节目服务中的座位状态和扣减库存
        ReduceRemainNumberDto reduceRemainNumberDto = new ReduceRemainNumberDto();
        reduceRemainNumberDto.setProgramId(orderCreateMq.getProgramId());
        reduceRemainNumberDto.setScreeningId(orderCreateMq.getScreeningId());
        reduceRemainNumberDto.setSellStatus(SellStatus.LOCK.getCode());
        reduceRemainNumberDto.setSeatIdList(orderTicketUserCreateDtoList.stream().map(OrderTicketUserCreateDto::getSeatId).collect(Collectors.toList()));
        reduceRemainNumberDto.setTicketCategoryCountDtoList(ticketCountList);
        reduceRemainNumberDto.setSourceType(INVENTORY_SOURCE_ORDER_CREATE_LOCK);
        reduceRemainNumberDto.setBizNo(Objects.isNull(orderCreateMq.getOrderNumber()) ? null : String.valueOf(orderCreateMq.getOrderNumber()));
        reduceRemainNumberDto.setOperatorId(orderCreateMq.getUserId());
        ApiResponse<Boolean> programApiResponse = programClient.operateSeatLockAndTicketCategoryRemainNumber(reduceRemainNumberDto);
        if (!Objects.equals(programApiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
            //将因为修改节目服务余票和座位失败，导致丢弃的订单放入redis中
            redisCache.leftPushForList(RedisKeyBuild.createRedisKey(RedisKeyManage.DISCARD_ORDER,
                    orderCreateMq.getProgramId()),new DiscardOrder(orderCreateMq, DiscardOrderReason.MODIFY_PROGRAM_REMAIN_NUMBER_SEAT_FAIL.getCode()));
            throw new TidesFrameException(programApiResponse);
        }
        //真正地创建订单
        String orderNumber = createByMq(orderCreateMq);
        redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.ORDER_MQ,orderNumber),orderNumber,1, TimeUnit.MINUTES);
        return orderNumber;
    }
    
    public String getCache(OrderGetDto orderGetDto) {
        return redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.ORDER_MQ,orderGetDto.getOrderNumber()),String.class);
    }
    
    @RepeatExecuteLimit(name = CANCEL_PROGRAM_ORDER,keys = {"#orderCancelDto.orderNumber"})
    @ServiceLock(name = UPDATE_ORDER_STATUS_LOCK,keys = {"#orderCancelDto.orderNumber"})
    @Transactional(rollbackFor = Exception.class)
    public boolean initiateCancel(OrderCancelDto orderCancelDto){
        Order order = orderMapper.selectOne(Wrappers.lambdaQuery(Order.class)
                .eq(Order::getOrderNumber, orderCancelDto.getOrderNumber()));
        if (Objects.isNull(order)) {
            throw new TidesFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        if (!Objects.equals(order.getOrderStatus(), OrderStatus.NO_PAY.getCode())) {
            throw new TidesFrameException(BaseCode.CAN_NOT_CANCEL);
        }
        return cancel(orderCancelDto);
    }

    @ServiceLock(name = UPDATE_ORDER_STATUS_LOCK, keys = {"#orderTicketVerifyDto.ticketCode"})
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyTicket(OrderTicketVerifyDto orderTicketVerifyDto) {
        OrderTicketUser orderTicketUser = orderTicketUserMapper.selectOne(Wrappers.lambdaQuery(OrderTicketUser.class)
                .eq(OrderTicketUser::getTicketCode, orderTicketVerifyDto.getTicketCode()));
        if (Objects.isNull(orderTicketUser)) {
            throw new TidesFrameException(BaseCode.TICKET_CODE_NOT_EXIST);
        }
        if (!Objects.equals(orderTicketUser.getOrderStatus(), OrderStatus.PAY.getCode()) ||
                !Objects.equals(orderTicketUser.getTicketStatus(), TicketStatus.ISSUED.getCode())) {
            throw new TidesFrameException(BaseCode.TICKET_STATUS_NOT_PERMIT);
        }
        OrderTicketUser updateOrderTicketUser = new OrderTicketUser();
        updateOrderTicketUser.setTicketStatus(TicketStatus.USED.getCode());
        updateOrderTicketUser.setTicketVerifyTime(DateUtils.now());
        int updateCount = orderTicketUserMapper.update(updateOrderTicketUser, Wrappers.lambdaUpdate(OrderTicketUser.class)
                .eq(OrderTicketUser::getOrderNumber, orderTicketUser.getOrderNumber())
                .eq(OrderTicketUser::getId, orderTicketUser.getId())
                .eq(OrderTicketUser::getTicketStatus, TicketStatus.ISSUED.getCode()));
        if (updateCount <= 0) {
            throw new TidesFrameException(BaseCode.TICKET_STATUS_NOT_PERMIT);
        }
        return true;
    }

    @ServiceLock(name = UPDATE_ORDER_STATUS_LOCK, keys = {"#orderRefundApplyDto.orderNumber"})
    @Transactional(rollbackFor = Exception.class)
    public boolean refundApply(OrderRefundApplyDto orderRefundApplyDto) {
        Order order = orderMapper.selectOne(Wrappers.lambdaQuery(Order.class)
                .eq(Order::getOrderNumber, orderRefundApplyDto.getOrderNumber()));
        if (Objects.isNull(order)) {
            throw new TidesFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        if (!Objects.equals(order.getOrderStatus(), OrderStatus.PAY.getCode())) {
            throw new TidesFrameException(BaseCode.OPERATE_ORDER_STATUS_NOT_PERMIT);
        }
        Date programShowTime = order.getProgramShowTime();
        if (Objects.nonNull(programShowTime) && !DateUtils.now().before(programShowTime)) {
            throw new TidesFrameException(BaseCode.ORDER_REFUND_TIME_EXPIRED);
        }
        List<OrderTicketUser> orderTicketUserList = orderTicketUserMapper.selectList(Wrappers.lambdaQuery(OrderTicketUser.class)
                .eq(OrderTicketUser::getOrderNumber, order.getOrderNumber()));
        if (CollectionUtil.isEmpty(orderTicketUserList)) {
            throw new TidesFrameException(BaseCode.TICKET_USER_ORDER_NOT_EXIST);
        }
        for (OrderTicketUser orderTicketUser : orderTicketUserList) {
            if (Objects.equals(orderTicketUser.getTicketStatus(), TicketStatus.USED.getCode())) {
                throw new TidesFrameException(BaseCode.TICKET_USED_CAN_NOT_REFUND);
            }
            if (Objects.equals(orderTicketUser.getTicketStatus(), TicketStatus.REFUNDING.getCode()) ||
                    Objects.equals(orderTicketUser.getTicketStatus(), TicketStatus.REFUNDED.getCode())) {
                throw new TidesFrameException(BaseCode.TICKET_STATUS_NOT_PERMIT);
            }
        }
        OrderTicketUser refundingTicketUser = new OrderTicketUser();
        refundingTicketUser.setTicketStatus(TicketStatus.REFUNDING.getCode());
        refundingTicketUser.setRefundApplyTime(DateUtils.now());
        refundingTicketUser.setRefundReason(orderRefundApplyDto.getReason());
        int refundingUpdateCount = orderTicketUserMapper.update(refundingTicketUser, Wrappers.lambdaUpdate(OrderTicketUser.class)
                .eq(OrderTicketUser::getOrderNumber, order.getOrderNumber()));
        if (refundingUpdateCount <= 0) {
            throw new TidesFrameException(BaseCode.ORDER_REFUND_ERROR);
        }

        RefundDto refundDto = new RefundDto();
        refundDto.setOrderNumber(String.valueOf(order.getOrderNumber()));
        refundDto.setAmount(order.getOrderPrice());
        refundDto.setChannel("alipay");
        refundDto.setReason(StringUtil.isEmpty(orderRefundApplyDto.getReason()) ? "order refund" : orderRefundApplyDto.getReason());
        ApiResponse<String> refundResponse = payClient.refund(refundDto);
        if (!Objects.equals(refundResponse.getCode(), BaseCode.SUCCESS.getCode())) {
            throw new TidesFrameException(refundResponse);
        }
        finishRefundOrder(order, orderTicketUserList);
        return true;
    }

    private void finishRefundOrder(Order order, List<OrderTicketUser> orderTicketUserList) {
        Order updateOrder = new Order();
        updateOrder.setOrderStatus(OrderStatus.REFUND.getCode());
        updateOrder.setEditTime(DateUtils.now());
        int updateOrderCount = orderMapper.update(updateOrder, Wrappers.lambdaUpdate(Order.class)
                .eq(Order::getOrderNumber, order.getOrderNumber())
                .eq(Order::getOrderStatus, OrderStatus.PAY.getCode()));

        OrderTicketUser updateOrderTicketUser = new OrderTicketUser();
        updateOrderTicketUser.setOrderStatus(OrderStatus.REFUND.getCode());
        updateOrderTicketUser.setTicketStatus(TicketStatus.REFUNDED.getCode());
        updateOrderTicketUser.setRefundTime(DateUtils.now());
        int updateTicketUserCount = orderTicketUserMapper.update(updateOrderTicketUser, Wrappers.lambdaUpdate(OrderTicketUser.class)
                .eq(OrderTicketUser::getOrderNumber, order.getOrderNumber())
                .eq(OrderTicketUser::getTicketStatus, TicketStatus.REFUNDING.getCode()));
        if (updateOrderCount <= 0 || updateTicketUserCount <= 0) {
            throw new TidesFrameException(BaseCode.ORDER_REFUND_ERROR);
        }

        List<SeatIdAndTicketUserIdDomain> seatIdAndTicketUserIdDomainList = new ArrayList<>();
        List<OrderTicketUserRecord> orderTicketUserRecordList = new ArrayList<>();
        for (OrderTicketUser orderTicketUser : orderTicketUserList) {
            OrderTicketUserRecord orderTicketUserRecord = new OrderTicketUserRecord();
            BeanUtils.copyProperties(orderTicketUser, orderTicketUserRecord);
            orderTicketUserRecord.setId(uidGenerator.getUid());
            orderTicketUserRecord.setIdentifierId(order.getIdentifierId());
            orderTicketUserRecord.setTicketUserOrderId(orderTicketUser.getId());
            orderTicketUserRecord.setRecordTypeCode(RecordType.INCREASE.getCode());
            orderTicketUserRecord.setRecordTypeValue(RecordType.INCREASE.getValue());
            orderTicketUserRecordList.add(orderTicketUserRecord);
            seatIdAndTicketUserIdDomainList.add(new SeatIdAndTicketUserIdDomain(orderTicketUser.getSeatId(),
                    orderTicketUser.getTicketUserId()));
        }
        orderTicketUserRecordService.saveBatch(orderTicketUserRecordList);
        redisCache.incrBy(RedisKeyBuild.createRedisKey(
                RedisKeyManage.ACCOUNT_ORDER_COUNT, order.getUserId(), order.getProgramId()), -updateTicketUserCount);

        Map<Long, List<OrderTicketUser>> orderTicketUserSeatList =
                orderTicketUserList.stream().collect(Collectors.groupingBy(OrderTicketUser::getTicketCategoryId));
        Map<Long, List<Long>> seatMap = new HashMap<>(orderTicketUserSeatList.size());
        orderTicketUserSeatList.forEach((k, v) -> seatMap.put(k, v.stream()
                .map(OrderTicketUser::getSeatId).collect(Collectors.toList())));
        updateProgramRelatedDataResolution(order.getProgramId(), order.getScreeningId(), seatMap, OrderStatus.REFUND,
                order.getIdentifierId(), order.getUserId(), seatIdAndTicketUserIdDomainList, order.getOrderVersion(),
                order.getOrderNumber());
    }
    
    
    public void delOrderAndOrderTicketUser(){
        orderMapper.relDelOrder();
        orderTicketUserMapper.relDelOrderTicketUser();
        orderTicketUserRecordMapper.relDelOrderTicketUserRecord();
        orderProgramMapper.relDelOrderProgram();
    }
    
    public List<OrderListVo> simpleList(OrderSimpleListDto orderSimpleListDto) {
        if (Objects.isNull(orderSimpleListDto.getOrderNumber()) && Objects.isNull(orderSimpleListDto.getUserId())) {
            throw new TidesFrameException(BaseCode.USER_ID_AND_ORDER_NUMBER_NOT_EXIST);
        }
        List<OrderListVo> orderListVos = new ArrayList<>();
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper =
                Wrappers.lambdaQuery(Order.class)
                        .eq(Objects.nonNull(orderSimpleListDto.getOrderNumber()),Order::getOrderNumber, orderSimpleListDto.getOrderNumber())
                        .eq(Objects.nonNull(orderSimpleListDto.getUserId()),Order::getUserId, orderSimpleListDto.getUserId())
                        .orderByDesc(Order::getCreateOrderTime);
        List<Order> orderList = orderMapper.selectList(orderLambdaQueryWrapper);
        if (CollectionUtil.isEmpty(orderList)) {
            return orderListVos;
        }
        return BeanUtil.copyToList(orderList, OrderListVo.class);
    }
}
