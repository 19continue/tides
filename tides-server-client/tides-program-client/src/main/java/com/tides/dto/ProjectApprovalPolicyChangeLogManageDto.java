package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "ProjectApprovalPolicyChangeLogManageDto", description = "Project approval policy change log page query")
public class ProjectApprovalPolicyChangeLogManageDto extends BasePageDto {

    @Schema(name = "policyId", type = "Long", description = "policy id")
    private Long policyId;

    @Schema(name = "actionType", type = "String", description = "action type")
    private String actionType;

    @Schema(name = "changeType", type = "String", description = "change type")
    private String changeType;

    @Schema(name = "operatorName", type = "String", description = "operator name")
    private String operatorName;

    @Schema(name = "operatorAccount", type = "String", description = "operator account")
    private String operatorAccount;
}
