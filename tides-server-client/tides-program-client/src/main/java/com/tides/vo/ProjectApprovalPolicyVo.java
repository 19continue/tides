package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Schema(title = "ProjectApprovalPolicyVo", description = "Project approval policy")
public class ProjectApprovalPolicyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String approvalType;

    private String actionType;

    private String actionName;

    private String allowedRoleCodes;

    private List<String> allowedRoleCodeList;

    private Integer preventSelfApproval;

    private Integer requireLogin;

    private String riskLevel;

    private String remark;

    private Integer status;

    private String statusName;

    private Date createTime;

    private Date editTime;
}
