package com.tides.handler;

import com.tides.entity.MessageProducerRecord;
import com.tides.enums.MessageType;

import java.util.List;

/**
 * @description: 异常消息处理
 * @author: 19continue
 **/
public interface ExceptionMessageHandler {
    
    /**
     * 获取没有对账的消息发送记录集合
     * @return 结果
     * */
    List<MessageProducerRecord> noReconciliationMessageProducerRecordList();
    
    /**
     * 处理消息
     * @param messageProducerRecord 消息记录
     * @return 结果
     * */
    Boolean handle(MessageProducerRecord messageProducerRecord);
    
    /**
     * 获取消息类型
     * @return 结果
     * */
    MessageType getMessageType();
}
