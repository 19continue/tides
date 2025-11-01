package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(title = "ProgramArtistManageVo", description = "Program artist relation manage")
public class ProgramArtistManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long programId;

    private Long artistId;

    private String artistName;

    private String avatar;

    private String roleType;

    private String roleName;

    private Integer sortOrder;

    private Integer displayFlag;

    private Integer status;

    private Date createTime;

    private Date editTime;
}
