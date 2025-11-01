package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("d_venue")
public class Venue extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long areaId;

    private String venueName;

    private Integer venueType;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String phone;

    private String trafficGuide;

    private String entryGuide;
}
