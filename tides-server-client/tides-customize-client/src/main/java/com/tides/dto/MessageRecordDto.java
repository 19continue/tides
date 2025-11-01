package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @description: 消息记录 dto
 * @author: 19continue
 **/
@Data
@Schema(title="MessageRecordDto", description ="消息记录")
public class MessageRecordDto extends BasePageDto {
    
    /**
     * 消息业务id
     */
    @Schema(name ="messageBusinessesId", type ="Long", description ="消息业务id")
    private Long messageBusinessesId;

    @Schema(name ="messageTraceId", type ="Long", description ="消息链路id")
    private Long messageTraceId;

    @Schema(name ="messageId", type ="Long", description ="消息id")
    private Long messageId;

    @Schema(name ="messageTopic", type ="String", description ="消息主题")
    private String messageTopic;

    @Schema(name ="messageSendStatus", type ="Integer", description ="发送状态")
    private Integer messageSendStatus;

    @Schema(name ="messageConsumerStatus", type ="Integer", description ="消费状态")
    private Integer messageConsumerStatus;

    @Schema(name ="reconciliationStatus", type ="Integer", description ="对账状态")
    private Integer reconciliationStatus;

    @Schema(name ="sendTimeStart", type ="Date", description ="发送时间开始")
    private Date sendTimeStart;

    @Schema(name ="sendTimeEnd", type ="Date", description ="发送时间结束")
    private Date sendTimeEnd;
}
