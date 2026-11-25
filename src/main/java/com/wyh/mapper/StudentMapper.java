package com.wyh.mapper;

import com.wyh.pojo.Clazz;
import com.wyh.pojo.Student;
import com.wyh.pojo.StudentQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface StudentMapper {
    int countByNo(@Param("no") String no, @Param("excludeId") Long excludeId);
    int countByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);
    int countByIdCard(@Param("idCard") String idCard, @Param("excludeId") Long excludeId);


    List<Student> selectAll(@Param("studentQueryParam") StudentQueryParam studentQueryParam);

    Student selectById(@Param("id") Long id);

    void insert(@Param("student") Student student);

    void update(@Param("student") Student student);

    void deleteBatchIds(@Param("ids") List<Long> ids);

    List<Clazz> getClazzList();

    void updateViolation(Student student);

    Student getStudentByName(String name);
}
