package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @description: 影院影厅实体
 */
@Data
@TableName("d_hall")
public class Hall extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long cinemaId;

    private String hallName;

    private String hallType;

    private String screenType;

    private String soundType;

    private String screenSize;

    private Integer rowCount;

    private Integer colCount;

    private Integer seatCount;

    private String hallTags;
}
