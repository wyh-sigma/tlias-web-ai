package com.wyh.pojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工实体类
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Emp {
    /**
     * ID,主键
     */
    private Integer id;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 角色, ADMIN: 管理员, USER: 普通用户
     */
    private String role;

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /**
     * 性别, 1:男, 2:女
     */
    @NotNull(message = "性别不能为空")
    private Integer gender;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 职位, 1 班主任, 2 讲师 , 3 学工主管, 4 教研主管, 5 咨询师
     */
    private Integer job;

    /**
     * 薪资
     */
    private Integer salary;

    /**
     * 头像
     */
    private String image;

    /**
     * 入职日期
     */
    private LocalDate entryDate;

    /**
     * 部门ID
     */
    private Integer deptId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    // ==========================================
    // 下面是多表关联的扩展属性，数据库中 emp 表并没有这些字段
    // ==========================================

    /**
     * 关联的部门名称（用于分页查询时，把部门名字带出来）
     */
    private String deptName;

    /**
     * 员工的工作经历列表（一对多关系）
     */
    private List<EmpExpr> exprList;
}