package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "MovieProfileStatusUpdateDto", description = "Movie profile status update")
public class MovieProfileStatusUpdateDto {

    @NotNull
    private Long profileId;

    @NotNull
    private Long programId;

    @NotNull
    private Integer status;
}
