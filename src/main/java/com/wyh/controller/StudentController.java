package com.wyh.controller;

import com.wyh.anno.Log;
import com.wyh.pojo.Result;
import com.wyh.pojo.Student;
import com.wyh.pojo.StudentQueryParam;
import com.wyh.service.StudentService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private StudentService studentService;
    @GetMapping
    public Result getAllStudents(StudentQueryParam studentQueryParam) {
        return Result.success(studentService.getAllStudents(studentQueryParam));
    }
    @GetMapping("/{id}")
    public Result getStudentById(@PathVariable("id") Long id) {
        return Result.success(studentService.getStudentById(id));
    }
    @GetMapping("/clazzlist")
    public Result getClazzList() {
        return Result.success(studentService.getClazzList());
    }
    @PostMapping
    @Log
    public Result addStudent(@RequestBody Student student) {
        log.info("添加学生={}", student.toString());
        studentService.addStudent(student);
        return Result.success();
    }
    @PutMapping
    @Log
    public Result updateStudent(@RequestBody Student student) {
        log.info("更新学生={}", student.toString());
        studentService.updateStudent(student);
        return Result.success();
    }
    @PutMapping("/violation")
    @Log
    public Result handleViolation(@RequestBody Student student) {
        log.info("处理学生违规,学生id{},违规次数{},扣分{}", student.getId(), student.getViolationCount(), student.getViolationScore());
        studentService.handleViolation(student);
        return Result.success();
    }
    @DeleteMapping
    @Log
    public Result deleteStudent(@RequestParam("ids") List<Long> ids) {
        log.info("删除学生={}", ids.toString());
        studentService.deleteStudentByIds(ids);
        return Result.success();
    }
}
