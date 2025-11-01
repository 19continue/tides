package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MovieInventoryReconcileIssueManageDto", description = "Movie inventory reconcile issue manage page")
public class MovieInventoryReconcileIssueManageDto extends BasePageDto {

    @Schema(name = "screeningId", type = "Long", description = "screening id")
    private Long screeningId;

    @Schema(name = "ticketCategoryId", type = "Long", description = "ticket category id")
    private Long ticketCategoryId;

    @Schema(name = "issueStatus", type = "Integer", description = "issue status")
    private Integer issueStatus;
}
