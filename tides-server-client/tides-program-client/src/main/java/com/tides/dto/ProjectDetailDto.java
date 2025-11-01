package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "ProjectDetailDto", description = "Professional project center detail")
public class ProjectDetailDto {

    @Schema(name = "projectId", type = "Long", description = "project/program id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long projectId;
}
