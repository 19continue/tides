package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @description: Movie inventory reconcile vo
 */
@Data
@Schema(title = "MovieInventoryReconcileVo", description = "Movie inventory reconcile")
public class MovieInventoryReconcileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long screeningId;

    private Long programId;

    private Long movieId;

    private Long cinemaId;

    private Long hallId;

    private Boolean consistent;

    private Long totalDbRemainNumber;

    private Long totalDbNoSoldCount;

    private Long totalDbLockCount;

    private Long totalDbSoldCount;

    private Long totalRedisNoSoldCount;

    private Long totalRedisLockCount;

    private Long totalRedisSoldCount;

    private Integer issueCount;

    private List<MovieInventoryReconcileItemVo> itemList;
}
