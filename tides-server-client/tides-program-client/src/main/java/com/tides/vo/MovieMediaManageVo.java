package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "MovieMediaManageVo", description = "Movie media manage")
public class MovieMediaManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long movieId;

    private Integer mediaType;

    private String title;

    private String coverUrl;

    private String mediaUrl;

    private Integer sortOrder;

    private Integer auditStatus;

    private Integer status;

    private Date createTime;

    private Date editTime;
}
