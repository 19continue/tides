package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 演示压测用户批量准备结果。
 */
@Data
@Schema(title = "SimulatorUserPrepareVo", description = "演示压测用户批量准备结果")
public class SimulatorUserPrepareVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "requestedCount", type = "Integer", description = "请求准备用户数量")
    private Integer requestedCount;

    @Schema(name = "createdCount", type = "Integer", description = "本次新建用户数量")
    private Integer createdCount;

    @Schema(name = "reusedCount", type = "Integer", description = "复用已有用户数量")
    private Integer reusedCount;

    @Schema(name = "ticketUserCreatedCount", type = "Integer", description = "本次新建购票人数量")
    private Integer ticketUserCreatedCount;

    @Schema(name = "ticketUserCount", type = "Integer", description = "每个用户准备的购票人数量")
    private Integer ticketUserCount;

    @Schema(name = "userList", type = "List", description = "可直接用于压测的用户列表")
    private List<SimulatorPreparedUserVo> userList;
}
