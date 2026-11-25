package com.wyh.aspect;


import com.wyh.mapper.OperateLogMapper;
import com.wyh.pojo.OperateLog;
import com.wyh.utils.BaseContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Slf4j
@Component
public class LogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;

    // 切入点：所有标了 @Log 的方法
    @Pointcut("@annotation(com.wyh.anno.Log)")
    public void pt() {}

    @Around("pt()")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();          // 放行，执行目标方法
            return result;
        } catch (Throwable e) {
            error = e;                             // 记下异常，然后原样抛出
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - begin;
            try {
                OperateLog operateLog = new OperateLog();
                operateLog.setOperateUser(BaseContext.getCurrentId());
                operateLog.setOperateTime(LocalDateTime.now());
                operateLog.setClassName(joinPoint.getTarget().getClass().getName());
                operateLog.setMethodName(((MethodSignature) joinPoint.getSignature()).getName());
                operateLog.setMethodParams(truncate(Arrays.toString(joinPoint.getArgs()), 1000));
                boolean success = (error == null);
                operateLog.setReturnValue(truncate(success ? String.valueOf(result) : error.getMessage(), 2000));
                operateLog.setStatus(success ? 1 : 0);   // 1:成功, 0:失败
                operateLog.setCostTime(cost);
                operateLogMapper.insert(operateLog);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);   // 日志失败不能影响业务
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
