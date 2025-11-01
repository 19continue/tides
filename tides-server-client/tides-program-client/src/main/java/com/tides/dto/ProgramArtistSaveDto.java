package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "ProgramArtistSaveDto", description = "Program artist relation create or update")
public class ProgramArtistSaveDto {

    private Long id;

    @NotNull
    private Long programId;

    @NotNull
    private Long artistId;

    @NotBlank
    private String roleType;

    private String roleName;

    private Integer sortOrder;

    private Integer displayFlag;
}
