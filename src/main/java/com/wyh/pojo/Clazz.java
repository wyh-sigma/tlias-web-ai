package com.wyh.pojo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 班级表实体类
 */
@Data
public class Clazz {

    /**
     * ID,主键
     */
    private Long id;

    /**
     * 班级名称
     */
    private String name;

    /**
     * 班级教室
     */
    private String room;

    /**
     * 开课时间
     */
    private LocalDate beginDate;

    /**
     * 结课时间
     */
    private LocalDate endDate;

    /**
     * 班主任ID，关联员工表ID
     */
    private Integer masterId;

    /**
     * 学科，1:java, 2:前端, 3:大数据, 4:Python, 5:Go, 6: 嵌入式
     */
    private Integer subject;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 班主任名称
     */
    private String masterName;
    /**
     * 状态，1:未开班，2:开班中，3:已结业
     */
    private Integer status;
}