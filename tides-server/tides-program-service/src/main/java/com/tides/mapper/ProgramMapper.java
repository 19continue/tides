package com.tides.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tides.dto.ProgramListDto;
import com.tides.dto.ProgramPageListDto;
import com.tides.dto.ProgramSearchDto;
import com.tides.entity.Program;
import com.tides.entity.ProgramJoinShowTime;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description: 节目 mapper
 * @author: 19continue
 **/
public interface ProgramMapper extends BaseMapper<Program> {
    
    /**
     * 主页查询
     * @param programListDto 参数
     * @return 结果
     * */
    List<Program> selectHomeList(@Param("programListDto")ProgramListDto programListDto);
    
    /**
     * 分页查询
     * @param page 分页对象
     * @param programPageListDto 参数
     * @return 结果
     * */
    IPage<ProgramJoinShowTime> selectPage(IPage<ProgramJoinShowTime> page, 
                                          @Param("programPageListDto")ProgramPageListDto programPageListDto);

    /**
     * 搜索分页查询
     * @param page 分页对象
     * @param programSearchDto 参数
     * @return 结果
     * */
    IPage<ProgramJoinShowTime> selectSearchPage(IPage<ProgramJoinShowTime> page,
                                                @Param("programSearchDto")ProgramSearchDto programSearchDto);
}
