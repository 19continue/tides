package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "ProjectApprovalPolicyStatusUpdateDto", description = "Project approval policy status update")
public class ProjectApprovalPolicyStatusUpdateDto {

    @Schema(name = "id", type = "Long", description = "policy id")
    @NotNull
    private Long id;

    @Schema(name = "status", type = "Integer", description = "status")
    @NotNull
    private Integer status;
}
