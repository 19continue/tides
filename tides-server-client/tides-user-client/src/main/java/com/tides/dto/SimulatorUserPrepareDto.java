package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 演示压测用户批量准备入参。
 * 只用于自有压测环境，普通注册仍然走验证码和频控链路。
 */
@Data
@Schema(title = "SimulatorUserPrepareDto", description = "演示压测用户批量准备")
public class SimulatorUserPrepareDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "count", type = "Integer", description = "需要准备的用户数量")
    private Integer count;

    @Schema(name = "startIndex", type = "Long", description = "生成手机号和邮箱使用的起始序号")
    private Long startIndex;

    @Schema(name = "mobilePrefix", type = "String", description = "手机号前缀，剩余位数由序号补齐")
    private String mobilePrefix;

    @Schema(name = "emailDomain", type = "String", description = "自动生成邮箱域名")
    private String emailDomain;

    @Schema(name = "password", type = "String", description = "生成用户的登录密码")
    private String password;

    @Schema(name = "ticketUserCount", type = "Integer", description = "每个用户需要准备的购票人数量")
    private Integer ticketUserCount;

    @Schema(name = "relNamePrefix", type = "String", description = "购票人姓名前缀")
    private String relNamePrefix;

    @Schema(name = "overwritePassword", type = "Boolean", description = "用户已存在时是否覆盖密码")
    private Boolean overwritePassword;
}
