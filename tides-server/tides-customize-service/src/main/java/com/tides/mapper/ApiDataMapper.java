package com.tides.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tides.dto.ApiDataDto;
import com.tides.entity.ApiData;
import com.tides.vo.ApiDataVo;

/**
 * @description: api调用记录 mapper
 * @author: 19continue
 **/
public interface ApiDataMapper extends BaseMapper<ApiData> {
    /**
     * 分页查询
     * @param page 分页对象
     * @param apiDataDto 参数
     * @return 分页数据
     * */
    Page<ApiDataVo> pageList(Page<ApiData> page, ApiDataDto apiDataDto);
}
