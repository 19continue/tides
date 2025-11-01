package com.tides.service;

import com.alibaba.fastjson.JSONObject;
import com.tides.core.RedisKeyManage;
import com.tides.util.StringUtil;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import com.tides.jwt.TokenUtil;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.vo.UserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @description: token service
 * @author: 19continue
 **/

@Component
public class TokenService {
    
    private static final String TOKEN_SECRET = "CSYZWECHAT";
    
    @Autowired
    private RedisCache redisCache;
    
    public String parseToken(String token){
        String userStr = TokenUtil.parseToken(token,TOKEN_SECRET);
        if (StringUtil.isNotEmpty(userStr)) {
            return JSONObject.parseObject(userStr).getString("userId");
        }
        return null;
    }
    
    public UserVo getUser(String token){
        UserVo userVo = null;
        String userId = parseToken(token);
        if (StringUtil.isNotEmpty(userId)) {
            userVo = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN, userId), UserVo.class);
        }
        return Optional.ofNullable(userVo).orElseThrow(() -> new TidesFrameException(BaseCode.USER_EMPTY));
    }
}
