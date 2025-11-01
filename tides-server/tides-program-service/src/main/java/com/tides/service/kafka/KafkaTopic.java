package com.tides.service.kafka;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @description: kafka topic
 * @author: 19continue
 **/
@Data
@Component
public class KafkaTopic {
    
    @Value("${spring.kafka.topic:default}")
    private String topic;

}
