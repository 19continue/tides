package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "ProjectManageDto", description = "Professional project center page")
public class ProjectManageDto extends BasePageDto {

    @Schema(name = "projectId", type = "Long", description = "project/program id")
    private Long projectId;

    @Schema(name = "title", type = "String", description = "project title")
    private String title;

    @Schema(name = "parentProgramCategoryId", type = "Long", description = "parent category id")
    private Long parentProgramCategoryId;

    @Schema(name = "programCategoryId", type = "Long", description = "category id")
    private Long programCategoryId;

    @Schema(name = "lifecycleStatus", type = "Integer", description = "project lifecycle status")
    private Integer lifecycleStatus;
}
