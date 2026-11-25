package com.wyh.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 员工工作经历实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmpExpr {
    /**
     * ID,主键
     */
    private Integer id;

    /**
     * 员工ID,逻辑关联emp.id
     */
    private Integer empId;

    /**
     * 开始时间
     */
    private LocalDate begin;

    /**
     * 结束时间
     */
    private LocalDate end;

    /**
     * 公司名称
     */
    private String company;

    /**
     * 职位
     */
    private String job;
}