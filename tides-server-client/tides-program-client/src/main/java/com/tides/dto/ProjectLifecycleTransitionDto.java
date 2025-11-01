package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "ProjectLifecycleTransitionDto", description = "Project lifecycle transition")
public class ProjectLifecycleTransitionDto {

    @Schema(name = "programId", type = "Long", description = "program/project id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long programId;

    @Schema(name = "actionType", type = "String", description = "lifecycle action type", requiredMode = RequiredMode.REQUIRED)
    @NotBlank
    private String actionType;

    @Schema(name = "operatorId", type = "Long", description = "operator user id")
    private Long operatorId;

    @Schema(name = "operatorName", type = "String", description = "operator name")
    private String operatorName;

    @Schema(name = "remark", type = "String", description = "operation remark")
    private String remark;

    @Schema(name = "confirmText", type = "String", description = "danger action confirm text")
    private String confirmText;

    @Schema(name = "riskAccepted", type = "Boolean", description = "whether operator has accepted danger action risk")
    private Boolean riskAccepted;
}
