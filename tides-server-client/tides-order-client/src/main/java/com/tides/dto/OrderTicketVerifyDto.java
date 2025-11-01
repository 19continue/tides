package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Ticket verification dto.
 */
@Data
@Schema(title = "OrderTicketVerifyDto", description = "ticket verification")
public class OrderTicketVerifyDto {

    @Schema(name = "ticketCode", type = "String", description = "ticket code", requiredMode = RequiredMode.REQUIRED)
    @NotBlank
    private String ticketCode;
}
