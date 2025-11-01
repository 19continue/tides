package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("d_movie_media")
public class MovieMedia extends BaseTableData implements Serializable {

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
}
