package com.wyh.service;


import com.wyh.pojo.EmpJobData;

import java.util.List;
import java.util.Map;

public interface ReportService {
    EmpJobData getEmpJobData();

    List<Map<String, Object>> getStudentEducationData();

    List<Map<String, Object>> getClazzStudentCount();

    List<Map<String, Object>> getClazzFinishWarning();

    List<Map<String, Object>> getViolationRanking();

    List<Map<String, Object>> getHighRiskStudents();

    List<Map<String, Object>> getMasterStudentCount();
}
