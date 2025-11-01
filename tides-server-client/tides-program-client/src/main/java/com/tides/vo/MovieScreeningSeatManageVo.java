package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description: Movie screening seat manage vo
 */
@Data
@Schema(title = "MovieScreeningSeatManageVo", description = "Movie screening seat manage")
public class MovieScreeningSeatManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long screeningId;

    private Long programId;

    private Long ticketCategoryId;

    private Integer rowCode;

    private Integer colCode;

    private String seatNo;

    private String zoneName;

    private String priceLevel;

    private Integer seatType;

    private String seatTypeName;

    private BigDecimal price;

    private Integer dbSellStatus;

    private String dbSellStatusName;

    private Integer redisSellStatus;

    private String redisSellStatusName;

    private Integer aisleFlag;

    private Integer coupleFlag;

    private Integer accessibleFlag;

    private Integer vipFlag;

    private Integer repairFlag;

    private Integer sellableFlag;
}
