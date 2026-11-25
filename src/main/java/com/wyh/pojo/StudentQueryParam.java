package com.wyh.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentQueryParam {
    private String name;
    private Integer degree;
    private String clazzName;
    private Integer pageNum;
    private Integer pageSize;
}
