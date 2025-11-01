package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @description: 影厅座位模板实体
 */
@Data
@TableName("d_hall_seat")
public class HallSeat extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long cinemaId;

    private Long hallId;

    private Integer rowCode;

    private Integer colCode;

    private String seatNo;

    private String zoneName;

    private String priceLevel;

    private Integer seatType;

    private Integer seatStatus;

    private Integer aisleFlag;

    private Integer coupleFlag;

    private Integer accessibleFlag;

    private Integer vipFlag;

    private Integer repairFlag;

    private Integer sellableFlag;

    private Integer xCoordinate;

    private Integer yCoordinate;
}
