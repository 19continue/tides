package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "MovieManageVo", description = "Movie manage")
public class MovieManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

    private Long screeningCount;

    private Long soldOutScreeningCount;

    private Long paidOrderCount;

    private Long paidTicketCount;

    private String movieName;

    private String movieAlias;

    private String director;

    private String actors;

    private Integer durationMinutes;

    private String language;

    private String region;

    private Date releaseDate;

    private String poster;

    private String description;

    private Integer status;

    private Date createTime;

    private Date editTime;
}
