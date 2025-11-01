package com.tides.enums;

/**
 * Electronic ticket status.
 */
public enum TicketStatus {

    WAIT_ISSUE(1, "waitIssue"),
    ISSUED(2, "issued"),
    USED(3, "used"),
    REFUNDING(4, "refunding"),
    REFUNDED(5, "refunded"),
    VOID(6, "void"),
    ;

    private Integer code;

    private String msg;

    TicketStatus(Integer code, String msg) {
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
        for (TicketStatus re : TicketStatus.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }

    public static TicketStatus getRc(Integer code) {
        for (TicketStatus re : TicketStatus.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}
