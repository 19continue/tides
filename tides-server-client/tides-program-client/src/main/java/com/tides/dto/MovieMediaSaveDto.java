package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "MovieMediaSaveDto", description = "Movie media create or update")
public class MovieMediaSaveDto {

    private Long id;

    @NotNull
    private Long movieId;

    @NotNull
    private Integer mediaType;

    private String title;

    private String coverUrl;

    @NotBlank
    private String mediaUrl;

    private Integer sortOrder;

    private Integer auditStatus;
}
