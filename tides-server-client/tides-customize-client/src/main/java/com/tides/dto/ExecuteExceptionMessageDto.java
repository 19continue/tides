package com.tides.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description: 实体
 * @author: 19continue
 **/
@Data
public class ExecuteExceptionMessageDto {

    @NotNull
    private Long messageId;
}
