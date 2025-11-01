package com.tides.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.entity.PayBill;
import com.tides.entity.RefundBill;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.PayBillMapper;
import com.tides.mapper.RefundBillMapper;
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
 * @description: 支付侧演示数据快照
 * @author: 19continue
 **/
@Service
public class PayDemoDataSnapshotService {

    private static final String SERVICE_NAME = "pay";

    @Value("${demo-data.snapshot-dir:./data/demo-snapshots}")
    private String snapshotDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PayBillMapper payBillMapper;

    @Autowired
    private RefundBillMapper refundBillMapper;

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
        PayDemoDataSnapshot snapshot = new PayDemoDataSnapshot();
        snapshot.setSnapshotTime(new Date());
        snapshot.setPayBillList(selectAll(payBillMapper));
        snapshot.setRefundBillList(selectAll(refundBillMapper));
        DemoDataSnapshotFileStore.write(snapshotDir, SERVICE_NAME, snapshot);
        DemoDataSnapshotVo status = buildStatus(snapshot);
        DemoDataSnapshotFileStore.writeStatus(snapshotDir, SERVICE_NAME, status);
        return status;
    }

    @Transactional(rollbackFor = Exception.class)
    public DemoDataSnapshotVo restoreSnapshot() {
        PayDemoDataSnapshot snapshot = requireSnapshot();
        physicalDeleteAll("d_refund_bill");
        physicalDeleteAll("d_pay_bill");
        insertAll(payBillMapper, snapshot.getPayBillList());
        insertAll(refundBillMapper, snapshot.getRefundBillList());
        return buildStatus(snapshot);
    }

    private PayDemoDataSnapshot getSnapshot() {
        return DemoDataSnapshotFileStore.read(snapshotDir, SERVICE_NAME, PayDemoDataSnapshot.class);
    }

    private PayDemoDataSnapshot requireSnapshot() {
        PayDemoDataSnapshot snapshot = getSnapshot();
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

    private DemoDataSnapshotVo buildStatus(PayDemoDataSnapshot snapshot) {
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
        rowCountMap.put("d_pay_bill", size(snapshot.getPayBillList()));
        rowCountMap.put("d_refund_bill", size(snapshot.getRefundBillList()));
        vo.setTableCount(rowCountMap.size());
        vo.setTableRowCount(rowCountMap);
        vo.setRowCount(rowCountMap.values().stream().mapToLong(Integer::longValue).sum());
        return vo;
    }

    private Map<String, Integer> emptyTableRowCount() {
        Map<String, Integer> rowCountMap = new LinkedHashMap<>();
        rowCountMap.put("d_pay_bill", 0);
        rowCountMap.put("d_refund_bill", 0);
        return rowCountMap;
    }

    private int size(Collection<?> collection) {
        return Objects.isNull(collection) ? 0 : collection.size();
    }

    @Data
    public static class PayDemoDataSnapshot {

        private Date snapshotTime;

        private List<PayBill> payBillList = new ArrayList<>();

        private List<RefundBill> refundBillList = new ArrayList<>();
    }
}
