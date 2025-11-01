package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @description: 电影影片实体
 */
@Data
@TableName("d_movie")
public class Movie extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

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
}
