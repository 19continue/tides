package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("d_movie_profile")
public class MovieProfile extends BaseTableData implements Serializable {

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
}
