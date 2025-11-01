package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("d_project_approval_policy")
public class ProjectApprovalPolicy extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String approvalType;

    private String actionType;

    private String actionName;

    private String allowedRoleCodes;

    private Integer preventSelfApproval;

    private Integer requireLogin;

    private String riskLevel;

    private String remark;
}
