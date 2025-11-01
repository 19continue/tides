package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "MovieStatusUpdateDto", description = "Movie status update")
public class MovieStatusUpdateDto {

    @Schema(name = "movieId", type = "Long", description = "movie id")
    @NotNull
    private Long movieId;

    @Schema(name = "programId", type = "Long", description = "program id")
    @NotNull
    private Long programId;

    @Schema(name = "status", type = "Integer", description = "status")
    @NotNull
    private Integer status;
}
