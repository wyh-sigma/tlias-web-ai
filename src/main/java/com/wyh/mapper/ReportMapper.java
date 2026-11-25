package com.wyh.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    List<Map<String, Object>> getEmpJobData();

    List<Map<String, Object>> getStudentEducationData();

    /** 每个班级的学生人数（含班主任、教室、学科） */
    List<Map<String, Object>> getClazzStudentCount();

    /** 结课预警：30 天内即将结课的班级 */
    List<Map<String, Object>> getClazzFinishWarning();

    /** 违纪排行：按违纪扣分倒序 */
    List<Map<String, Object>> getViolationRanking();

    /** 高危学生清单：违纪扣分或违纪次数达到阈值 */
    List<Map<String, Object>> getHighRiskStudents();

    /** 每个班主任带的学生人数 */
    List<Map<String, Object>> getMasterStudentCount();
}
