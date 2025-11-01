package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.AddApiDataDto;
import com.tides.dto.InsertMessageConsumerRecordDto;
import com.tides.dto.InsertMessageProducerRecordDto;
import com.tides.dto.MessageIdDto;
import com.tides.dto.UpdateMessageConsumerRecordDto;
import com.tides.dto.UpdateMessageProducerRecordDto;
import com.tides.vo.MessageConsumerRecordVo;
import com.tides.vo.MessageProducerRecordVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import static com.tides.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

/**
 * @description: 定制服务 feign
 * @author: 19continue
 **/
@Component
@FeignClient(value = SPRING_INJECT_PREFIX_DISTINCTION_NAME+"-"+"customize-service",fallback = ApiDataClientFallback.class)
public interface ApiDataClient {
    
    /**
     * 添加
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping(value = "/apiData/add")
    ApiResponse<Boolean> add(AddApiDataDto dto);
    
    /**
     * 添加消息发送记录
     * @param insertMessageProducerRecordDto 参数
     * @return 结果
     * */
    @PostMapping(value = "/message/producer/record/insert")
    ApiResponse<MessageProducerRecordVo> insertMessageProducerRecord(InsertMessageProducerRecordDto insertMessageProducerRecordDto);
    
    /**
     * 更新消息发送记录
     * @param updateMessageProducerRecordDto 参数
     * @return 结果
     * */
    @PostMapping(value = "/message/producer/record/update")
    ApiResponse<Boolean> updateMessageProducerRecord(UpdateMessageProducerRecordDto updateMessageProducerRecordDto);
    
    /**
     * 查询消息消费记录
     * @param messageIdDto 参数
     * @return 结果
     * */
    @PostMapping(value = "/message/consumer/record/getByMessageId")
    ApiResponse<MessageConsumerRecordVo> getMessageConsumerByMessageId(MessageIdDto messageIdDto);
    
    /**
     * 添加消息消费记录
     * @param insertMessageConsumerRecordDto 参数
     * @return 结果
     * */
    @PostMapping(value = "/message/consumer/record/insert")
    ApiResponse<MessageConsumerRecordVo> insertMessageConsumerRecord(InsertMessageConsumerRecordDto insertMessageConsumerRecordDto);
    
    /**
     * 更新消息消费记录
     * @param updateMessageConsumerRecordDto 参数
     * @return 结果
     * */
    @PostMapping(value = "/message/consumer/record/update")
    ApiResponse<Boolean> updateMessageConsumerRecord(UpdateMessageConsumerRecordDto updateMessageConsumerRecordDto);
}
