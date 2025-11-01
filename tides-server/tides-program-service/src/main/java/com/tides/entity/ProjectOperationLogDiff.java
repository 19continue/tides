package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("d_project_operation_log_diff")
public class ProjectOperationLogDiff extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

    private Long operationLogId;

    private String fieldName;

    private String fieldLabel;

    private String beforeValue;

    private String afterValue;
}
