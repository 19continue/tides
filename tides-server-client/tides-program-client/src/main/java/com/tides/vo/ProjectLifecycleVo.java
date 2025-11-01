package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Schema(title = "ProjectLifecycleVo", description = "Project lifecycle current state")
public class ProjectLifecycleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

    private String projectTypeName;

    private Integer lifecycleStatus;

    private String lifecycleStatusName;

    private Integer previousLifecycleStatus;

    private String previousLifecycleStatusName;

    private Integer reviewStatus;

    private String reviewStatusName;

    private Long ownerId;

    private Long reviewerId;

    private String reviewRemark;

    private String lastActionType;

    private String lastActionName;

    private Long lastOperatorId;

    private String lastOperatorName;

    private Date lastTransitionTime;

    private Integer version;

    private List<LifecycleActionVo> actionList;

    @Data
    public static class LifecycleActionVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String actionType;

        private String actionName;

        private Integer targetLifecycleStatus;

        private String targetLifecycleStatusName;

        private Integer sortOrder;

        private Integer dangerFlag;

        private String confirmText;

        private String riskTip;
    }
}
