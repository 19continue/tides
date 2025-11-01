package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "ProjectApprovalManageDto", description = "Project high risk approval page query")
public class ProjectApprovalManageDto extends BasePageDto {

    @Schema(name = "programId", type = "Long", description = "project/program id")
    private Long programId;

    @Schema(name = "approvalStatus", type = "Integer", description = "1 pending, 2 approved, 3 rejected")
    private Integer approvalStatus;

    @Schema(name = "actionType", type = "String", description = "lifecycle action type")
    private String actionType;

    @Schema(name = "requesterName", type = "String", description = "requester name")
    private String requesterName;

    @Schema(name = "approverName", type = "String", description = "approver name")
    private String approverName;

    @Schema(name = "createTimeStart", type = "String", description = "create time start, yyyy-MM-dd HH:mm:ss")
    private String createTimeStart;

    @Schema(name = "createTimeEnd", type = "String", description = "create time end, yyyy-MM-dd HH:mm:ss")
    private String createTimeEnd;
}
