package com.tides.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.tides.dto.BackManageLoginDto;
import com.tides.exception.TidesFrameException;
import com.tides.properties.BackManageProperties;
import com.tides.vo.BackManageLoginVo;
import com.tides.vo.BackManageUserDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @description: 后台管理登录 service
 * @author: 19continue
 **/
@Service
public class BackManageUserService {
    
    @Autowired
    private BackManageProperties backManageProperties;
    

    public BackManageLoginVo login(BackManageLoginDto backManageLoginDto){
        //验证用户信息
        verifyUser(backManageLoginDto);
        //登录
        StpUtil.login(backManageLoginDto.getUsername());
        SaSession session = StpUtil.getSession();
        BackManageUserDetailVo backManageUserDetailVo = new BackManageUserDetailVo();
        backManageUserDetailVo.setUserId("1");
        backManageUserDetailVo.setHomePath("/");
        backManageUserDetailVo.setRealName("19continue");
        backManageUserDetailVo.setDesc("javaup@yeah.net");
        backManageUserDetailVo.setUsername(backManageLoginDto.getUsername());
        backManageUserDetailVo.setAvatar("https://multimedia-javaup.cn/%E5%A4%A7%E9%BA%A6pro/dog.png");
        backManageUserDetailVo.setRoleCodeList(backManageProperties.getRoleCodeList());
        backManageUserDetailVo.setRoleNameList(backManageProperties.getRoleNameList());
        backManageUserDetailVo.setDepartmentId(backManageProperties.getDepartmentId());
        backManageUserDetailVo.setDepartmentName(backManageProperties.getDepartmentName());
        backManageUserDetailVo.setDataScopeType(backManageProperties.getDataScopeType());
        session.set("userDetail", JSON.toJSONString(backManageUserDetailVo));
        BackManageLoginVo backManageLoginVo = new BackManageLoginVo();
        backManageLoginVo.setId("1");
        backManageLoginVo.setRealName("19continue");
        backManageLoginVo.setUsername(backManageLoginDto.getUsername());
        backManageLoginVo.setPassword(backManageLoginDto.getPassword());
        backManageLoginVo.setAccessToken(StpUtil.getTokenValue());
        return backManageLoginVo;
    }
    
    public BackManageUserDetailVo userInfo() {
        Object userDetailObj = StpUtil.getSession().get("userDetail");
        if (userDetailObj == null) {
            throw new TidesFrameException("用户未登录或登录已过期");
        }
        return JSON.parseObject(String.valueOf(userDetailObj), BackManageUserDetailVo.class);
    }
    
    public void verifyUser(BackManageLoginDto backManageLoginDto){
        if (!backManageProperties.getUsername().equals(backManageLoginDto.getUsername())) {
            throw new TidesFrameException("用户名错误");
        }
        if (!backManageProperties.getPassword().equals(backManageLoginDto.getPassword())) {
            throw new TidesFrameException("用户密码错误");
        }
    }
}
