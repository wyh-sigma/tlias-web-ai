package com.wyh.controller;


import com.wyh.pojo.Result;
import com.wyh.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/empJobData")
    public Result getEmpJobData() {
        return Result.success(reportService.getEmpJobData());
    }

    @GetMapping("/studentEducationData")
    public Result getStudentEducationData() {
        return Result.success(reportService.getStudentEducationData());
    }

    /** 每个班级有多少学生 */
    @GetMapping("/clazzStudentCount")
    public Result getClazzStudentCount() {
        return Result.success(reportService.getClazzStudentCount());
    }

    /** 结课预警：30 天内会结课的班级 */
    @GetMapping("/clazzFinishWarning")
    public Result getClazzFinishWarning() {
        return Result.success(reportService.getClazzFinishWarning());
    }

    /** 违纪排行 */
    @GetMapping("/violationRanking")
    public Result getViolationRanking() {
        return Result.success(reportService.getViolationRanking());
    }

    /** 高危学生清单 */
    @GetMapping("/highRiskStudents")
    public Result getHighRiskStudents() {
        return Result.success(reportService.getHighRiskStudents());
    }

    /** 每个班主任带的学生人数 */
    @GetMapping("/masterStudentCount")
    public Result getMasterStudentCount() {
        return Result.success(reportService.getMasterStudentCount());
    }
}
