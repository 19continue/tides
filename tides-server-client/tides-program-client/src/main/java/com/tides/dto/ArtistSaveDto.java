package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(title = "ArtistSaveDto", description = "Artist create or update")
public class ArtistSaveDto {

    private Long id;

    @NotBlank
    private String artistName;

    private String englishName;

    private Integer artistType;

    private String avatar;

    private String region;

    private String profession;

    private String representativeWorks;

    private String description;
}
