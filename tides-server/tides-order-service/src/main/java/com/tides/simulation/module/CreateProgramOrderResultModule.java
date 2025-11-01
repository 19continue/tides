package com.tides.simulation.module;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 用户登录结果
 * @author: 19continue
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateProgramOrderResultModule extends ApiResponseModule{

    private String data;
}
