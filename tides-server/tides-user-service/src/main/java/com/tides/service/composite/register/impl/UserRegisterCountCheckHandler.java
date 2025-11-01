package com.tides.service.composite.register.impl;

import com.tides.dto.UserRegisterDto;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import com.tides.service.composite.register.AbstractUserRegisterCheckHandler;
import com.tides.service.tool.RequestCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @description: 用户注册请求数检查
 * @author: 19continue
 **/
@Component
public class UserRegisterCountCheckHandler extends AbstractUserRegisterCheckHandler {
    
    @Autowired
    private RequestCounter requestCounter;
    
    @Override
    protected void execute(final UserRegisterDto param) {
        boolean result = requestCounter.onRequest();
        if (result) {
            throw new TidesFrameException(BaseCode.USER_REGISTER_FREQUENCY);
        }
    }
    
    @Override
    public Integer executeParentOrder() {
        return 1;
    }
    
    @Override
    public Integer executeTier() {
        return 2;
    }
    
    @Override
    public Integer executeOrder() {
        return 1;
    }
}
