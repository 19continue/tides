package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(title = "VenueManageVo", description = "Venue manage")
public class VenueManageVo implements Serializable {

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

    private Integer status;

    private Date createTime;

    private Date editTime;
}
