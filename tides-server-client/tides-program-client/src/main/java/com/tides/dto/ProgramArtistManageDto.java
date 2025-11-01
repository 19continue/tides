package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "ProgramArtistManageDto", description = "Program artist relation manage query")
public class ProgramArtistManageDto extends BasePageDto {

    private Long relationId;

    private Long programId;

    private Long artistId;

    private String roleType;

    private Integer status;
}
