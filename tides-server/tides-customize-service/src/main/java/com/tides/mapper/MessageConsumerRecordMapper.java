package com.tides.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.MessageConsumerRecord;
import org.apache.ibatis.annotations.Delete;

/**
 * @description: 消息消费记录 mapper
 * @author: 19continue
 **/
public interface MessageConsumerRecordMapper extends BaseMapper<MessageConsumerRecord> {
    
    /**
     * 删除所有记录 
     * @return Integer 结果
     * */
    @Delete("DELETE FROM d_message_consumer_record")
    Integer delete();
}
