package com.wyh.pojo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 部门实体类
 * 对应数据库表：dept
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dept {

    // 主键 ID
    private Integer id;

    // 部门名称
    @NotBlank(message = "部门名称不能为空")
    private String name;

    // 创建时间
    private LocalDateTime createTime;

    // 修改时间
    private LocalDateTime updateTime;

}