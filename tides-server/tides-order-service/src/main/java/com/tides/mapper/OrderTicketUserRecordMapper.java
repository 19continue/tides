package com.tides.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.OrderTicketUserRecord;

/**
 * @description: 购票人订单记录 mapper
 * @author: 19continue
 **/
public interface OrderTicketUserRecordMapper extends BaseMapper<OrderTicketUserRecord> {
    
    /**
     * 真实删除购票人订单记录数据
     * @return 结果
     * */
    Integer relDelOrderTicketUserRecord();

}
