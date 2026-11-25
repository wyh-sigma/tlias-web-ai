package com.wyh.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmpJobData {
    private List<String> jobList;
    private List<Integer> dataList;
}
