package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @description: Movie inventory reconcile item vo
 */
@Data
@Schema(title = "MovieInventoryReconcileItemVo", description = "Movie inventory reconcile item")
public class MovieInventoryReconcileItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long ticketCategoryId;

    private String priceName;

    private Long dbRemainNumber;

    private Long dbNoSoldCount;

    private Long dbLockCount;

    private Long dbSoldCount;

    private Long redisNoSoldCount;

    private Long redisLockCount;

    private Long redisSoldCount;

    private Boolean redisCacheReady;

    private Boolean remainConsistent;

    private Boolean seatStatusConsistent;

    private Boolean consistent;

    private String problemDesc;
}
