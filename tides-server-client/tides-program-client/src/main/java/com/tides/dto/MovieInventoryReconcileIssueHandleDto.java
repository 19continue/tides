package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "MovieInventoryReconcileIssueHandleDto", description = "Movie inventory reconcile issue handle")
public class MovieInventoryReconcileIssueHandleDto {

    @Schema(name = "id", type = "Long", description = "issue id")
    @NotNull
    private Long id;

    @Schema(name = "screeningId", type = "Long", description = "screening id")
    @NotNull
    private Long screeningId;

    @Schema(name = "issueStatus", type = "Integer", description = "2 processed, 3 ignored")
    @NotNull
    private Integer issueStatus;

    @Schema(name = "handlerId", type = "Long", description = "handler id")
    private Long handlerId;

    @Schema(name = "handleRemark", type = "String", description = "handle remark")
    private String handleRemark;
}
