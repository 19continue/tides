package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.AddApiDataDto;
import com.tides.dto.InsertMessageConsumerRecordDto;
import com.tides.dto.InsertMessageProducerRecordDto;
import com.tides.dto.MessageIdDto;
import com.tides.dto.UpdateMessageConsumerRecordDto;
import com.tides.dto.UpdateMessageProducerRecordDto;
import com.tides.enums.BaseCode;
import com.tides.vo.MessageConsumerRecordVo;
import com.tides.vo.MessageProducerRecordVo;
import org.springframework.stereotype.Component;

/**
 * @description: 定制服务 feign 异常
 * @author: 19continue
 **/
@Component
public class ApiDataClientFallback implements ApiDataClient {
    
    @Override
    public ApiResponse<Boolean> add(final AddApiDataDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<MessageProducerRecordVo> insertMessageProducerRecord(final InsertMessageProducerRecordDto insertMessageProducerRecordDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<Boolean> updateMessageProducerRecord(final UpdateMessageProducerRecordDto updateMessageProducerRecordDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<MessageConsumerRecordVo> getMessageConsumerByMessageId(final MessageIdDto messageIdDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<MessageConsumerRecordVo> insertMessageConsumerRecord(final InsertMessageConsumerRecordDto insertMessageConsumerRecordDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<Boolean> updateMessageConsumerRecord(final UpdateMessageConsumerRecordDto updateMessageConsumerRecordDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
