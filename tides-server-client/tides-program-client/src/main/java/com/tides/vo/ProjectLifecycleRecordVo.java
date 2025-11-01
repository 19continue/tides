package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "ProjectLifecycleRecordVo", description = "Project lifecycle transition record")
public class ProjectLifecycleRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long lifecycleId;

    private Long programId;

    private Integer fromLifecycleStatus;

    private String fromLifecycleStatusName;

    private Integer toLifecycleStatus;

    private String toLifecycleStatusName;

    private String actionType;

    private String actionName;

    private Long operatorId;

    private String operatorName;

    private String remark;

    private Date createTime;
}
