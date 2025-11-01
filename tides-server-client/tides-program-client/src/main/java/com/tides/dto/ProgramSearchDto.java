package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @description: 节目搜索 dto
 * @author: 19continue
 **/
@Data
@Schema(title="ProgramSearchDto", description ="节目搜索")
public class ProgramSearchDto extends ProgramPageListDto{
    
    @Schema(name ="content", type ="String", description ="搜索内容")
    private String content;
}
