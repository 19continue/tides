package com.tides.simulation.module;

import com.tides.vo.TicketUserVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @description: 用户登录结果
 * @author: 19continue
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class TickerUserListResultModule extends ApiResponseModule{

    private List<TicketUserVo> data;
}
