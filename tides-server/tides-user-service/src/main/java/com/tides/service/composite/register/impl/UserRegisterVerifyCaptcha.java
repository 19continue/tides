package com.tides.service.composite.register.impl;

import com.tides.captcha.model.common.ResponseModel;
import com.tides.captcha.model.vo.CaptchaVO;
import com.tides.core.RedisKeyManage;
import com.tides.util.StringUtil;
import com.tides.dto.UserRegisterDto;
import com.tides.enums.BaseCode;
import com.tides.enums.VerifyCaptcha;
import com.tides.exception.TidesFrameException;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.service.CaptchaHandle;
import com.tides.service.composite.register.AbstractUserRegisterCheckHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @description: 用户注册检查
 * @author: 19continue
 **/
@Slf4j
@Component
public class UserRegisterVerifyCaptcha extends AbstractUserRegisterCheckHandler {
    
    @Autowired
    private CaptchaHandle captchaHandle;
    
    @Autowired
    private RedisCache redisCache;
    
    @Override
    protected void execute(UserRegisterDto param) {
        String password = param.getPassword();
        String confirmPassword = param.getConfirmPassword();
        if (!password.equals(confirmPassword)) {
            throw new TidesFrameException(BaseCode.TWO_PASSWORDS_DIFFERENT);
        }
        String verifyCaptcha = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.VERIFY_CAPTCHA_ID,param.getCaptchaId()), String.class);
        if (StringUtil.isEmpty(verifyCaptcha)) {
            throw new TidesFrameException(BaseCode.VERIFY_CAPTCHA_ID_NOT_EXIST);
        }
        if (VerifyCaptcha.YES.getValue().equals(verifyCaptcha)) {
            if (StringUtil.isEmpty(param.getCaptchaVerification())) {
                throw new TidesFrameException(BaseCode.VERIFY_CAPTCHA_EMPTY);
            }
            log.info("传入的captchaVerification:{}",param.getCaptchaVerification());
            CaptchaVO captchaVO = new CaptchaVO();
            captchaVO.setCaptchaVerification(param.getCaptchaVerification());
            ResponseModel responseModel = captchaHandle.verification(captchaVO);
            if (!responseModel.isSuccess()) {
                throw new TidesFrameException(responseModel.getRepCode(),responseModel.getRepMsg());
            }
        }
    }
    
    @Override
    public Integer executeParentOrder() {
        return 0;
    }
    
    @Override
    public Integer executeTier() {
        return 1;
    }
    
    @Override
    public Integer executeOrder() {
        return 1;
    }
}
