package com.tides.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.core.RedisKeyManage;
import com.tides.entity.MovieInventoryEvent;
import com.tides.entity.MovieInventoryReconcileIssue;
import com.tides.entity.MovieScreeningPrice;
import com.tides.entity.MovieScreeningSeat;
import com.tides.entity.ProgramRecordTask;
import com.tides.entity.Seat;
import com.tides.entity.TicketCategory;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.MovieInventoryEventMapper;
import com.tides.mapper.MovieInventoryReconcileIssueMapper;
import com.tides.mapper.MovieScreeningPriceMapper;
import com.tides.mapper.MovieScreeningSeatMapper;
import com.tides.mapper.ProgramRecordTaskMapper;
import com.tides.mapper.SeatMapper;
import com.tides.mapper.TicketCategoryMapper;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.util.DemoDataSnapshotFileStore;
import com.tides.vo.DemoDataSnapshotVo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
 * @description: 节目侧演示数据快照
 * @author: 19continue
 **/
@Slf4j
@Service
public class ProgramDemoDataSnapshotService {

    private static final String SERVICE_NAME = "program";

    @Autowired
    private RedisCache redisCache;

    @Value("${demo-data.snapshot-dir:./data/demo-snapshots}")
    private String snapshotDir;

    @Autowired
    private ProgramService programService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private TicketCategoryMapper ticketCategoryMapper;

    @Autowired
    private MovieScreeningPriceMapper movieScreeningPriceMapper;

    @Autowired
    private MovieScreeningSeatMapper movieScreeningSeatMapper;

    @Autowired
    private MovieInventoryEventMapper movieInventoryEventMapper;

    @Autowired
    private MovieInventoryReconcileIssueMapper movieInventoryReconcileIssueMapper;

    @Autowired
    private ProgramRecordTaskMapper programRecordTaskMapper;

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
        ProgramDemoDataSnapshot snapshot = new ProgramDemoDataSnapshot();
        snapshot.setSnapshotTime(new Date());
        snapshot.setSeatList(selectAll(seatMapper));
        snapshot.setTicketCategoryList(selectAll(ticketCategoryMapper));
        snapshot.setMovieScreeningPriceList(selectAll(movieScreeningPriceMapper));
        snapshot.setMovieScreeningSeatList(selectAll(movieScreeningSeatMapper));
        snapshot.setMovieInventoryEventList(selectAll(movieInventoryEventMapper));
        snapshot.setMovieInventoryReconcileIssueList(selectAll(movieInventoryReconcileIssueMapper));
        snapshot.setProgramRecordTaskList(selectAll(programRecordTaskMapper));
        DemoDataSnapshotFileStore.write(snapshotDir, SERVICE_NAME, snapshot);
        DemoDataSnapshotVo status = buildStatus(snapshot);
        DemoDataSnapshotFileStore.writeStatus(snapshotDir, SERVICE_NAME, status);
        return status;
    }

    @Transactional(rollbackFor = Exception.class)
    public DemoDataSnapshotVo restoreSnapshot() {
        ProgramDemoDataSnapshot snapshot = requireSnapshot();
        physicalDeleteAll("d_program_record_task");
        physicalDeleteAll("d_movie_inventory_reconcile_issue");
        physicalDeleteAll("d_movie_inventory_event");
        physicalDeleteAll("d_movie_screening_seat");
        physicalDeleteAll("d_movie_screening_price");
        physicalDeleteAll("d_seat");
        physicalDeleteAll("d_ticket_category");
        insertAll(ticketCategoryMapper, snapshot.getTicketCategoryList());
        insertAll(seatMapper, snapshot.getSeatList());
        insertAll(movieScreeningPriceMapper, snapshot.getMovieScreeningPriceList());
        insertAll(movieScreeningSeatMapper, snapshot.getMovieScreeningSeatList());
        insertAll(movieInventoryEventMapper, snapshot.getMovieInventoryEventList());
        insertAll(movieInventoryReconcileIssueMapper, snapshot.getMovieInventoryReconcileIssueList());
        insertAll(programRecordTaskMapper, snapshot.getProgramRecordTaskList());
        clearProgramRedisCache();
        clearProgramLocalCache();
        return buildStatus(snapshot);
    }

    private ProgramDemoDataSnapshot getSnapshot() {
        return DemoDataSnapshotFileStore.read(snapshotDir, SERVICE_NAME, ProgramDemoDataSnapshot.class);
    }

    private ProgramDemoDataSnapshot requireSnapshot() {
        ProgramDemoDataSnapshot snapshot = getSnapshot();
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

    private DemoDataSnapshotVo buildStatus(ProgramDemoDataSnapshot snapshot) {
        DemoDataSnapshotVo vo = new DemoDataSnapshotVo();
        vo.setServiceName(SERVICE_NAME);
        vo.setSnapshotExists(Objects.nonNull(snapshot));
        if (Objects.isNull(snapshot)) {
            vo.setTableCount(7);
            vo.setRowCount(0L);
            vo.setTableRowCount(emptyTableRowCount());
            return vo;
        }
        vo.setSnapshotTime(snapshot.getSnapshotTime());
        Map<String, Integer> rowCountMap = new LinkedHashMap<>();
        rowCountMap.put("d_seat", size(snapshot.getSeatList()));
        rowCountMap.put("d_ticket_category", size(snapshot.getTicketCategoryList()));
        rowCountMap.put("d_movie_screening_price", size(snapshot.getMovieScreeningPriceList()));
        rowCountMap.put("d_movie_screening_seat", size(snapshot.getMovieScreeningSeatList()));
        rowCountMap.put("d_movie_inventory_event", size(snapshot.getMovieInventoryEventList()));
        rowCountMap.put("d_movie_inventory_reconcile_issue", size(snapshot.getMovieInventoryReconcileIssueList()));
        rowCountMap.put("d_program_record_task", size(snapshot.getProgramRecordTaskList()));
        vo.setTableCount(rowCountMap.size());
        vo.setTableRowCount(rowCountMap);
        vo.setRowCount(rowCountMap.values().stream().mapToLong(Integer::longValue).sum());
        return vo;
    }

    private Map<String, Integer> emptyTableRowCount() {
        Map<String, Integer> rowCountMap = new LinkedHashMap<>();
        rowCountMap.put("d_seat", 0);
        rowCountMap.put("d_ticket_category", 0);
        rowCountMap.put("d_movie_screening_price", 0);
        rowCountMap.put("d_movie_screening_seat", 0);
        rowCountMap.put("d_movie_inventory_event", 0);
        rowCountMap.put("d_movie_inventory_reconcile_issue", 0);
        rowCountMap.put("d_program_record_task", 0);
        return rowCountMap;
    }

    private int size(Collection<?> collection) {
        return Objects.isNull(collection) ? 0 : collection.size();
    }

    private void clearProgramRedisCache() {
        List<String> patterns = List.of(
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_GROUP, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SHOW_TIME, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH, "*", "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH, "*", "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_SOLD_RESOLUTION_HASH, "*", "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_TICKET_CATEGORY_LIST, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_TICKET_REMAIN_NUMBER_HASH_RESOLUTION, "*", "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_RECORD, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_RECORD_FINISH, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.MOVIE_SCREENING_SEAT_NO_SOLD_RESOLUTION_HASH, "*", "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.MOVIE_SCREENING_SEAT_LOCK_RESOLUTION_HASH, "*", "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.MOVIE_SCREENING_SEAT_SOLD_RESOLUTION_HASH, "*", "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.MOVIE_SCREENING_TICKET_REMAIN_NUMBER_HASH_RESOLUTION, "*", "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.MOVIE_SCREENING_RECORD, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.DISCARD_ORDER, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.ACCOUNT_ORDER_COUNT_ALL).getRelKey()
        );
        deleteByPatterns(patterns);
    }

    private void deleteByPatterns(List<String> patterns) {
        for (String pattern : patterns) {
            Set<String> keys = redisCache.keys(pattern);
            if (Objects.nonNull(keys) && !keys.isEmpty()) {
                redisCache.getInstance().delete(keys);
            }
        }
    }

    private void clearProgramLocalCache() {
        try {
            List<Long> programIdList = programService.getAllProgramIdList();
            for (Long programId : programIdList) {
                programService.delLocalCache(programId);
            }
        } catch (Exception exception) {
            log.warn("clear program local cache error", exception);
        }
    }

    @Data
    public static class ProgramDemoDataSnapshot {

        private Date snapshotTime;

        private List<Seat> seatList = new ArrayList<>();

        private List<TicketCategory> ticketCategoryList = new ArrayList<>();

        private List<MovieScreeningPrice> movieScreeningPriceList = new ArrayList<>();

        private List<MovieScreeningSeat> movieScreeningSeatList = new ArrayList<>();

        private List<MovieInventoryEvent> movieInventoryEventList = new ArrayList<>();

        private List<MovieInventoryReconcileIssue> movieInventoryReconcileIssueList = new ArrayList<>();

        private List<ProgramRecordTask> programRecordTaskList = new ArrayList<>();
    }
}
