package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @description: 电影场次后台管理响应对象
 */
@Data
@Schema(title = "MovieScreeningManageVo", description = "电影场次后台管理响应")
public class MovieScreeningManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long movieId;

    private String movieName;

    private String poster;

    private Long programId;

    private Long cinemaId;

    private Long areaId;

    private String cinemaName;

    private String cinemaAddress;

    private String cityName;

    private String districtName;

    private Long hallId;

    private String hallName;

    private String hallType;

    private Date showTime;

    private Date showDayTime;

    private String showWeekTime;

    private Date endTime;

    private String language;

    private String version;

    private BigDecimal lowestPrice;

    private Date stopSellTime;

    private Integer screeningStatus;

    private Long totalNumber;

    private Long dbRemainNumber;

    private Long soldNumber;

    private Long soldSeatNumber;

    private Long lockedNumber;

    private Long occupiedNumber;

    private Long paidOrderCount;

    private Long paidTicketCount;

    private Boolean soldOut;
}
