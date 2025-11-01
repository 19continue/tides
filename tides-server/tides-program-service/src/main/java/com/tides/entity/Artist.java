package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("d_artist")
public class Artist extends BaseTableData implements Serializable {

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
}
