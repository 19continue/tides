package com.tides.service.kafka;

import com.alibaba.fastjson.JSON;
import com.tides.core.RedisKeyManage;
import com.tides.domain.DiscardOrder;
import com.tides.domain.OrderCreateMq;
import com.tides.dto.OrderTicketUserCreateDto;
import com.tides.enums.DiscardOrderReason;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.service.OrderService;
import com.tides.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tides.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

/**
 * @description: kafka 创建订单 消费
 * @author: 19continue
 **/
@Slf4j
@Component
public class CreateOrderConsumer {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${tides.order.create-message-max-delay-ms:60000}")
    private long messageDelayTime;

    @KafkaListener(
            topics = {SPRING_INJECT_PREFIX_DISTINCTION_NAME+"-"+"${spring.kafka.topic:create_order}"},
            concurrency = "${spring.kafka.listener.concurrency:3}")
    public void consumerOrderMessage(ConsumerRecord<String,String> consumerRecord){
        String value = consumerRecord.value();
        if (StringUtil.isEmpty(value)) {
            return;
        }
        OrderCreateMq orderCreateMq = JSON.parseObject(value, OrderCreateMq.class);
        try {
            long createOrderTimeTimestamp = orderCreateMq.getCreateOrderTime().getTime();
            
            long currentTimeTimestamp = System.currentTimeMillis();
            
            long delayTime = currentTimeTimestamp - createOrderTimeTimestamp;

            log.debug("消费到kafka的创建订单消息 orderNumber:{} 延迟时间:{}毫秒",
                    orderCreateMq.getOrderNumber(), delayTime);

            if (currentTimeTimestamp - createOrderTimeTimestamp > messageDelayTime) {
                Map<Long, List<OrderTicketUserCreateDto>> orderTicketUserSeatList =
                        orderCreateMq.getOrderTicketUserCreateDtoList().stream().collect(Collectors.groupingBy(OrderTicketUserCreateDto::getTicketCategoryId));
                //key: 节目票档id value: 座位id集合
                Map<Long,List<Long>> seatMap = new HashMap<>(orderTicketUserSeatList.size());
                orderTicketUserSeatList.forEach((k,v) -> {
                    seatMap.put(k,v.stream().map(OrderTicketUserCreateDto::getSeatId).collect(Collectors.toList()));
                });
                log.warn("消费到kafka的创建订单消息延迟时间大于了 {} 毫秒 此订单消息被丢弃 订单号:{} 座位信息:{}",
                        messageDelayTime, orderCreateMq.getOrderNumber(), JSON.toJSONString(seatMap));
                //将延迟丢弃的订单放入redis中
                redisCache.leftPushForList(RedisKeyBuild.createRedisKey(RedisKeyManage.DISCARD_ORDER,
                        orderCreateMq.getProgramId()),new DiscardOrder(orderCreateMq, DiscardOrderReason.CONSUMER_DELAY.getCode(), "消费延迟"));
                //上报指标给Promethus
                meterRegistry.counter("tides_order_create_fail_total", "reason", "CREATE_ORDER_DELAY", "programId", String.valueOf(orderCreateMq.getProgramId())).increment();
            }else {
                String orderNumber = orderService.createMq(orderCreateMq);
                log.debug("消费到kafka的创建订单消息 创建订单成功 订单号:{}", orderNumber);
            }
        }catch (Exception e) {
            //将创建失败的订单放入redis中
            redisCache.leftPushForList(RedisKeyBuild.createRedisKey(RedisKeyManage.DISCARD_ORDER,
                    orderCreateMq.getProgramId()),new DiscardOrder(orderCreateMq, DiscardOrderReason.CREATE_ORDER_FAIL.getCode(), e.getMessage()));
            //上报指标给Promethus
            meterRegistry.counter("tides_order_create_fail_total", "reason", "CREATE_ORDER_FAIL", "programId", String.valueOf(orderCreateMq.getProgramId())).increment();
            log.error("处理消费到kafka的创建订单消息失败 error",e);
        }
    }
}
