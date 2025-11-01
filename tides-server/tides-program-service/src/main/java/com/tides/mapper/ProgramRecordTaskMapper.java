package com.tides.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.ProgramRecordTask;

/**
 * @description: 节目对账记录任务 mapper
 * @author: 19continue
 **/
public interface ProgramRecordTaskMapper extends BaseMapper<ProgramRecordTask> {
    /**
     * 真实删除节目对账记录任务数据
     * @return 结果
     * */
    Integer relDelProgramRecordTask();
}
