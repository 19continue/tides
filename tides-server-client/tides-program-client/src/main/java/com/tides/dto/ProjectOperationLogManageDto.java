package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "ProjectOperationLogManageDto", description = "Project operation audit log page query")
public class ProjectOperationLogManageDto extends BasePageDto {

    @Schema(name = "programId", type = "Long", description = "project/program id")
    private Long programId;

    @Schema(name = "operationModule", type = "String", description = "operation module")
    private String operationModule;

    @Schema(name = "operationType", type = "String", description = "operation type")
    private String operationType;

    @Schema(name = "riskLevel", type = "String", description = "risk level")
    private String riskLevel;

    @Schema(name = "operatorName", type = "String", description = "operator name")
    private String operatorName;

    @Schema(name = "operatorAccount", type = "String", description = "operator login account")
    private String operatorAccount;

    @Schema(name = "operationSource", type = "String", description = "operation source")
    private String operationSource;

    @Schema(name = "diffFieldName", type = "String", description = "changed field name")
    private String diffFieldName;

    @Schema(name = "createTimeStart", type = "String", description = "create time start, yyyy-MM-dd HH:mm:ss")
    private String createTimeStart;

    @Schema(name = "createTimeEnd", type = "String", description = "create time end, yyyy-MM-dd HH:mm:ss")
    private String createTimeEnd;
}
