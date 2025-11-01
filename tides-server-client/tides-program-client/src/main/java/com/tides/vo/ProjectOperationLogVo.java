package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Schema(title = "ProjectOperationLogVo", description = "Project operation audit log")
public class ProjectOperationLogVo implements Serializable {

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

    private String diffSummary;

    private List<FieldDiff> diffList;

    private Date createTime;

    @Data
    public static class FieldDiff implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String fieldName;

        private String fieldLabel;

        private String beforeValue;

        private String afterValue;
    }
}
