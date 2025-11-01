package com.tides.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.OrderTicketUser;
import com.tides.entity.OrderTicketUserAggregate;
import com.tides.vo.MovieOrderSalesCountVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description: 购票人订单 mapper
 * @author: 19continue
 **/
public interface OrderTicketUserMapper extends BaseMapper<OrderTicketUser> {
    
    /**
     * 查询订单下购票人数量
     * @param orderNumberList 参数
     * @return 结果
     * */
    List<OrderTicketUserAggregate> selectOrderTicketUserAggregate(@Param("orderNumberList")List<Long> orderNumberList);

    /**
     * 按电影场次/节目聚合已支付真实购票明细。
     * @param programIdList 节目id
     * @param screeningIdList 场次id
     * @return 结果
     * */
    List<MovieOrderSalesCountVo> selectMovieSalesCount(@Param("programIdList")List<Long> programIdList,
                                                       @Param("screeningIdList")List<Long> screeningIdList);

    /**
     * 按已路由的订单号聚合电影真实已支付购票明细
     * @param orderNumberList 订单号列表
     * @return 结果
     * */
    List<MovieOrderSalesCountVo> selectMovieSalesCountByOrderNumbers(@Param("orderNumberList")List<Long> orderNumberList);
    
    /**
     * 真实删除购票人订单数据
     * @return 结果
     * */
    Integer relDelOrderTicketUser();

}
