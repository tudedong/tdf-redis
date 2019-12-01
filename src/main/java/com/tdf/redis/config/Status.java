package com.tdf.redis.config;

public enum Status {

    SUCCESS_OPERATION(0, "操作成功"),
    FAIL_OPERATION(2005, "操作失败"),
    FAIL_EXCEPTION(2006, "系统异常");

    private int code;
    private String label;

    private Status(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return this.code;
    }

    public String label() {
        return this.label;
    }
}
