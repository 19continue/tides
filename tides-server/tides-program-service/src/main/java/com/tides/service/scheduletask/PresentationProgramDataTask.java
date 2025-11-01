package com.tides.service.scheduletask;

import cn.hutool.core.collection.CollectionUtil;
import com.tides.BusinessThreadPool;
import com.tides.dto.ProgramResetExecuteDto;
import com.tides.mapper.ProgramRecordTaskMapper;
import com.tides.service.ProgramService;
import com.tides.service.init.ProgramElasticsearchInitData;
import com.tides.service.init.ProgramShowTimeRenewal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 节目服务定时任务重置
 * @author: 19continue
 **/
@Slf4j
@Component
public class PresentationProgramDataTask {
    
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    
    @Autowired
    private ProgramService programService;
    
    @Autowired
    private ProgramShowTimeRenewal programShowTimeRenewal;
    
    @Autowired
    private ProgramElasticsearchInitData programElasticsearchInitData;
    
    @Autowired
    private ProgramRecordTaskMapper programRecordTaskMapper;
    
    
    @Scheduled(cron = "0 0 23 * * ?")
    public void executeTask(){
        BusinessThreadPool.execute( () -> {
            try {
                log.info("节目服务定时任务重置执行");
                List<Long> allProgramIdList = programService.getAllProgramIdList();
                if (CollectionUtil.isNotEmpty(allProgramIdList)) {
                    for (Long programId : allProgramIdList) {
                        ProgramResetExecuteDto programResetExecuteDto = new ProgramResetExecuteDto();
                        programResetExecuteDto.setProgramId(programId);
                        //将数据库中的座位和票档数量重置，并删除Redis和本地缓存的所有数据
                        programService.resetExecute(programResetExecuteDto);
                    }
                }
                //真实删除节目对账记录任务数据(潮声普通版本没有这步)
                programRecordTaskMapper.relDelProgramRecordTask();
                //将节目的演出时间更新，并删除相关缓存，如果更新了演出时间，则删除elasticsearch的索引
                programShowTimeRenewal.executeInit(applicationContext);
                //重新初始化elasticsearch数据
                programElasticsearchInitData.executeInit(applicationContext);
            }catch (Exception e) {
                log.error("executeTask error",e);
            }
        });
    }
}
