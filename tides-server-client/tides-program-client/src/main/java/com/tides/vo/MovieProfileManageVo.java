package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(title = "MovieProfileManageVo", description = "Movie profile manage")
public class MovieProfileManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long movieId;

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

    private Integer status;

    private Date createTime;

    private Date editTime;
}
