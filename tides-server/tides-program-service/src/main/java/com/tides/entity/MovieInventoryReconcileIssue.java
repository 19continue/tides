package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @description: Movie inventory reconcile issue entity
 */
@Data
@TableName("d_movie_inventory_reconcile_issue")
public class MovieInventoryReconcileIssue extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long screeningId;

    private Long programId;

    private Long movieId;

    private Long cinemaId;

    private Long hallId;

    private Long ticketCategoryId;

    private String priceName;

    private Integer issueStatus;

    private String issueType;

    private Long dbRemainNumber;

    private Long dbNoSoldCount;

    private Long dbLockCount;

    private Long dbSoldCount;

    private Long redisNoSoldCount;

    private Long redisLockCount;

    private Long redisSoldCount;

    private Integer redisCacheReady;

    private Integer remainConsistent;

    private Integer seatStatusConsistent;

    private String problemDesc;

    private String handleRemark;

    private Long handlerId;

    private Date handleTime;

    private Date lastReconcileTime;
}
