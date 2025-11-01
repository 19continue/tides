package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "MovieInventoryReconcileIssueManageVo", description = "Movie inventory reconcile issue manage")
public class MovieInventoryReconcileIssueManageVo implements Serializable {

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

    private String issueStatusName;

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

    private Date createTime;

    private Date editTime;
}
