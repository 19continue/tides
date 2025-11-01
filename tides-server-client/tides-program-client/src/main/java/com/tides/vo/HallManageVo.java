package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "HallManageVo", description = "Hall manage")
public class HallManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long cinemaId;

    private String cinemaName;

    private String hallName;

    private String hallType;

    private String screenType;

    private String soundType;

    private String screenSize;

    private Integer rowCount;

    private Integer colCount;

    private Integer seatCount;

    private String hallTags;

    private Integer status;

    private Date createTime;

    private Date editTime;
}
