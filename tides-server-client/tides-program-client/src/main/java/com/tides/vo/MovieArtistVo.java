package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(title = "MovieArtistVo", description = "Movie artist")
public class MovieArtistVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long artistId;

    private String artistName;

    private String englishName;

    private String avatar;

    private String region;

    private String profession;

    private String representativeWorks;

    private String roleType;

    private String roleName;

    private Integer sortOrder;
}
