package com.tides.service;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tides.dto.OrderWorkbenchPageDto;
import com.tides.entity.Order;
import com.tides.entity.OrderTicketUser;
import com.tides.enums.OrderStatus;
import com.tides.enums.TicketStatus;
import com.tides.mapper.OrderMapper;
import com.tides.mapper.OrderTicketUserMapper;
import com.tides.page.PageVo;
import com.tides.util.DateUtils;
import com.tides.util.StringUtil;
import com.tides.vo.OrderManageVo;
import com.tides.vo.OrderTicketUserManageVo;
import com.tides.vo.OrderWorkbenchVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Order operation workbench query service.
 */
@Service
public class OrderWorkbenchService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final int DEFAULT_RECENT_DAYS = 7;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderTicketUserMapper orderTicketUserMapper;

    public OrderWorkbenchVo page(OrderWorkbenchPageDto dto) {
        Date startTime = resolveStartTime(dto);
        Date endTime = resolveEndTime(dto, startTime);
        int pageNumber = Math.max(1, dto.getPageNumber());
        int pageSize = Math.min(Math.max(1, dto.getPageSize()), MAX_PAGE_SIZE);
        LambdaQueryWrapper<Order> pageQuery = buildOrderQuery(dto, startTime, endTime, true)
                .orderByDesc(Order::getCreateOrderTime);
        IPage<Order> orderPage = orderMapper.selectPage(new Page<>(pageNumber, pageSize), pageQuery);
        List<OrderManageVo> orderManageVoList = orderPage.getRecords().stream()
                .map(this::buildOrderManageVo)
                .toList();

        OrderWorkbenchVo result = new OrderWorkbenchVo();
        result.setOrderPage(new PageVo<>(orderPage.getCurrent(), orderPage.getSize(),
                orderPage.getTotal(), orderManageVoList));
        result.setTotalCount(countByStatus(dto, startTime, endTime, null));
        result.setNoPayCount(countByStatus(dto, startTime, endTime, OrderStatus.NO_PAY.getCode()));
        result.setPaidCount(countByStatus(dto, startTime, endTime, OrderStatus.PAY.getCode()));
        result.setCancelCount(countByStatus(dto, startTime, endTime, OrderStatus.CANCEL.getCode()));
        result.setRefundCount(countByStatus(dto, startTime, endTime, OrderStatus.REFUND.getCode()));
        result.setAbnormalCount(result.getNoPayCount() + result.getRefundCount());
        result.setTimeRangeStart(DateUtils.formatDateTime(startTime));
        result.setTimeRangeEnd(DateUtils.formatDateTime(endTime));
        result.setScopeTip(buildScopeTip(dto, startTime));
        return result;
    }

    private OrderManageVo buildOrderManageVo(Order order) {
        OrderManageVo orderManageVo = new OrderManageVo();
        BeanUtils.copyProperties(order, orderManageVo);
        orderManageVo.setOrderStatusName(orderStatusName(order.getOrderStatus()));
        List<OrderTicketUser> ticketUserList = orderTicketUserMapper.selectList(Wrappers.lambdaQuery(OrderTicketUser.class)
                .eq(OrderTicketUser::getOrderNumber, order.getOrderNumber()));
        if (CollectionUtil.isEmpty(ticketUserList)) {
            orderManageVo.setTicketCount(0);
            orderManageVo.setIssuedTicketCount(0);
            orderManageVo.setUsedTicketCount(0);
            orderManageVo.setRefundingTicketCount(0);
            return orderManageVo;
        }
        List<OrderTicketUserManageVo> ticketUserManageVoList = ticketUserList.stream()
                .map(this::buildTicketUserManageVo)
                .toList();
        orderManageVo.setTicketCount(ticketUserManageVoList.size());
        orderManageVo.setIssuedTicketCount(countTicketStatus(ticketUserList, TicketStatus.ISSUED.getCode()));
        orderManageVo.setUsedTicketCount(countTicketStatus(ticketUserList, TicketStatus.USED.getCode()));
        orderManageVo.setRefundingTicketCount(countTicketStatus(ticketUserList, TicketStatus.REFUNDING.getCode()));
        orderManageVo.setOrderTicketUserManageVoList(ticketUserManageVoList);
        return orderManageVo;
    }

    private OrderTicketUserManageVo buildTicketUserManageVo(OrderTicketUser orderTicketUser) {
        OrderTicketUserManageVo orderTicketUserManageVo = new OrderTicketUserManageVo();
        BeanUtils.copyProperties(orderTicketUser, orderTicketUserManageVo);
        orderTicketUserManageVo.setOrderStatusName(orderStatusName(orderTicketUser.getOrderStatus()));
        orderTicketUserManageVo.setTicketStatusName(ticketStatusName(orderTicketUser.getTicketStatus()));
        return orderTicketUserManageVo;
    }

    private Long countByStatus(OrderWorkbenchPageDto dto, Date startTime, Date endTime, Integer orderStatus) {
        LambdaQueryWrapper<Order> countQuery = buildOrderQuery(dto, startTime, endTime, false);
        if (Objects.nonNull(orderStatus)) {
            countQuery.eq(Order::getOrderStatus, orderStatus);
        }
        return orderMapper.selectCount(countQuery);
    }

    private LambdaQueryWrapper<Order> buildOrderQuery(OrderWorkbenchPageDto dto, Date startTime,
                                                     Date endTime, boolean includeSelectedStatus) {
        LambdaQueryWrapper<Order> query = Wrappers.lambdaQuery(Order.class)
                .eq(Objects.nonNull(dto.getProgramId()), Order::getProgramId, dto.getProgramId())
                .eq(Objects.nonNull(dto.getScreeningId()), Order::getScreeningId, dto.getScreeningId())
                .eq(Objects.nonNull(dto.getOrderNumber()), Order::getOrderNumber, dto.getOrderNumber())
                .eq(Objects.nonNull(dto.getUserId()), Order::getUserId, dto.getUserId())
                .ge(Objects.nonNull(startTime), Order::getCreateOrderTime, startTime)
                .le(Objects.nonNull(endTime), Order::getCreateOrderTime, endTime);
        if (includeSelectedStatus && Objects.nonNull(dto.getOrderStatus())) {
            query.eq(Order::getOrderStatus, dto.getOrderStatus());
        }
        return query;
    }

    private Date resolveStartTime(OrderWorkbenchPageDto dto) {
        Date parsed = parseQueryTime(dto.getCreateTimeStart(), false);
        if (Objects.nonNull(parsed) || preciseSearch(dto)) {
            return parsed;
        }
        return DateUtils.addDay(DateUtils.now(), -DEFAULT_RECENT_DAYS);
    }

    private Date resolveEndTime(OrderWorkbenchPageDto dto, Date startTime) {
        Date parsed = parseQueryTime(dto.getCreateTimeEnd(), true);
        if (Objects.nonNull(parsed)) {
            return parsed;
        }
        return Objects.nonNull(startTime) ? DateUtils.now() : null;
    }

    private Date parseQueryTime(String value, boolean endOfDay) {
        if (StringUtil.isEmpty(value)) {
            return null;
        }
        String trimValue = value.trim();
        Date date = trimValue.length() <= DateUtils.FORMAT_DATE.length() ?
                DateUtils.parseDate(trimValue) : DateUtils.parseDateTime(trimValue);
        if (Objects.isNull(date)) {
            return null;
        }
        return trimValue.length() <= DateUtils.FORMAT_DATE.length() ?
                (endOfDay ? DateUtils.getDateEnd(date) : DateUtils.getDateStart(date)) : date;
    }

    private boolean preciseSearch(OrderWorkbenchPageDto dto) {
        return Objects.nonNull(dto.getOrderNumber()) || Objects.nonNull(dto.getUserId());
    }

    private String buildScopeTip(OrderWorkbenchPageDto dto, Date startTime) {
        if (Objects.nonNull(dto.getOrderNumber())) {
            return "按订单号精确查询";
        }
        if (Objects.nonNull(dto.getUserId())) {
            return "按用户查询";
        }
        if (Objects.nonNull(startTime)) {
            return "默认保护最近7天下单数据，可通过时间筛选扩大范围";
        }
        return "当前筛选范围";
    }

    private Integer countTicketStatus(List<OrderTicketUser> ticketUserList, Integer ticketStatus) {
        return Math.toIntExact(ticketUserList.stream()
                .filter(ticketUser -> Objects.equals(ticketUser.getTicketStatus(), ticketStatus))
                .count());
    }

    private String orderStatusName(Integer orderStatus) {
        return Objects.isNull(orderStatus) ? "" : OrderStatus.getMsg(orderStatus);
    }

    private String ticketStatusName(Integer ticketStatus) {
        return Objects.isNull(ticketStatus) ? "" : TicketStatus.getMsg(ticketStatus);
    }
}
