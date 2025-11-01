package com.tides.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.core.RedisKeyManage;
import com.tides.entity.Order;
import com.tides.entity.OrderProgram;
import com.tides.entity.OrderTicketUser;
import com.tides.entity.OrderTicketUserRecord;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.OrderMapper;
import com.tides.mapper.OrderProgramMapper;
import com.tides.mapper.OrderTicketUserMapper;
import com.tides.mapper.OrderTicketUserRecordMapper;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.util.DemoDataSnapshotFileStore;
import com.tides.vo.DemoDataSnapshotVo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @description: 订单侧演示数据快照
 * @author: 19continue
 **/
@Service
public class OrderDemoDataSnapshotService {

    private static final String SERVICE_NAME = "order";

    @Autowired
    private RedisCache redisCache;

    @Value("${demo-data.snapshot-dir:./data/demo-snapshots}")
    private String snapshotDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderTicketUserMapper orderTicketUserMapper;

    @Autowired
    private OrderTicketUserRecordMapper orderTicketUserRecordMapper;

    @Autowired
    private OrderProgramMapper orderProgramMapper;

    public DemoDataSnapshotVo status() {
        DemoDataSnapshotVo status = DemoDataSnapshotFileStore.readStatus(snapshotDir, SERVICE_NAME);
        if (Objects.nonNull(status)) {
            return status;
        }
        DemoDataSnapshotVo emptyStatus = buildStatus(null);
        if (DemoDataSnapshotFileStore.exists(snapshotDir, SERVICE_NAME)) {
            emptyStatus.setSnapshotExists(true);
            emptyStatus.setSnapshotTime(DemoDataSnapshotFileStore.lastModifiedTime(snapshotDir, SERVICE_NAME));
        }
        return emptyStatus;
    }

    public DemoDataSnapshotVo createSnapshot() {
        OrderDemoDataSnapshot snapshot = new OrderDemoDataSnapshot();
        snapshot.setSnapshotTime(new Date());
        snapshot.setOrderList(selectAll(orderMapper));
        snapshot.setOrderTicketUserList(selectAll(orderTicketUserMapper));
        snapshot.setOrderTicketUserRecordList(selectAll(orderTicketUserRecordMapper));
        snapshot.setOrderProgramList(selectAll(orderProgramMapper));
        DemoDataSnapshotFileStore.write(snapshotDir, SERVICE_NAME, snapshot);
        DemoDataSnapshotVo status = buildStatus(snapshot);
        DemoDataSnapshotFileStore.writeStatus(snapshotDir, SERVICE_NAME, status);
        return status;
    }

    @Transactional(rollbackFor = Exception.class)
    public DemoDataSnapshotVo restoreSnapshot() {
        OrderDemoDataSnapshot snapshot = requireSnapshot();
        physicalDeleteAll("d_order_program");
        physicalDeleteAll("d_order_ticket_user_record");
        physicalDeleteAll("d_order_ticket_user");
        physicalDeleteAll("d_order");
        insertAll(orderMapper, snapshot.getOrderList());
        insertAll(orderTicketUserMapper, snapshot.getOrderTicketUserList());
        insertAll(orderTicketUserRecordMapper, snapshot.getOrderTicketUserRecordList());
        insertAll(orderProgramMapper, snapshot.getOrderProgramList());
        clearOrderRedisCache();
        return buildStatus(snapshot);
    }

    private OrderDemoDataSnapshot getSnapshot() {
        return DemoDataSnapshotFileStore.read(snapshotDir, SERVICE_NAME, OrderDemoDataSnapshot.class);
    }

    private OrderDemoDataSnapshot requireSnapshot() {
        OrderDemoDataSnapshot snapshot = getSnapshot();
        if (Objects.isNull(snapshot)) {
            throw new TidesFrameException(BaseCode.DEMO_DATA_SNAPSHOT_NOT_EXIST);
        }
        return snapshot;
    }

    private <T> List<T> selectAll(BaseMapper<T> mapper) {
        List<T> list = mapper.selectList(null);
        return Objects.isNull(list) ? new ArrayList<>() : list;
    }

    private void physicalDeleteAll(String tableName) {
        jdbcTemplate.update("DELETE FROM " + tableName);
    }

    private <T> void insertAll(BaseMapper<T> mapper, List<T> list) {
        if (Objects.isNull(list) || list.isEmpty()) {
            return;
        }
        for (T item : list) {
            mapper.insert(item);
        }
    }

    private DemoDataSnapshotVo buildStatus(OrderDemoDataSnapshot snapshot) {
        DemoDataSnapshotVo vo = new DemoDataSnapshotVo();
        vo.setServiceName(SERVICE_NAME);
        vo.setSnapshotExists(Objects.nonNull(snapshot));
        if (Objects.isNull(snapshot)) {
            vo.setTableCount(4);
            vo.setRowCount(0L);
            vo.setTableRowCount(emptyTableRowCount());
            return vo;
        }
        vo.setSnapshotTime(snapshot.getSnapshotTime());
        Map<String, Integer> rowCountMap = new LinkedHashMap<>();
        rowCountMap.put("d_order", size(snapshot.getOrderList()));
        rowCountMap.put("d_order_ticket_user", size(snapshot.getOrderTicketUserList()));
        rowCountMap.put("d_order_ticket_user_record", size(snapshot.getOrderTicketUserRecordList()));
        rowCountMap.put("d_order_program", size(snapshot.getOrderProgramList()));
        vo.setTableCount(rowCountMap.size());
        vo.setTableRowCount(rowCountMap);
        vo.setRowCount(rowCountMap.values().stream().mapToLong(Integer::longValue).sum());
        return vo;
    }

    private Map<String, Integer> emptyTableRowCount() {
        Map<String, Integer> rowCountMap = new LinkedHashMap<>();
        rowCountMap.put("d_order", 0);
        rowCountMap.put("d_order_ticket_user", 0);
        rowCountMap.put("d_order_ticket_user_record", 0);
        rowCountMap.put("d_order_program", 0);
        return rowCountMap;
    }

    private int size(Collection<?> collection) {
        return Objects.isNull(collection) ? 0 : collection.size();
    }

    private void clearOrderRedisCache() {
        List<String> patterns = List.of(
                RedisKeyBuild.createRedisKey(RedisKeyManage.ACCOUNT_ORDER_COUNT_ALL).getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.ORDER_MQ, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.DISCARD_ORDER, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_ORDER_CREATE_IDEMPOTENT, "*", "*").getRelKey()
        );
        for (String pattern : patterns) {
            Set<String> keys = redisCache.keys(pattern);
            if (Objects.nonNull(keys) && !keys.isEmpty()) {
                redisCache.getInstance().delete(keys);
            }
        }
    }

    @Data
    public static class OrderDemoDataSnapshot {

        private Date snapshotTime;

        private List<Order> orderList = new ArrayList<>();

        private List<OrderTicketUser> orderTicketUserList = new ArrayList<>();

        private List<OrderTicketUserRecord> orderTicketUserRecordList = new ArrayList<>();

        private List<OrderProgram> orderProgramList = new ArrayList<>();
    }
}
