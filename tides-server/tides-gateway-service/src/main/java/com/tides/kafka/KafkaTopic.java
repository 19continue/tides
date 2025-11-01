package com.tides.kafka;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

/**
 * @description: kafka topic
 * @author: 19continue
 **/
@Data
public class KafkaTopic {
    
    @Value("${spring.kafka.topic:default}")
    private String topic;

}
