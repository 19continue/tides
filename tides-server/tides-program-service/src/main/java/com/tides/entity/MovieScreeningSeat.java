package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description: 电影场次座位库存实体
 */
@Data
@TableName("d_movie_screening_seat")
public class MovieScreeningSeat extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long screeningId;

    private Long programId;

    private Long hallSeatId;

    private Long ticketCategoryId;

    private Integer rowCode;

    private Integer colCode;

    private String seatNo;

    private String zoneName;

    private String priceLevel;

    private Integer seatType;

    private BigDecimal price;

    private Integer sellStatus;

    private Integer aisleFlag;

    private Integer coupleFlag;

    private Integer accessibleFlag;

    private Integer vipFlag;

    private Integer repairFlag;

    private Integer sellableFlag;
}
