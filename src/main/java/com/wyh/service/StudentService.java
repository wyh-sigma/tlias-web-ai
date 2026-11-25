package com.wyh.service;

import com.wyh.pojo.Clazz;
import com.wyh.pojo.PageResult;
import com.wyh.pojo.Student;
import com.wyh.pojo.StudentQueryParam;

import java.util.List;

public interface StudentService {
    PageResult<Student> getAllStudents(StudentQueryParam studentQueryParam);

    Student getStudentById(Long id);

    void addStudent(Student student);

    void updateStudent(Student student);

    void deleteStudentByIds(List<Long> ids);

    List<Clazz> getClazzList();

    void handleViolation(Student student);

    Student getStudentByName(String name);
}
