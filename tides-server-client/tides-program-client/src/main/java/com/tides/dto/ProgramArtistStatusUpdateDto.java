package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "ProgramArtistStatusUpdateDto", description = "Program artist relation status update")
public class ProgramArtistStatusUpdateDto {

    @NotNull
    private Long relationId;

    @NotNull
    private Long programId;

    @NotNull
    private Integer status;
}
