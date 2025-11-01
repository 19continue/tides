package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @description: 广播调用 dto
 * @author: 19continue
 **/
@Data
@Schema(title="BroadcastCallDto", description ="广播调用")
public class BroadcastCallDto {
    
    @Schema(name ="serviceName", type ="String", description ="服务名", requiredMode= RequiredMode.REQUIRED)
    @NotBlank
    private String serviceName;
    
    @Schema(name ="requestBody", type ="String", description ="请求体", requiredMode= RequiredMode.REQUIRED)
    @NotBlank
    private String requestBody;
}
