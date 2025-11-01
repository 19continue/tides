package com.tides.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.Rule;

/**
 * @description: 普通规则 mapper
 * @author: 19continue
 **/
public interface RuleMapper extends BaseMapper<Rule> {
    
    /**
     * 删除所有规则
     * @return 结果
     * */
    int delAll();
}
