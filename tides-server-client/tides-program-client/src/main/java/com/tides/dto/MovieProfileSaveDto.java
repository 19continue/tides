package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(title = "MovieProfileSaveDto", description = "Movie profile create or update")
public class MovieProfileSaveDto {

    private Long id;

    @NotNull
    private Long movieId;

    @NotNull
    private Long programId;

    private String genre;

    private Integer releaseStatus;

    private Long wantWatchCount;

    private Long watchedCount;

    private BigDecimal ratingScore;

    private BigDecimal boxOfficeAmount;

    private String producer;

    private String distributor;

    private String ageTips;

    private String longDescription;
}
