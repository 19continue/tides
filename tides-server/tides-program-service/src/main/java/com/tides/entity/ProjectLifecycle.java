package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("d_project_lifecycle")
public class ProjectLifecycle extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

    private String projectTypeName;

    private Integer lifecycleStatus;

    private Integer previousLifecycleStatus;

    private Integer reviewStatus;

    private Long ownerId;

    private Long reviewerId;

    private String reviewRemark;

    private String lastActionType;

    private String lastActionName;

    private Long lastOperatorId;

    private String lastOperatorName;

    private Date lastTransitionTime;

    private Integer version;
}
