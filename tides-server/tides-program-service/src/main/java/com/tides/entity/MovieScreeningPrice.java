package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description: 电影场次票价实体
 */
@Data
@TableName("d_movie_screening_price")
public class MovieScreeningPrice extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long screeningId;

    private Long ticketCategoryId;

    private String priceName;

    private BigDecimal price;

    private Long totalNumber;

    private Long remainNumber;
}
