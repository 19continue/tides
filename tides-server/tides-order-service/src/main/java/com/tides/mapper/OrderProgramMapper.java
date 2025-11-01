package com.tides.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.OrderProgram;

/**
 * @description: 订单节目 mapper
 * @author: 19continue
 **/
public interface OrderProgramMapper extends BaseMapper<OrderProgram> {
    
    /**
     * 真实删除订单节目数据
     * @return 结果
     * */
    Integer relDelOrderProgram();
}
