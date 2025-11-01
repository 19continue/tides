package com.tides.vo;

import lombok.Data;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @description: 演示数据快照状态
 * @author: 19continue
 **/
@Data
public class DemoDataSnapshotVo {

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 是否已经保存快照
     */
    private Boolean snapshotExists;

    /**
     * 快照保存时间
     */
    private Date snapshotTime;

    /**
     * 快照覆盖的表数量
     */
    private Integer tableCount;

    /**
     * 快照总行数
     */
    private Long rowCount;

    /**
     * 每张表的快照行数
     */
    private Map<String, Integer> tableRowCount = new LinkedHashMap<>();
}
