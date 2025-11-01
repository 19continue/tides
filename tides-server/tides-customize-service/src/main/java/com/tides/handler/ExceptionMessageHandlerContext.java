package com.tides.handler;

import com.tides.enums.BaseCode;
import com.tides.enums.MessageType;
import com.tides.exception.TidesFrameException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @description: 异常消息处理上下文
 * @author: 19continue
 **/
@Component
public class ExceptionMessageHandlerContext {

    @Autowired
    private List<ExceptionMessageHandler> exceptionMessageHandlerList;
    
    private final Map<MessageType, ExceptionMessageHandler> exceptionMessageHandlerMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        for (ExceptionMessageHandler exceptionMessageHandler : exceptionMessageHandlerList) {
            exceptionMessageHandlerMap.put(exceptionMessageHandler.getMessageType(), exceptionMessageHandler);
        }
    }
    
    public ExceptionMessageHandler getExceptionMessageHandler(MessageType messageType) {
        return Optional.ofNullable(exceptionMessageHandlerMap.get(messageType)).orElseThrow(
                () -> new TidesFrameException(BaseCode.MESSAGE_TYPE_NOT_EXIST));
    }
}
