package com.tides.service.composite.register;


import com.tides.dto.UserRegisterDto;
import com.tides.enums.CompositeCheckType;
import com.tides.initialize.impl.composite.AbstractComposite;


/**
 * @description: 用户注册验证基类，用户注册的相关验证逻辑继承此类
 * @author: 19continue
 **/
public abstract class AbstractUserRegisterCheckHandler extends AbstractComposite<UserRegisterDto> {
    
    @Override
    public String type() {
        return CompositeCheckType.USER_REGISTER_CHECK.getValue();
    }
}
