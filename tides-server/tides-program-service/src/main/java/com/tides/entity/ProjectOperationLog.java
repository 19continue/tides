package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("d_project_operation_log")
public class ProjectOperationLog extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

    private String operationModule;

    private String operationType;

    private String operationName;

    private Long operatorId;

    private String operatorName;

    private String operatorAccount;

    private String operationSource;

    private String requestTraceId;

    private String clientIp;

    private String userAgent;

    private String riskLevel;

    private String remark;

    private String beforeSnapshotJson;

    private String afterSnapshotJson;
}
