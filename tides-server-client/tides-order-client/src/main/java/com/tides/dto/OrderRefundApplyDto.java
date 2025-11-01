package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Whole order refund apply dto.
 */
@Data
@Schema(title = "OrderRefundApplyDto", description = "whole order refund apply")
public class OrderRefundApplyDto {

    @Schema(name = "orderNumber", type = "Long", description = "order number", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long orderNumber;

    @Schema(name = "reason", type = "String", description = "refund reason")
    private String reason;
}
