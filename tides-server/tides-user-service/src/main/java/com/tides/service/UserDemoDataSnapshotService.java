package com.tides.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tides.core.RedisKeyManage;
import com.tides.entity.TicketUser;
import com.tides.entity.User;
import com.tides.entity.UserEmail;
import com.tides.entity.UserMobile;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.TicketUserMapper;
import com.tides.mapper.UserEmailMapper;
import com.tides.mapper.UserMapper;
import com.tides.mapper.UserMobileMapper;
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
 * @description: 用户侧演示数据基准恢复
 * @author: 19continue
 **/
@Service
public class UserDemoDataSnapshotService {

    private static final String SERVICE_NAME = "user";

    @Value("${demo-data.snapshot-dir:./data/demo-snapshots}")
    private String snapshotDir;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserMobileMapper userMobileMapper;

    @Autowired
    private UserEmailMapper userEmailMapper;

    @Autowired
    private TicketUserMapper ticketUserMapper;

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

    @Transactional(rollbackFor = Exception.class)
    public DemoDataSnapshotVo restoreSnapshot() {
        UserDemoDataSnapshot snapshot = requireSnapshot();
        physicalDeleteAll("d_ticket_user");
        physicalDeleteAll("d_user_email");
        physicalDeleteAll("d_user_mobile");
        physicalDeleteAll("d_user");
        insertAll(userMapper, snapshot.getUserList());
        insertAll(userMobileMapper, snapshot.getUserMobileList());
        insertAll(userEmailMapper, snapshot.getUserEmailList());
        insertAll(ticketUserMapper, snapshot.getTicketUserList());
        clearUserRedisCache();
        return buildStatus(snapshot);
    }

    private UserDemoDataSnapshot getSnapshot() {
        return DemoDataSnapshotFileStore.read(snapshotDir, SERVICE_NAME, UserDemoDataSnapshot.class);
    }

    private UserDemoDataSnapshot requireSnapshot() {
        UserDemoDataSnapshot snapshot = getSnapshot();
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

    private DemoDataSnapshotVo buildStatus(UserDemoDataSnapshot snapshot) {
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
        rowCountMap.put("d_user", size(snapshot.getUserList()));
        rowCountMap.put("d_user_mobile", size(snapshot.getUserMobileList()));
        rowCountMap.put("d_user_email", size(snapshot.getUserEmailList()));
        rowCountMap.put("d_ticket_user", size(snapshot.getTicketUserList()));
        vo.setTableCount(rowCountMap.size());
        vo.setTableRowCount(rowCountMap);
        vo.setRowCount(rowCountMap.values().stream().mapToLong(Integer::longValue).sum());
        return vo;
    }

    private Map<String, Integer> emptyTableRowCount() {
        Map<String, Integer> rowCountMap = new LinkedHashMap<>();
        rowCountMap.put("d_user", 0);
        rowCountMap.put("d_user_mobile", 0);
        rowCountMap.put("d_user_email", 0);
        rowCountMap.put("d_ticket_user", 0);
        return rowCountMap;
    }

    private int size(Collection<?> collection) {
        return Objects.isNull(collection) ? 0 : collection.size();
    }

    private void clearUserRedisCache() {
        List<String> patterns = List.of(
                RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN, "*", "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.TICKET_USER_LIST, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_MOBILE_ERROR, "*").getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_EMAIL_ERROR, "*").getRelKey()
        );
        for (String pattern : patterns) {
            Set<String> keys = redisCache.keys(pattern);
            if (Objects.nonNull(keys) && !keys.isEmpty()) {
                redisCache.getInstance().delete(keys);
            }
        }
    }

    @Data
    public static class UserDemoDataSnapshot {

        private Date snapshotTime;

        private List<User> userList = new ArrayList<>();

        private List<UserMobile> userMobileList = new ArrayList<>();

        private List<UserEmail> userEmailList = new ArrayList<>();

        private List<TicketUser> ticketUserList = new ArrayList<>();
    }
}
