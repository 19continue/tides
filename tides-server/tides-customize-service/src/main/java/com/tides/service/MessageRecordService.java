package com.tides.service;


import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tides.dto.ExecuteExceptionMessageDto;
import com.tides.dto.MessageRecordDto;
import com.tides.entity.MessageConsumerRecord;
import com.tides.entity.MessageProducerRecord;
import com.tides.enums.BaseCode;
import com.tides.enums.MessageConsumerStatus;
import com.tides.enums.MessageSendStatus;
import com.tides.enums.MessageType;
import com.tides.enums.ReconciliationStatus;
import com.tides.exception.TidesFrameException;
import com.tides.handler.ExceptionMessageHandlerContext;
import com.tides.mapper.MessageConsumerRecordMapper;
import com.tides.mapper.MessageProducerRecordMapper;
import com.tides.page.PageUtil;
import com.tides.reconciliation.ReconciliationTask;
import com.tides.reconciliation.ReconciliationTaskQueue;
import com.tides.vo.MessageRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @description: 消息记录实现层
 * @author: 19continue
 **/
@Slf4j
@Service
public class MessageRecordService {
    
    @Autowired
    private MessageProducerRecordMapper messageProducerRecordMapper;
    
    @Autowired
    private MessageConsumerRecordMapper messageConsumerRecordMapper;
    
    @Autowired
    private ExceptionMessageHandlerContext exceptionMessageHandlerContext;
    
    @Autowired
    private ReconciliationTaskQueue reconciliationTaskQueue;
    
    @Autowired
    private MessageProducerRecordService messageProducerRecordService;
    
    
    public IPage<MessageRecordVo> page(MessageRecordDto messageRecordDto) {
        IPage<MessageRecordVo> messageRecordVoPage = new Page<>(messageRecordDto.getPageNumber(), messageRecordDto.getPageSize());
        LambdaQueryWrapper<MessageProducerRecord> producerWrapper = Wrappers.lambdaQuery(MessageProducerRecord.class)
                .eq(Objects.nonNull(messageRecordDto.getMessageBusinessesId()),
                        MessageProducerRecord::getMessageBusinessesId, messageRecordDto.getMessageBusinessesId())
                .eq(Objects.nonNull(messageRecordDto.getMessageTraceId()),
                        MessageProducerRecord::getMessageTraceId, messageRecordDto.getMessageTraceId())
                .eq(Objects.nonNull(messageRecordDto.getMessageId()),
                        MessageProducerRecord::getMessageId, messageRecordDto.getMessageId())
                .eq(Objects.nonNull(messageRecordDto.getMessageSendStatus()),
                        MessageProducerRecord::getMessageSendStatus, messageRecordDto.getMessageSendStatus())
                .eq(Objects.nonNull(messageRecordDto.getReconciliationStatus()),
                        MessageProducerRecord::getReconciliationStatus, messageRecordDto.getReconciliationStatus())
                .like(Objects.nonNull(messageRecordDto.getMessageTopic()),
                        MessageProducerRecord::getMessageTopic, messageRecordDto.getMessageTopic())
                .ge(Objects.nonNull(messageRecordDto.getSendTimeStart()),
                        MessageProducerRecord::getSendTime, messageRecordDto.getSendTimeStart())
                .le(Objects.nonNull(messageRecordDto.getSendTimeEnd()),
                        MessageProducerRecord::getSendTime, messageRecordDto.getSendTimeEnd())
                .orderByDesc(MessageProducerRecord::getId);
        IPage<MessageProducerRecord> messageProducerRecordPage =
                messageProducerRecordMapper.selectPage(PageUtil.getPageParams(messageRecordDto.getPageNumber(),
                        messageRecordDto.getPageSize()), producerWrapper);
        List<MessageProducerRecord> messageProducerRecordList = messageProducerRecordPage.getRecords();
        if (CollectionUtil.isEmpty(messageProducerRecordList)) {
            return messageRecordVoPage;
        }
        List<MessageConsumerRecord> messageConsumerRecordList = 
                messageConsumerRecordMapper.selectList(Wrappers.lambdaQuery(MessageConsumerRecord.class)
                        .in(MessageConsumerRecord::getMessageId, messageProducerRecordList.stream()
                                .map(MessageProducerRecord::getMessageId).toList())
                        .eq(Objects.nonNull(messageRecordDto.getMessageConsumerStatus()),
                                MessageConsumerRecord::getMessageConsumerStatus,
                                messageRecordDto.getMessageConsumerStatus()));
        Map<Long, MessageConsumerRecord> messageConsumerRecordMap = messageConsumerRecordList.stream().collect(
                Collectors.toMap(MessageConsumerRecord::getMessageId, v -> v, 
                        (v1, v2) -> v2));
        List<MessageRecordVo> messageRecordVoList = new ArrayList<>();
        for (MessageProducerRecord messageProducerRecord : messageProducerRecordList) {
            if (Objects.nonNull(messageRecordDto.getMessageConsumerStatus()) &&
                    !messageConsumerRecordMap.containsKey(messageProducerRecord.getMessageId())) {
                continue;
            }
            MessageRecordVo messageRecordVo = new MessageRecordVo();
            BeanUtils.copyProperties(messageProducerRecord, messageRecordVo);
            messageRecordVo.setMessageProducerRecordId(messageProducerRecord.getId());
            messageRecordVo.setMessageTypeName(MessageType.getMsg(messageProducerRecord.getMessageType()));
            messageRecordVo.setMessageSendStatusName(MessageSendStatus.getMsg(messageProducerRecord.getMessageSendStatus()));
            messageRecordVo.setReconciliationStatusName(ReconciliationStatus.getMsg(messageProducerRecord.getReconciliationStatus()));
            MessageConsumerRecord messageConsumerRecord = messageConsumerRecordMap.get(messageProducerRecord.getMessageId());
            if (Objects.nonNull(messageConsumerRecord)) {
                messageRecordVo.setMessageConsumerRecordId(messageConsumerRecord.getId());
                messageRecordVo.setMessageConsumerException(messageConsumerRecord.getMessageConsumerException());
                messageRecordVo.setMessageConsumerStatus(messageConsumerRecord.getMessageConsumerStatus());
                messageRecordVo.setMessageConsumerStatusName(MessageConsumerStatus.getMsg(messageConsumerRecord.getMessageConsumerStatus()));
                messageRecordVo.setMessageConsumerCount(messageConsumerRecord.getMessageConsumerCount());
                messageRecordVo.setConsumerTime(messageConsumerRecord.getConsumerTime());
            }
            messageRecordVoList.add(messageRecordVo);
        }
        BeanUtils.copyProperties(messageProducerRecordPage, messageRecordVoPage);
        messageRecordVoPage.setRecords(messageRecordVoList);
        if (Objects.nonNull(messageRecordDto.getMessageConsumerStatus())) {
            messageRecordVoPage.setTotal(messageRecordVoList.size());
        }
        return messageRecordVoPage;
    }
    
    public Boolean executeExceptionMessage(ExecuteExceptionMessageDto executeExceptionMessageDto) {
        LambdaQueryWrapper<MessageProducerRecord> wrapper = Wrappers.lambdaQuery(MessageProducerRecord.class);
        wrapper.eq(MessageProducerRecord::getMessageId, executeExceptionMessageDto.getMessageId());
        MessageProducerRecord existMessageProducerRecord = messageProducerRecordMapper.selectOne(wrapper);
        if (Objects.isNull(existMessageProducerRecord)) {
            throw new TidesFrameException(BaseCode.MESSAGE_NOT_EXIST);
        }
        if (ReconciliationStatus.RECONCILIATION_SUCCESS.getCode().equals(existMessageProducerRecord.getReconciliationStatus())) {
            return true;
        }
        MessageType messageType = MessageType.getRc(existMessageProducerRecord.getMessageType());
        if (Objects.isNull(messageType)) {
            throw new TidesFrameException(BaseCode.MESSAGE_TYPE_NOT_EXIST);
        }
        return exceptionMessageHandlerContext.getExceptionMessageHandler(messageType)
                .handle(existMessageProducerRecord);
    }
    
    public Boolean executeReconciliationTask() {
        log.info("执行消息记录的对账任务");
        for (MessageType messageType : MessageType.values()) {
            try {
                List<MessageProducerRecord> noReconciliationMessageProducerRecordList =
                        exceptionMessageHandlerContext.getExceptionMessageHandler(messageType).noReconciliationMessageProducerRecordList();
                if (CollectionUtil.isEmpty(noReconciliationMessageProducerRecordList)) {
                    continue;
                }
                Map<Long, MessageConsumerRecord> messageConsumerRecordMap = getMessageConsumerRecordMap(noReconciliationMessageProducerRecordList.stream()
                        .map(MessageProducerRecord::getMessageId).toList());
                
                for (MessageProducerRecord messageProducerRecord : noReconciliationMessageProducerRecordList){
                    MessageConsumerRecord messageConsumerRecord =
                            messageConsumerRecordMap.get(messageProducerRecord.getMessageId());
                    if (Objects.isNull(messageConsumerRecord) ||
                            messageConsumerRecord.getMessageConsumerStatus().equals(MessageConsumerStatus.UNCONSUMED.getCode()) ||
                            messageConsumerRecord.getMessageConsumerStatus().equals(MessageConsumerStatus.CONSUMER_FAIL.getCode())) {
                        ReconciliationTask reconciliationTask = () -> {
                            exceptionMessageHandlerContext.getExceptionMessageHandler(messageType).handle(messageProducerRecord);
                        };
                        reconciliationTaskQueue.putTask(reconciliationTask);
                    }else {
                        Integer messageSendStatus = messageProducerRecord.getMessageSendStatus();
                        Integer messageConsumerStatus = messageConsumerRecord.getMessageConsumerStatus();
                        if (messageSendStatus.equals(MessageSendStatus.SEND_SUCCESS.getCode()) &&
                                messageConsumerStatus.equals(MessageConsumerStatus.CONSUMER_SUCCESS.getCode())) {
                            messageProducerRecordService.updateToReconciliationSuccess(messageProducerRecord,messageConsumerRecord);
                        }
                    }
                }
            }catch (Exception e){
                log.error("executeReconciliationTask error",e);
            }
        }
        return true;
    }
    
    /**
     * 查询对应的消息消费记录
     * */
    public Map<Long, MessageConsumerRecord> getMessageConsumerRecordMap(List<Long> messageIdList){
        LambdaQueryWrapper<MessageConsumerRecord> messageConsumerRecordWrapper = Wrappers.lambdaQuery(MessageConsumerRecord.class);
        messageConsumerRecordWrapper.in(MessageConsumerRecord::getMessageId, messageIdList);
        List<MessageConsumerRecord> messageConsumerRecordList = messageConsumerRecordMapper.selectList(messageConsumerRecordWrapper);
        return messageConsumerRecordList.stream().collect(Collectors.toMap(MessageConsumerRecord::getMessageId, m -> m));
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessageRecord(Date date){
        //把之前的消息记录数据删除掉，真实环境中不会删除的，这里是为了在线演示才删除的，要不然数据太多了
        messageProducerRecordMapper.delete();
        messageConsumerRecordMapper.delete();
    }
}
