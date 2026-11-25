package com.wyh.exception;



public class BusinessException extends RuntimeException {

    private final Integer code;

    // 方式一：直接用一个标准错误码
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
    }

    // 方式二：标准码 + 自定义细节提示（保留 code，覆盖 message）
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public Integer getCode() {
        return code;
    }

}
