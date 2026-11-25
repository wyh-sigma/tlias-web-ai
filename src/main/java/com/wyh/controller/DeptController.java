package com.wyh.controller;


import com.wyh.anno.Log;
import com.wyh.pojo.Dept;
import com.wyh.pojo.Result;
import com.wyh.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@RequestMapping("/depts")
@RestController
public class DeptController {
    @Autowired
    private DeptService deptService;
    @GetMapping
    public Result list(){
        List<Dept> deptList = deptService.findAll();
        return Result.success(deptList);
    }
    @DeleteMapping("/{id}") // 删除部门
    @Log
    public Result delete(@PathVariable("id") Integer id){
        log.info("删除部门，id：{}", id);
        deptService.deleteById(id);
        return Result.success();
    }
    @PostMapping // 添加部门
    @Log
    public Result add(@RequestBody @Validated Dept dept){
        log.info("添加部门:{}", dept);
        if (dept.getName() == null || dept.getName().trim().isEmpty()) {
            return Result.error("部门名称不能为空");
        }
        deptService.addDept(dept);
        return Result.success();
    }
    @GetMapping("/{id}")
    public Result get(@PathVariable("id") Integer id){
        Dept dept = deptService.findDeptById(id);
        return Result.success(dept);
    }
    @PutMapping // 修改部门
    @Log
    public Result update(@RequestBody Dept dept){
        log.info("修改部门:{}", dept);
        if (dept.getId() == null) {
            return Result.error("部门编号不能为空");
        }
        if (dept.getName() == null || dept.getName().trim().isEmpty()) {
            return Result.error("部门名称不能为空");
        }
        deptService.updateDept(dept);
        return Result.success();
    }
}
