package com.wyh.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;


@AllArgsConstructor
@Data
@NoArgsConstructor
public class ClazzQueryParam {

    private String name;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
