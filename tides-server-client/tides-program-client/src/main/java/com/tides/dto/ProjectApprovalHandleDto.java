package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "ProjectApprovalHandleDto", description = "Project high risk approval handle")
public class ProjectApprovalHandleDto {

    @Schema(name = "approvalId", type = "Long", description = "approval order id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long approvalId;

    @Schema(name = "programId", type = "Long", description = "project/program id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long programId;

    @Schema(name = "approveResult", type = "Integer", description = "2 approve, 3 reject", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Integer approveResult;

    @Schema(name = "approveRemark", type = "String", description = "approval remark")
    private String approveRemark;

    @Schema(name = "operatorId", type = "Long", description = "operator user id")
    private Long operatorId;

    @Schema(name = "operatorName", type = "String", description = "operator name")
    private String operatorName;
}
