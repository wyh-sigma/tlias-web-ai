package com.wyh.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class StudentClazzData {
    private List<String> clazzList;
    private List<Integer> dataList;
}
