package com.wyh.service.impl;

import com.wyh.exception.BusinessException;
import com.wyh.mapper.DeptMapper;
import com.wyh.pojo.Dept;
import com.wyh.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Override
    public List<Dept> findAll() {
        return deptMapper.findAll();

    }

    @Override
    public void deleteById(Integer id) {

        Long count = deptMapper.countByDeptId(id);
        if (count > 0){
            throw new BusinessException("部门下有员工，不能删除");
        }
        deptMapper.deleteById(id);
    }

    @Override
    public void addDept(Dept dept) {
        LocalDateTime localDateTime = LocalDateTime.now();
        dept.setCreateTime(localDateTime);
        dept.setUpdateTime(localDateTime);
        deptMapper.insert(dept);
    }

    @Override
    public Dept findDeptById(Integer id) {

        return deptMapper.findDeptById(id);
    }

    @Override
    public void updateDept(Dept dept) {
        LocalDateTime localDateTime = LocalDateTime.now();
        dept.setUpdateTime(localDateTime);
        deptMapper.updateDept(dept);
    }

    @Override
    public Dept findDeptByName(String name) {
        return deptMapper.findDeptByName(name);
    }
}
