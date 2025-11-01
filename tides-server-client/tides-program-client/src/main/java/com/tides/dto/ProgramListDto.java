package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @description: 主页节目列表查询 dto
 * @author: 19continue
 **/
@Data
@Schema(title="ProgramListDto", description ="主页节目列表")
public class ProgramListDto {
    
    @Schema(name ="areaId", type ="Long", description ="所在区域id")
    private Long areaId;

    @Schema(name ="areaIds", type ="Long[]", description ="所在区域id集合，区县场景可同时传区县和城市")
    private List<Long> areaIds;
    
    @Schema(name ="parentProgramCategoryIds", type ="Long[]", description ="父节目类型id集合",requiredMode= RequiredMode.REQUIRED)
    @NotNull
    @Size(max = 4)
    private List<Long> parentProgramCategoryIds;
}
