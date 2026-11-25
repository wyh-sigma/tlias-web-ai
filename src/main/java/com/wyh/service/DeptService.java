package com.wyh.service;

import com.wyh.pojo.Dept;

import java.util.List;

public interface DeptService {

    List<Dept> findAll();

    void deleteById(Integer id);

    void addDept(Dept dept);

    Dept findDeptById(Integer id);

    void updateDept(Dept dept);

    Dept findDeptByName(String name);
}
