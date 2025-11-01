package com.tides.enums;

/**
 * @description: 座位类型
 * @author: 19continue
 **/
public enum SeatType {
    /**
     * 座位类型
     * */
    GENERAL(1,"通用座位"),
    VIP(2,"VIP座"),
    COUPLE(3,"情侣座"),
    ACCESSIBLE(4,"无障碍座"),
    ;

    private Integer code;

    private String msg;

    SeatType(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static String getMsg(Integer code) {
        for (SeatType re : SeatType.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }

    public static SeatType getRc(Integer code) {
        for (SeatType re : SeatType.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}
