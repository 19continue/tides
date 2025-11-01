package com.tides.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.DepthRule;

/**
 * @description: 深度规则 mapper
 * @author: 19continue
 **/
public interface DepthRuleMapper extends BaseMapper<DepthRule> {
    
    /**
     * 删除所有规则
     * @return 结果
     * */
    int delAll();
}
