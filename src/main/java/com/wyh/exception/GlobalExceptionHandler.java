package com.wyh.exception;


import com.wyh.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全局异常处理器
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Pattern DUP_KEY = Pattern.compile("for key '([^']+)'");

    @ExceptionHandler(DuplicateKeyException.class)
    public Result handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据库唯一约束冲突", e);
        String key = null;
        String message = e.getMessage();
        if (message != null) {
            Matcher m = DUP_KEY.matcher(message);
            if (m.find()) {
                key = m.group(1).toLowerCase();   // student.no / emp.username
            }
        }
        if (key != null) {
            // 顺序很重要：username 也以 name 结尾，name 必须放在最后判断
            if (key.endsWith("id_card"))  return Result.error("该身份证号已被注册");
            if (key.endsWith("username")) return Result.error("该用户名已被占用");
            if (key.endsWith("phone"))    return Result.error("该手机号已被注册");
            if (key.endsWith("no"))       return Result.error("该学号已存在");
            if (key.endsWith("name"))     return Result.error("班级名称已存在");
        }
        return Result.error("数据已存在，请勿重复提交");
    }

    /**
     * 路径参数/查询参数类型不匹配（如 /clazzs/list 被 /{id} 匹配后转 Long 失败）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配：{}", e.getName());
        return Result.error("参数格式错误：" + e.getName());
    }
    /**
     * 专门处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        log.warn("业务异常：{} {}", e.getCode(), e.getMessage());
        // 直接把具体的业务提示（比如“部门下有员工”）返回给前端
        return Result.error(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);          // 第二个参数传异常对象，日志里才有完整堆栈
        return Result.error("服务器繁忙，请稍后重试");
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleNotBlankException(MethodArgumentNotValidException e) {
        log.error("参数验证失败", e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst().orElse("参数验证失败");
        return Result.error(message);
    }




}
