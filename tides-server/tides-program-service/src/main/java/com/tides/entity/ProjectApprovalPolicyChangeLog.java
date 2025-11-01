package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("d_project_approval_policy_change_log")
public class ProjectApprovalPolicyChangeLog extends BaseTableData implements Serializable {

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
}
