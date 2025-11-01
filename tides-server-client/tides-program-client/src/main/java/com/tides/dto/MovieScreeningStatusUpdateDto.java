package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "MovieScreeningStatusUpdateDto", description = "Movie screening status update")
public class MovieScreeningStatusUpdateDto {

    @Schema(name = "screeningId", type = "Long", description = "screening id")
    @NotNull
    private Long screeningId;

    @Schema(name = "screeningStatus", type = "Integer", description = "screening status")
    @NotNull
    private Integer screeningStatus;
}
