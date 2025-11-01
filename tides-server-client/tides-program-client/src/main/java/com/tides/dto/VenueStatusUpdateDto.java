package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "VenueStatusUpdateDto", description = "Venue status update")
public class VenueStatusUpdateDto {

    @NotNull
    private Long venueId;

    @NotNull
    private Integer status;
}
