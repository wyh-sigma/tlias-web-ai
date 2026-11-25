package com.wyh.service;

import com.wyh.pojo.Clazz;
import com.wyh.pojo.ClazzQueryParam;
import com.wyh.pojo.Emp;
import com.wyh.pojo.PageResult;

import java.util.List;

public interface ClazzService {
    PageResult<Clazz> findAll(ClazzQueryParam clazzQueryParam);
    Clazz findById(Long id);

    void add(Clazz clazz);

    void update(Clazz clazz);

    void deleteById(Long id);

    List<Emp> findAllTeacher();

    Clazz getClazzByName(String name);
}
