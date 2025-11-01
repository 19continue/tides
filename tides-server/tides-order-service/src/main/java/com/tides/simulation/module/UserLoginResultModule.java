package com.tides.simulation.module;

import com.tides.vo.UserLoginVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 用户登录结果
 * @author: 19continue
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class UserLoginResultModule extends ApiResponseModule{

    private UserLoginVo data;
}
