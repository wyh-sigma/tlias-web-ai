package com.wyh.exception;

/**
 * 业务错误码枚举：集中管理 code 与提示语
 * code 规范建议：A-用户端 / B-系统 / C-第三方（阿里《阿里巴巴Java开发手册》推荐）
 */
public enum ErrorCode {

    SUCCESS(1, "操作成功"),
    BUSINESS_ERROR(5000, "业务处理失败"),
    PARAM_ERROR(4000, "参数校验失败"),
    NOT_LOGIN(4001, "未登录或登录已过期"),
    REFRESH_TOKEN_INVALID(4002, "刷新令牌无效或已过期，请重新登录"),
    NO_PERMISSION(4003, "无操作权限"),
    ACCOUNT_LOCKED(4023, "登录失败次数过多，账号已锁定 15 分钟"),
    TOO_MANY_REQUESTS(4029, "请求过于频繁，请稍后再试"),

    DATA_NOT_FOUND(5001, "数据不存在"),
    DATA_DUPLICATE(5002, "数据已存在，请勿重复提交"),

    SYSTEM_ERROR(9999, "服务器繁忙，请稍后重试");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}