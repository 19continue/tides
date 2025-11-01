package com.tides.service.delayconsumer;

import com.alibaba.fastjson.JSON;
import com.tides.client.ApiDataClient;
import com.tides.common.ApiResponse;
import com.tides.core.ConsumerTask;
import com.tides.core.SpringUtil;
import com.tides.dto.InsertMessageConsumerRecordDto;
import com.tides.dto.MessageIdDto;
import com.tides.dto.OrderCancelDto;
import com.tides.dto.UpdateMessageConsumerRecordDto;
import com.tides.enums.BaseCode;
import com.tides.enums.MessageConsumerStatus;
import com.tides.enums.MessageType;
import com.tides.exception.TidesFrameException;
import com.tides.module.DelayOrderCancelMessageModule;
import com.tides.service.OrderService;
import com.tides.util.DateUtils;
import com.tides.util.StringUtil;
import com.tides.vo.MessageConsumerRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.tides.service.constant.OrderConstant.DELAY_ORDER_CANCEL_TOPIC;

/**
 * @description: 延迟订单取消
 * @author: 19continue
 **/
@Slf4j
@Component
public class DelayOrderCancelConsumer implements ConsumerTask {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ApiDataClient apiDataClient;
    
    
    @Override
    public void execute(String content) {
        log.info("延迟订单取消消息进行消费 content : {}", content);
        if (StringUtil.isEmpty(content)) {
            log.error("延迟队列消息不存在");
            return;
        }
        DelayOrderCancelMessageModule delayOrderCancelMessageModule = JSON.parseObject(content, DelayOrderCancelMessageModule.class);
        
        Long messageTraceId = delayOrderCancelMessageModule.getMessageTraceId();
        Long messageId = delayOrderCancelMessageModule.getMessageId();
        Long programId = delayOrderCancelMessageModule.getProgramId();
        Long orderNumber = delayOrderCancelMessageModule.getOrderNumber();
        
        MessageIdDto messageIdDto = new MessageIdDto();
        messageIdDto.setMessageId(messageId);
        ApiResponse<MessageConsumerRecordVo> apiResponse = apiDataClient.getMessageConsumerByMessageId(messageIdDto);
        if (!apiResponse.getCode().equals(BaseCode.SUCCESS.getCode())) {
            log.error("查询消息消费记录失败 messageId : {}",messageId);
            return;
        }
        
        MessageConsumerRecordVo existMessageConsumerRecordVo = apiResponse.getData();
        
        if (Objects.nonNull(existMessageConsumerRecordVo) &&
                existMessageConsumerRecordVo.getMessageConsumerStatus().equals(MessageConsumerStatus.CONSUMER_SUCCESS.getCode())) {
            return;
        }
        Long messageConsumerRecordId = null;
        Integer messageConsumerCount;
        if (Objects.isNull(existMessageConsumerRecordVo)) {
            InsertMessageConsumerRecordDto insertMessageConsumerRecordDto = new InsertMessageConsumerRecordDto();
            insertMessageConsumerRecordDto.setMessageId(messageId);
            insertMessageConsumerRecordDto.setMessageTraceId(messageTraceId);
            insertMessageConsumerRecordDto.setMessageType(MessageType.DELAY_ORDER_CANCEL.getCode());
            insertMessageConsumerRecordDto.setMessageBusinessesId(programId);
            insertMessageConsumerRecordDto.setMessageTopic(SpringUtil.getPrefixDistinctionName() + "-" + DELAY_ORDER_CANCEL_TOPIC);
            insertMessageConsumerRecordDto.setMessageContent(content);
            ApiResponse<MessageConsumerRecordVo> insertApiResponse = apiDataClient.insertMessageConsumerRecord(insertMessageConsumerRecordDto);
            if (!insertApiResponse.getCode().equals(BaseCode.SUCCESS.getCode())) {
                log.error("添加消息消费记录失败 insertMessageConsumerRecordDto : {}", JSON.toJSONString(insertMessageConsumerRecordDto));
                return;
            }
            MessageConsumerRecordVo saveMessageConsumerRecordVo = insertApiResponse.getData();
            messageConsumerRecordId = saveMessageConsumerRecordVo.getId();
            messageConsumerCount = saveMessageConsumerRecordVo.getMessageConsumerCount();
        }else {
            messageConsumerRecordId = existMessageConsumerRecordVo.getId();
            messageConsumerCount = existMessageConsumerRecordVo.getMessageConsumerCount() + 1;
        }
        UpdateMessageConsumerRecordDto updateMessageConsumerRecordDto = new UpdateMessageConsumerRecordDto();
        updateMessageConsumerRecordDto.setId(messageConsumerRecordId);
        updateMessageConsumerRecordDto.setMessageConsumerCount(messageConsumerCount);
        updateMessageConsumerRecordDto.setConsumerTime(DateUtils.now());
        
        try {
            OrderCancelDto orderCancelDto = new OrderCancelDto();
            orderCancelDto.setOrderNumber(orderNumber);
            boolean cancel = orderService.cancel(orderCancelDto);
            if (cancel) {
                log.info("延迟订单取消成功 orderCancelDto : {}",content);
                updateMessageConsumerRecordDto.setMessageConsumerStatus(MessageConsumerStatus.CONSUMER_SUCCESS.getCode());
            }else {
                log.error("延迟订单取消失败 orderCancelDto : {}",content);
                updateMessageConsumerRecordDto.setMessageConsumerStatus(MessageConsumerStatus.CONSUMER_FAIL.getCode());
                updateMessageConsumerRecordDto.setMessageConsumerException("订单取消失败");
            }
        } catch (TidesFrameException e) {
            updateMessageConsumerRecordDto.setMessageConsumerStatus(MessageConsumerStatus.CONSUMER_SUCCESS.getCode());
        } catch (Exception e) {
            updateMessageConsumerRecordDto.setMessageConsumerStatus(MessageConsumerStatus.CONSUMER_FAIL.getCode());
            updateMessageConsumerRecordDto.setMessageConsumerException(e.getMessage());
        }
        apiDataClient.updateMessageConsumerRecord(updateMessageConsumerRecordDto);
    }
    
    @Override
    public String topic() {
        return SpringUtil.getPrefixDistinctionName() + "-" + DELAY_ORDER_CANCEL_TOPIC;
    }
}
