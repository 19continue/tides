package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("d_project_approval_order")
public class ProjectApprovalOrder extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

    private String approvalType;

    private String approvalTitle;

    private String actionType;

    private String actionName;

    private Integer fromLifecycleStatus;

    private Integer targetLifecycleStatus;

    private Integer approvalStatus;

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

    private String snapshotJson;
}
