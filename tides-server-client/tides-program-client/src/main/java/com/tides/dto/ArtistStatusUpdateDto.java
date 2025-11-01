package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "ArtistStatusUpdateDto", description = "Artist status update")
public class ArtistStatusUpdateDto {

    @NotNull
    private Long artistId;

    @NotNull
    private Integer status;
}
