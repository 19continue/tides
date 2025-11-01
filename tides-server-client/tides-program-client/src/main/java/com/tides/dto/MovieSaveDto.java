package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@Schema(title = "MovieSaveDto", description = "Movie create or update")
public class MovieSaveDto {

    @Schema(name = "id", type = "Long", description = "movie id")
    private Long id;

    @Schema(name = "programId", type = "Long", description = "program id")
    @NotNull
    private Long programId;

    @Schema(name = "movieName", type = "String", description = "movie name")
    @NotBlank
    private String movieName;

    @Schema(name = "movieAlias", type = "String", description = "movie alias")
    private String movieAlias;

    @Schema(name = "director", type = "String", description = "director")
    private String director;

    @Schema(name = "actors", type = "String", description = "actors")
    private String actors;

    @Schema(name = "durationMinutes", type = "Integer", description = "duration minutes")
    private Integer durationMinutes;

    @Schema(name = "language", type = "String", description = "language")
    private String language;

    @Schema(name = "region", type = "String", description = "region")
    private String region;

    @Schema(name = "releaseDate", type = "Date", description = "release date")
    private Date releaseDate;

    @Schema(name = "poster", type = "String", description = "poster")
    private String poster;

    @Schema(name = "description", type = "String", description = "description")
    private String description;
}
