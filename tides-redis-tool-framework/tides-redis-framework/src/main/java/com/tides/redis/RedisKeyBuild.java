package com.tides.redis;


import com.tides.core.RedisKeyManage;
import com.tides.core.SpringUtil;
import lombok.Getter;

import java.util.Objects;

/**
 * @description: redis key包装
 * @author: 19continue
 **/
@Getter
public final class RedisKeyBuild {
    /**
     * 实际使用的key
     * */
    private final String relKey;

    private RedisKeyBuild(String relKey) {
        this.relKey = relKey;
    }

    /**
     * 构建真实的key
     * @param redisKeyManage key的枚举
     * @param args 占位符的值
     * */
    public static RedisKeyBuild createRedisKey(RedisKeyManage redisKeyManage, Object... args){
        String redisRelKey = String.format(redisKeyManage.getKey(),args);
        String prefix = SpringUtil.getPrefixDistinctionName();
        return new RedisKeyBuild(prefix + "-{" + prefix + "}-" + redisRelKey);
    }
    
    public static String getRedisKey(RedisKeyManage redisKeyManage) {
        String prefix = SpringUtil.getPrefixDistinctionName();
        return prefix + "-{" + prefix + "}-" + redisKeyManage.getKey();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedisKeyBuild that = (RedisKeyBuild) o;
        return relKey.equals(that.relKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relKey);
    }
}
