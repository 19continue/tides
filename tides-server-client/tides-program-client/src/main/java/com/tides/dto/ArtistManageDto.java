package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "ArtistManageDto", description = "Artist manage query")
public class ArtistManageDto extends BasePageDto {

    private Long artistId;

    private String artistName;

    private Integer artistType;

    private Integer status;
}
