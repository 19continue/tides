package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @description: 节目推荐列表查询 dto
 * @author: 19continue
 **/
@Data
@Schema(title="ProgramRecommendListDto", description ="节目推荐列表")
public class ProgramRecommendListDto {
    
    @Schema(name ="areaId", type ="Long", description ="所在区域id")
    private Long areaId;

    @Schema(name ="areaIds", type ="Long[]", description ="所在区域id集合，区县场景可同时传区县和城市")
    private List<Long> areaIds;
    
    @Schema(name ="parentProgramCategoryId", type ="Long", description ="父节目类型id")
    private Long parentProgramCategoryId;
    
    @Schema(name ="programId", type ="Long", description ="查看节目详情时，调用推荐列表时要传入此节目id")
    private Long programId;
}
