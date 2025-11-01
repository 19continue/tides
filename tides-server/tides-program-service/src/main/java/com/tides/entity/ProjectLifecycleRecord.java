package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("d_project_lifecycle_record")
public class ProjectLifecycleRecord extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long lifecycleId;

    private Long programId;

    private Integer fromLifecycleStatus;

    private Integer toLifecycleStatus;

    private String actionType;

    private String actionName;

    private Long operatorId;

    private String operatorName;

    private String remark;

    private String snapshotJson;
}
