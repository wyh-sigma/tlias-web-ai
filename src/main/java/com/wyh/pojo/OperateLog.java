package com.wyh.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperateLog {
    private Integer id;
    private Long operateUser;
    private LocalDateTime operateTime;
    private String className;
    private String methodName;
    private String methodParams;
    private String returnValue;
    private Integer status;     // 操作状态, 1:成功, 0:失败
    private Long costTime;
}