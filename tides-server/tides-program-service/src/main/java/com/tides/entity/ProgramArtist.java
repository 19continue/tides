package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("d_program_artist")
public class ProgramArtist extends BaseTableData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

    private Long artistId;

    private String roleType;

    private String roleName;

    private Integer sortOrder;

    private Integer displayFlag;
}
