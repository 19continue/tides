package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description: Movie screening price manage vo
 */
@Data
@Schema(title = "MovieScreeningPriceManageVo", description = "Movie screening price manage")
public class MovieScreeningPriceManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long screeningId;

    private Long ticketCategoryId;

    private String priceName;

    private BigDecimal price;

    private Long totalNumber;

    private Long dbRemainNumber;

    private Long redisRemainNumber;
}
