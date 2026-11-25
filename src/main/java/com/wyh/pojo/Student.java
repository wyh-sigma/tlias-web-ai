package com.wyh.pojo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学员表实体类
 */
@Data
public class Student {

    /**
     * ID,主键
     */
    private Long id;

    /**
     * 姓名
     */
    @NotBlank
    private String name;

    /**
     * 学号
     */
    @NotBlank
    private String no;

    /**
     * 性别, 1: 男, 2: 女
     */
    @NotNull
    private Integer gender;

    /**
     * 手机号
     */
    @NotBlank
    private String phone;

    /**
     * 身份证号
     */
    @NotBlank
    private String idCard;

    /**
     * 是否来自于院校, 1:是, 0:否
     */
    @NotNull
    private Integer isCollege;

    /**
     * 联系地址
     */
    private String address;

    /**
     * 最高学历, 1:初中, 2:高中, 3:大专, 4:本科, 5:硕士, 6:博士
     */
    private Integer degree;

    /**
     * 毕业时间
     */
    private LocalDate graduationDate;

    /**
     * 班级ID, 关联班级表ID
     */
    @NotNull
    private Long clazzId;

    /**
     * 违纪次数
     */
    private Integer violationCount;

    /**
     * 违纪扣分
     */
    private Integer violationScore;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 班级名称
     */
    private String clazzName;
}