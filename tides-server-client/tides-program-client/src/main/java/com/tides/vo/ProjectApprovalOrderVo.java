package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "ProjectApprovalOrderVo", description = "Project high risk approval order")
public class ProjectApprovalOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

    private String approvalType;

    private String approvalTitle;

    private String actionType;

    private String actionName;

    private Integer fromLifecycleStatus;

    private String fromLifecycleStatusName;

    private Integer targetLifecycleStatus;

    private String targetLifecycleStatusName;

    private Integer approvalStatus;

    private String approvalStatusName;

    private String riskLevel;

    private Long requesterId;

    private String requesterName;

    private String requesterAccount;

    private String requestReason;

    private String confirmText;

    private Long approverId;

    private String approverName;

    private String approverAccount;

    private String approveRemark;

    private Date applyTime;

    private Date approveTime;

    private Date executeTime;

    private Date createTime;
}
