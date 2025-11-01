package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(title = "ProjectApprovalPolicySaveDto", description = "Project approval policy save")
public class ProjectApprovalPolicySaveDto {

    @Schema(name = "id", type = "Long", description = "policy id")
    private Long id;

    @Schema(name = "approvalType", type = "String", description = "approval type")
    @NotBlank
    private String approvalType;

    @Schema(name = "actionType", type = "String", description = "action type")
    @NotBlank
    private String actionType;

    @Schema(name = "actionName", type = "String", description = "action name")
    @NotBlank
    private String actionName;

    @Schema(name = "allowedRoleCodeList", type = "List", description = "allowed role code list")
    @NotEmpty
    private List<String> allowedRoleCodeList;

    @Schema(name = "preventSelfApproval", type = "Integer", description = "1 prevent requester approving own order")
    @NotNull
    private Integer preventSelfApproval;

    @Schema(name = "requireLogin", type = "Integer", description = "1 require back manage login context")
    @NotNull
    private Integer requireLogin;

    @Schema(name = "riskLevel", type = "String", description = "risk level")
    @NotBlank
    private String riskLevel;

    @Schema(name = "remark", type = "String", description = "policy remark")
    private String remark;
}
