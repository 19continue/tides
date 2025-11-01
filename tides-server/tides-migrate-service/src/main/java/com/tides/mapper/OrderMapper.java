package com.tides.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description: 订单 mapper
 * @author: 19continue
 **/
public interface OrderMapper extends BaseMapper<Order> {
    
    /**
     * 物理删除订单 
     * @param ids 订单 id 列表
     * @return Integer 结果
     * */
    Integer physicalDeleteByIds(@Param("ids") List<Long> ids);
}
