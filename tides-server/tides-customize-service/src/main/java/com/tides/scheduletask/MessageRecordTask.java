package com.tides.scheduletask;

import com.tides.BusinessThreadPool;
import com.tides.service.MessageRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @description: 消息记录对账定时任务
 * @author: 19continue
 **/
@Slf4j
@Component
public class MessageRecordTask {
    
    @Autowired
    private MessageRecordService messageRecordService;

    @Scheduled(cron = "0 0/1 * * * ? ")
    public void reconciliationTask(){
        BusinessThreadPool.execute( () -> {
            messageRecordService.executeReconciliationTask();
        });
    }
}
