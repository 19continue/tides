package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @description: Movie inventory event manage vo
 */
@Data
@Schema(title = "MovieInventoryEventManageVo", description = "Movie inventory event manage")
public class MovieInventoryEventManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long screeningId;

    private Long programId;

    private Long ticketCategoryId;

    private Long seatId;

    private String eventType;

    private Integer beforeSellStatus;

    private String beforeSellStatusName;

    private Integer afterSellStatus;

    private String afterSellStatusName;

    private Long beforeRemainNumber;

    private Long afterRemainNumber;

    private Long changeCount;

    private String sourceType;

    private String bizNo;

    private Long operatorId;

    private Date eventTime;

    private String remark;
}
