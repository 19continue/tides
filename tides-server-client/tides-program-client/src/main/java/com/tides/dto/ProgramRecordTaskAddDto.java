package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description: 节目对账记录任务添加 dto
 * @author: 19continue
 **/
@Data
@Schema(title="ProgramRecordTaskAddDto", description ="节目对账记录任务添加")
public class ProgramRecordTaskAddDto {
    
    @Schema(name ="programId", type ="Long", description ="节目id",requiredMode= RequiredMode.REQUIRED)
    @NotNull
    private Long programId;
}
