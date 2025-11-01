package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @description: 电影库存变更流水实体
 */
@Data
@TableName("d_movie_inventory_event")
public class MovieInventoryEvent extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long screeningId;

    private Long programId;

    private Long ticketCategoryId;

    private Long seatId;

    private String eventType;

    private Integer beforeSellStatus;

    private Integer afterSellStatus;

    private Long beforeRemainNumber;

    private Long afterRemainNumber;

    private Long changeCount;

    private String sourceType;

    private String bizNo;

    private Long operatorId;

    private Date eventTime;

    private String remark;
}
