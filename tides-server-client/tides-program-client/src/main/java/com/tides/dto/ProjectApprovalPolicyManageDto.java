package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "ProjectApprovalPolicyManageDto", description = "Project approval policy page query")
public class ProjectApprovalPolicyManageDto extends BasePageDto {

    @Schema(name = "approvalType", type = "String", description = "approval type")
    private String approvalType;

    @Schema(name = "actionType", type = "String", description = "action type")
    private String actionType;

    @Schema(name = "actionName", type = "String", description = "action name")
    private String actionName;

    @Schema(name = "roleCode", type = "String", description = "allowed role code")
    private String roleCode;

    @Schema(name = "status", type = "Integer", description = "status")
    private Integer status;
}
