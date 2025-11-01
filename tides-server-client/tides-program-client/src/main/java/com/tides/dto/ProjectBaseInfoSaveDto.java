package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(title = "ProjectBaseInfoSaveDto", description = "Project base information save")
public class ProjectBaseInfoSaveDto {

    @Schema(name = "projectId", type = "Long", description = "project/program id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long projectId;

    @Schema(name = "operatorId", type = "Long", description = "operator id")
    private Long operatorId;

    @Schema(name = "operatorName", type = "String", description = "operator name")
    @Size(max = 128)
    private String operatorName;

    @Schema(name = "remark", type = "String", description = "operation remark")
    @Size(max = 512)
    private String remark;

    @Schema(name = "title", type = "String", description = "project title", requiredMode = RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 512)
    private String title;

    @Schema(name = "parentProgramCategoryId", type = "Long", description = "parent category id")
    private Long parentProgramCategoryId;

    @Schema(name = "programCategoryId", type = "Long", description = "category id")
    private Long programCategoryId;

    @Schema(name = "areaId", type = "Long", description = "city or area id")
    private Long areaId;

    @Schema(name = "actor", type = "String", description = "actor or main artist")
    @Size(max = 256)
    private String actor;

    @Schema(name = "mainActor", type = "String", description = "main actor")
    @Size(max = 100)
    private String mainActor;

    @Schema(name = "place", type = "String", description = "place or venue")
    @Size(max = 100)
    private String place;

    @Schema(name = "itemPicture", type = "String", description = "main image")
    private String itemPicture;

    @Schema(name = "detail", type = "String", description = "project detail")
    private String detail;

    @Schema(name = "preSell", type = "Integer", description = "pre sell flag")
    private Integer preSell;

    @Schema(name = "preSellInstruction", type = "String", description = "pre sell instruction")
    @Size(max = 256)
    private String preSellInstruction;

    @Schema(name = "importantNotice", type = "String", description = "important notice")
    @Size(max = 100)
    private String importantNotice;
}
