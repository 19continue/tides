package com.tides.service.composite;

import com.tides.dto.ProgramOrderCreateDto;
import com.tides.enums.CompositeCheckType;
import com.tides.initialize.impl.composite.AbstractComposite;

/**
 * @description: 生成节目订单验证基类，生成节目订单的相关验证逻辑继承此类
 * @author: 19continue
 **/
public abstract class AbstractProgramCheckHandler extends AbstractComposite<ProgramOrderCreateDto> {
    
    @Override
    public String type() {
        return CompositeCheckType.PROGRAM_ORDER_CREATE_CHECK.getValue();
    }
}
