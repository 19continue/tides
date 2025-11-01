package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description: 消息id dto
 * @author: 19continue
 **/
@Data
@Schema(title="MessageIdDto", description ="消息id")
public class MessageIdDto {
    
    /**
     * 消息id
     */
    @Schema(name ="messageId", type ="Long", description ="消息id", requiredMode= RequiredMode.REQUIRED)
    @NotNull
    private Long messageId;
}
