package com.wyh.service.impl;

import com.wyh.mapper.ReportMapper;
import com.wyh.pojo.EmpJobData;
import com.wyh.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Override
    public EmpJobData getEmpJobData() {
        List<Map<String, Object>> list = reportMapper.getEmpJobData();

        // 注意：这里泛型改成 List<String>，因为我们要把数字转成中文
        List<String> jobList = new ArrayList<>();
        List<Integer> dataList = new ArrayList<>();

        for (Map<String, Object> map : list) {
            jobList.add((String) map.get("jobName"));

            // 处理 COUNT(*) 返回的 Long
            dataList.add(((Number) map.get("num")).intValue());
        }
        return new EmpJobData(jobList, dataList);
    }

    @Override
    public List<Map<String, Object>> getStudentEducationData() {
        return reportMapper.getStudentEducationData();
    }

    @Override
    public List<Map<String, Object>> getClazzStudentCount() {
        return reportMapper.getClazzStudentCount();
    }

    @Override
    public List<Map<String, Object>> getClazzFinishWarning() {
        return reportMapper.getClazzFinishWarning();
    }

    @Override
    public List<Map<String, Object>> getViolationRanking() {
        return reportMapper.getViolationRanking();
    }

    @Override
    public List<Map<String, Object>> getHighRiskStudents() {
        return reportMapper.getHighRiskStudents();
    }

    @Override
    public List<Map<String, Object>> getMasterStudentCount() {
        return reportMapper.getMasterStudentCount();
    }
}
