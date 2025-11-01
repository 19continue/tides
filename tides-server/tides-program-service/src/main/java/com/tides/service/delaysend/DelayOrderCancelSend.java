package com.tides.service.delaysend;

import com.alibaba.fastjson2.JSON;
import com.baidu.fsg.uid.UidGenerator;
import com.tides.BusinessThreadPool;
import com.tides.client.ApiDataClient;
import com.tides.common.ApiResponse;
import com.tides.context.DelayQueueContext;
import com.tides.core.SpringUtil;
import com.tides.dto.DelayOrderCancelDto;
import com.tides.dto.InsertMessageProducerRecordDto;
import com.tides.dto.UpdateMessageProducerRecordDto;
import com.tides.enums.BaseCode;
import com.tides.enums.MessageSendStatus;
import com.tides.enums.MessageType;
import com.tides.module.DelayOrderCancelMessageModule;
import com.tides.vo.MessageProducerRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.tides.constant.ProgramOrderConstant.DELAY_ORDER_CANCEL_TIME;
import static com.tides.constant.ProgramOrderConstant.DELAY_ORDER_CANCEL_TIME_UNIT;
import static com.tides.constant.ProgramOrderConstant.DELAY_ORDER_CANCEL_TOPIC;

/**
 * @description: 延迟订单发送
 * @author: 19continue
 **/
@Slf4j
@Component
public class DelayOrderCancelSend {
    
    @Autowired
    private UidGenerator uidGenerator;
    
    @Autowired
    private DelayQueueContext delayQueueContext;
    
    
    @Autowired
    private ApiDataClient apiDataClient;
    
    @Value("${delay.order.cancel:false}")
    private Boolean delayOrderCancel;
    
    public void sendMessage(DelayOrderCancelDto delayOrderCancelDto){
        if (!delayOrderCancel){
            return;
        }
        BusinessThreadPool.execute(() -> {
            Long messageTraceId = uidGenerator.getUid();
            Long messageId = uidGenerator.getUid();
            
            DelayOrderCancelMessageModule delayOrderCancelMessageModule = new DelayOrderCancelMessageModule();
            delayOrderCancelMessageModule.setMessageTraceId(messageTraceId);
            delayOrderCancelMessageModule.setMessageId(messageId);
            delayOrderCancelMessageModule.setProgramId(delayOrderCancelDto.getProgramId());
            delayOrderCancelMessageModule.setOrderNumber(delayOrderCancelDto.getOrderNumber());
            
            String messageContent = JSON.toJSONString(delayOrderCancelMessageModule);
            InsertMessageProducerRecordDto insertMessageProducerRecordDto = new InsertMessageProducerRecordDto();
            insertMessageProducerRecordDto.setMessageType(MessageType.DELAY_ORDER_CANCEL.getCode());
            insertMessageProducerRecordDto.setMessageTraceId(messageTraceId);
            insertMessageProducerRecordDto.setMessageBusinessesId(delayOrderCancelMessageModule.getProgramId());
            insertMessageProducerRecordDto.setMessageId(messageId);
            insertMessageProducerRecordDto.setMessageTopic(SpringUtil.getPrefixDistinctionName() + "-" + DELAY_ORDER_CANCEL_TOPIC);
            insertMessageProducerRecordDto.setMessageContent(messageContent);
            ApiResponse<MessageProducerRecordVo> insertMessageProducerRecordApiResponse = apiDataClient.insertMessageProducerRecord(insertMessageProducerRecordDto);
            if (!insertMessageProducerRecordApiResponse.getCode().equals(BaseCode.SUCCESS.getCode())){
                log.error("添加记录消息发送日志失败，参数 : {}", JSON.toJSONString(insertMessageProducerRecordDto));
                return;
            }
            MessageProducerRecordVo messageProducerRecordVo = insertMessageProducerRecordApiResponse.getData();
            
            UpdateMessageProducerRecordDto updateMessageProducerRecordDto = new UpdateMessageProducerRecordDto();
            updateMessageProducerRecordDto.setId(messageProducerRecordVo.getId());
            
            try {
                log.info("延迟订单取消消息进行发送 消息体 : {}",messageContent);
                delayQueueContext.sendMessage(SpringUtil.getPrefixDistinctionName() + "-" + DELAY_ORDER_CANCEL_TOPIC,
                        messageContent, DELAY_ORDER_CANCEL_TIME, DELAY_ORDER_CANCEL_TIME_UNIT);
                updateMessageProducerRecordDto.setMessageSendStatus(MessageSendStatus.SEND_SUCCESS.getCode());
            }catch (Exception e) {
                log.error("send message error message : {}",messageContent,e);
                updateMessageProducerRecordDto.setMessageSendStatus(MessageSendStatus.SEND_FAIL.getCode());
                updateMessageProducerRecordDto.setMessageSendException(e.getMessage());
            }
            apiDataClient.updateMessageProducerRecord(updateMessageProducerRecordDto);
        });
    }
}
