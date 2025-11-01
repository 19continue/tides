package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "HallSeatManageVo", description = "Hall seat template manage")
public class HallSeatManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long cinemaId;

    private String cinemaName;

    private Long hallId;

    private String hallName;

    private Integer rowCode;

    private Integer colCode;

    private String seatNo;

    private String zoneName;

    private String priceLevel;

    private Integer seatType;

    private String seatTypeName;

    private Integer seatStatus;

    private String seatStatusName;

    private Integer aisleFlag;

    private Integer coupleFlag;

    private Integer accessibleFlag;

    private Integer vipFlag;

    private Integer repairFlag;

    private Integer sellableFlag;

    private Integer xCoordinate;

    private Integer yCoordinate;

    private Integer status;

    private Date createTime;

    private Date editTime;
}
