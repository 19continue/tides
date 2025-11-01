package com.tides.controller;

import com.tides.captcha.model.common.ResponseModel;
import com.tides.captcha.model.vo.CaptchaVO;
import com.tides.common.ApiResponse;
import com.tides.service.UserCaptchaService;
import com.tides.vo.CheckNeedCaptchaDataVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 验证码 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/user/captcha")
@Tag(name = "captcha", description = "验证码")
public class UserCaptchaController {
    
    @Autowired
    private UserCaptchaService userCaptchaService;
    
    @Operation(summary  = "检查是否需要验证码")
    @PostMapping(value = "/check/need")
    public ApiResponse<CheckNeedCaptchaDataVo> checkNeedCaptcha(){
        return ApiResponse.ok(userCaptchaService.checkNeedCaptcha());
    }
    
    @Operation(summary  = "获取验证码")
    @PostMapping(value = "/get")
    public ResponseModel getCaptcha(@RequestBody CaptchaVO captchaVO){
        return userCaptchaService.getCaptcha(captchaVO);
    }
    
    @Operation(summary  = "验证验证码")
    @PostMapping(value = "/verify")
    public ResponseModel verifyCaptcha(@RequestBody CaptchaVO captchaVO){
        return userCaptchaService.verifyCaptcha(captchaVO);
    }
}
