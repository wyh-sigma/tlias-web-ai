package com.wyh.pojo;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果封装类
 * 作用：后端给前端返回数据时，统一格式
 */
@Data
public class Result implements Serializable {

    private static final long serialVersionUID = 1L; // 序列化版本号

    private Integer code; // 响应码：1代表成功，0代表失败
    private String msg;   // 提示信息
    private Object data;  // 实际返回的数据

    // ----------------- 下面是几个快捷工具方法 -----------------

    /**
     * 成功响应（不带数据，比如：删除、修改成功，不需要给前端返回东西）
     */
    public static Result success() {
        Result result = new Result();
        result.code = 1;
        result.msg = "success";
        return result;
    }

    /**
     * 成功响应（带数据，比如：查询到了部门列表，要把列表给前端）
     */
    public static Result success(Object data) {
        Result result = new Result();
        result.code = 1;
        result.msg = "success";
        result.data = data;
        return result;
    }

    /**
     * 失败响应（比如：密码错误、系统异常）
     */
    public static Result error(String msg) {
        Result result = new Result();
        result.code = 0;
        result.msg = msg;
        return result;
    }

    /**
     * 失败响应（带标准错误码）
     */
    public static Result error(Integer code, String msg) {
        Result result = new Result();
        result.code = code;
        result.msg = msg;
        return result;
    }
}