package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 演示压测已准备好的用户信息。
 * ID 使用字符串返回，避免前端 JavaScript 大整数精度丢失。
 */
@Data
@Schema(title = "SimulatorPreparedUserVo", description = "演示压测已准备好的用户")
public class SimulatorPreparedUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "userId", type = "String", description = "用户ID")
    private String userId;

    @Schema(name = "mobile", type = "String", description = "手机号")
    private String mobile;

    @Schema(name = "email", type = "String", description = "邮箱")
    private String email;

    @Schema(name = "password", type = "String", description = "密码")
    private String password;

    @Schema(name = "ticketUserIds", type = "List", description = "购票人ID列表")
    private List<String> ticketUserIds;
}
