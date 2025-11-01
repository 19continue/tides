package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "ArtistManageVo", description = "Artist manage")
public class ArtistManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String artistName;

    private String englishName;

    private Integer artistType;

    private String avatar;

    private String region;

    private String profession;

    private String representativeWorks;

    private String description;

    private Integer status;

    private Date createTime;

    private Date editTime;
}
