package com.tides.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.MessageConsumerRecord;
import com.tides.entity.MessageProducerRecord;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.MessageConsumerRecordMapper;
import com.tides.mapper.MessageProducerRecordMapper;
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

/**
 * @description: 定制服务演示数据快照
 * @author: 19continue
 **/
@Service
public class CustomizeDemoDataSnapshotService {

    private static final String SERVICE_NAME = "customize";

    @Value("${demo-data.snapshot-dir:./data/demo-snapshots}")
    private String snapshotDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MessageProducerRecordMapper messageProducerRecordMapper;

    @Autowired
    private MessageConsumerRecordMapper messageConsumerRecordMapper;

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
        CustomizeDemoDataSnapshot snapshot = new CustomizeDemoDataSnapshot();
        snapshot.setSnapshotTime(new Date());
        snapshot.setMessageProducerRecordList(selectAll(messageProducerRecordMapper));
        snapshot.setMessageConsumerRecordList(selectAll(messageConsumerRecordMapper));
        DemoDataSnapshotFileStore.write(snapshotDir, SERVICE_NAME, snapshot);
        DemoDataSnapshotVo status = buildStatus(snapshot);
        DemoDataSnapshotFileStore.writeStatus(snapshotDir, SERVICE_NAME, status);
        return status;
    }

    @Transactional(rollbackFor = Exception.class)
    public DemoDataSnapshotVo restoreSnapshot() {
        CustomizeDemoDataSnapshot snapshot = requireSnapshot();
        physicalDeleteAll("d_message_consumer_record");
        physicalDeleteAll("d_message_producer_record");
        insertAll(messageProducerRecordMapper, snapshot.getMessageProducerRecordList());
        insertAll(messageConsumerRecordMapper, snapshot.getMessageConsumerRecordList());
        return buildStatus(snapshot);
    }

    private CustomizeDemoDataSnapshot getSnapshot() {
        return DemoDataSnapshotFileStore.read(snapshotDir, SERVICE_NAME, CustomizeDemoDataSnapshot.class);
    }

    private CustomizeDemoDataSnapshot requireSnapshot() {
        CustomizeDemoDataSnapshot snapshot = getSnapshot();
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

    private DemoDataSnapshotVo buildStatus(CustomizeDemoDataSnapshot snapshot) {
        DemoDataSnapshotVo vo = new DemoDataSnapshotVo();
        vo.setServiceName(SERVICE_NAME);
        vo.setSnapshotExists(Objects.nonNull(snapshot));
        if (Objects.isNull(snapshot)) {
            vo.setTableCount(2);
            vo.setRowCount(0L);
            vo.setTableRowCount(emptyTableRowCount());
            return vo;
        }
        vo.setSnapshotTime(snapshot.getSnapshotTime());
        Map<String, Integer> rowCountMap = new LinkedHashMap<>();
        rowCountMap.put("d_message_producer_record", size(snapshot.getMessageProducerRecordList()));
        rowCountMap.put("d_message_consumer_record", size(snapshot.getMessageConsumerRecordList()));
        vo.setTableCount(rowCountMap.size());
        vo.setTableRowCount(rowCountMap);
        vo.setRowCount(rowCountMap.values().stream().mapToLong(Integer::longValue).sum());
        return vo;
    }

    private Map<String, Integer> emptyTableRowCount() {
        Map<String, Integer> rowCountMap = new LinkedHashMap<>();
        rowCountMap.put("d_message_producer_record", 0);
        rowCountMap.put("d_message_consumer_record", 0);
        return rowCountMap;
    }

    private int size(Collection<?> collection) {
        return Objects.isNull(collection) ? 0 : collection.size();
    }

    @Data
    public static class CustomizeDemoDataSnapshot {

        private Date snapshotTime;

        private List<MessageProducerRecord> messageProducerRecordList = new ArrayList<>();

        private List<MessageConsumerRecord> messageConsumerRecordList = new ArrayList<>();
    }
}
