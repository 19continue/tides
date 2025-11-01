package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description: 父节目类型 dto
 * @author: 19continue
 **/
@Data
@Schema(title="ParentProgramCategoryDto", description ="父节目类型")
public class ParentProgramCategoryDto {
    
    @Schema(name ="parentProgramCategoryId", requiredMode= RequiredMode.REQUIRED, type ="Long", description ="父节目类型id")
    @NotNull
    private Long parentProgramCategoryId;
}
