package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "ProjectApprovalPolicyChangeLogVo", description = "Project approval policy change log")
public class ProjectApprovalPolicyChangeLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long policyId;

    private String approvalType;

    private String actionType;

    private String actionName;

    private String changeType;

    private String changeSummary;

    private Long operatorId;

    private String operatorName;

    private String operatorAccount;

    private String operationSource;

    private String requestTraceId;

    private String clientIp;

    private String userAgent;

    private String beforeSnapshotJson;

    private String afterSnapshotJson;

    private String remark;

    private Integer status;

    private Date createTime;

    private Date editTime;
}
