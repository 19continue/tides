package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "MovieMediaStatusUpdateDto", description = "Movie media status update")
public class MovieMediaStatusUpdateDto {

    @NotNull
    private Long mediaId;

    @NotNull
    private Long movieId;

    @NotNull
    private Integer status;
}
