package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "ProjectManageVo", description = "Professional project center item")
public class ProjectManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;

    private String title;

    private Long parentProgramCategoryId;

    private String parentProgramCategoryName;

    private Long programCategoryId;

    private String programCategoryName;

    private String projectTypeName;

    private Integer lifecycleStatus;

    private String lifecycleStatusName;

    private Integer programStatus;

    private String programStatusName;

    private Integer completenessScore;

    private Long ticketCategoryCount;

    private Long totalRemainNumber;

    private Long screeningCount;

    private Long activeScreeningCount;

    private Long openInventoryIssueCount;

    private Integer riskCount;

    private String nextAction;

    private Date issueTime;

    private Date createTime;

    private Date editTime;
}
