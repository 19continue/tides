package com.tides.enums;

import java.util.Objects;

/**
 * Professional ticketing project lifecycle status.
 */
public enum ProjectLifecycleStatus {

    DRAFT(0, "草稿"),
    PENDING_REVIEW(10, "待审核"),
    REVIEW_REJECTED(20, "审核驳回"),
    PENDING_SCHEDULE(30, "待排期"),
    WARMING_UP(40, "预热中"),
    PRE_SELLING(50, "预售中"),
    ON_SALE(60, "售卖中"),
    SALE_STOPPED(70, "停售"),
    PERFORMING(80, "演出/放映中"),
    FINISHED(90, "已结束"),
    SETTLING(100, "结算中"),
    ARCHIVED(110, "已归档"),
    POSTPONED(120, "延期"),
    CANCELED(130, "取消");

    private final Integer code;

    private final String msg;

    ProjectLifecycleStatus(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public static String getMsg(Integer code) {
        for (ProjectLifecycleStatus status : values()) {
            if (Objects.equals(status.getCode(), code)) {
                return status.getMsg();
            }
        }
        return null;
    }

    public static boolean canSell(Integer code) {
        return Objects.equals(PRE_SELLING.getCode(), code) || Objects.equals(ON_SALE.getCode(), code);
    }

    public static boolean canEditCoreData(Integer code) {
        return Objects.equals(DRAFT.getCode(), code) ||
                Objects.equals(REVIEW_REJECTED.getCode(), code) ||
                Objects.equals(PENDING_REVIEW.getCode(), code);
    }
}
