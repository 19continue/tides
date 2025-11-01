package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: Movie inventory event manage page dto
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "MovieInventoryEventManageDto", description = "Movie inventory event manage page")
public class MovieInventoryEventManageDto extends BasePageDto {

    @Schema(name = "screeningId", type = "Long", description = "screening id")
    private Long screeningId;

    @Schema(name = "ticketCategoryId", type = "Long", description = "ticket category id")
    private Long ticketCategoryId;

    @Schema(name = "seatId", type = "Long", description = "seat id")
    private Long seatId;

    @Schema(name = "eventType", type = "String", description = "event type")
    private String eventType;

    @Schema(name = "sourceType", type = "String", description = "source type")
    private String sourceType;

    @Schema(name = "bizNo", type = "String", description = "business number")
    private String bizNo;
}
