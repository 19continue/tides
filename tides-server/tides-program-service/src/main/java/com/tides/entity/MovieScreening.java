package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @description: 电影放映场次实体
 */
@Data
@TableName("d_movie_screening")
public class MovieScreening extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long movieId;

    private Long programId;

    private Long cinemaId;

    private Long hallId;

    private Date showTime;

    private Date showDayTime;

    private String showWeekTime;

    private Date endTime;

    private String language;

    private String version;

    private BigDecimal lowestPrice;

    private Date stopSellTime;

    private Integer screeningStatus;
}
