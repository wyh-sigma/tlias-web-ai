package com.wyh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wyh.exception.BusinessException;
import com.wyh.mapper.StudentMapper;
import com.wyh.pojo.Clazz;
import com.wyh.pojo.PageResult;
import com.wyh.pojo.Student;
import com.wyh.pojo.StudentQueryParam;
import com.wyh.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {
    @Autowired
    private StudentMapper studentMapper;
    @Override
    public PageResult<Student> getAllStudents(StudentQueryParam studentQueryParam) {
        Integer pageNum = studentQueryParam.getPageNum();
        Integer pageSize = studentQueryParam.getPageSize();
        pageNum = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        pageSize = Math.min(pageSize, 100);
        PageHelper.startPage(pageNum, pageSize);
        List<Student> students = studentMapper.selectAll(studentQueryParam);
        Page<Student> page = (Page<Student>) students;
        return new PageResult<>(page.getTotal(), page.getResult());
    }

    @Override
    public Student getStudentById(Long id) {
        return studentMapper.selectById(id);
    }

    @Override
    @Transactional
    public void addStudent(Student student) {
        if (studentMapper.countByNo(student.getNo(), null) > 0) {
            throw new BusinessException("该学号已被占用，请检查后重新输入");
        }
        if (studentMapper.countByPhone(student.getPhone(), null) > 0) {
            throw new BusinessException("该手机号已被注册");
        }
        if (student.getIdCard() != null && studentMapper.countByIdCard(student.getIdCard(), null) > 0) {
            throw new BusinessException("该身份证号已被注册");
        }
        if (student.getViolationCount() == null) student.setViolationCount(0);
        if (student.getViolationScore() == null) student.setViolationScore(0);
        student.setCreateTime(LocalDateTime.now());
        student.setUpdateTime(LocalDateTime.now());
        studentMapper.insert(student);
    }

    @Override
    @Transactional
    public void updateStudent(Student student) {
        student.setUpdateTime(LocalDateTime.now());
        if (studentMapper.countByNo(student.getNo(), student.getId()) > 0) {
            throw new BusinessException("该学号已被占用，请检查后重新输入");
        }
        if (studentMapper.countByPhone(student.getPhone(), student.getId()) > 0) {
            throw new BusinessException("该手机号已被注册");
        }
        if (student.getIdCard() != null && studentMapper.countByIdCard(student.getIdCard(), student.getId()) > 0) {
            throw new BusinessException("该身份证号已被注册");
        }
        studentMapper.update(student);
    }

    @Override
    @Transactional
    public void deleteStudentByIds(List<Long> ids) {
        studentMapper.deleteBatchIds(ids);
    }

    @Override
    public List<Clazz> getClazzList() {
        return studentMapper.getClazzList();
    }

    @Override
    public void handleViolation(Student student) {
        if (student.getViolationCount() == null && student.getViolationScore() == null) {
            throw new BusinessException("违纪次数和扣分不能同时为空");
        }
        student.setUpdateTime(LocalDateTime.now());
        studentMapper.updateViolation(student);
    }

    @Override
    public Student getStudentByName(String name) {
        return studentMapper.getStudentByName(name);
    }
}
