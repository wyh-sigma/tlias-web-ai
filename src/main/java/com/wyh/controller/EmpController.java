package com.wyh.controller;


import com.wyh.anno.Log;
import com.wyh.pojo.Emp;
import com.wyh.pojo.EmpQueryParam;
import com.wyh.pojo.PageResult;
import com.wyh.pojo.Result;
import com.wyh.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/emps")
public class EmpController {
    @Autowired
    private EmpService empService;

    @GetMapping
    public Result getEmps(EmpQueryParam empQueryParam){
        return Result.success(empService.getEmps(empQueryParam));
    }
    @PostMapping
    @Log
    public Result addEmp(@RequestBody @Validated Emp emp){
        log.info("新增员工={}", emp);
        empService.addEmp(emp);
        return Result.success();
    }

    @DeleteMapping
    @Log
    public Result deleteEmp(@RequestParam List<Integer> ids){
        log.info("删除员工={}", ids.toString());
        empService.deleteEmpByIds(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getEmpById(@PathVariable Integer id){
        return Result.success(empService.getEmpById(id));
    }
    @PutMapping
    @Log
    public Result updateEmp(@RequestBody @Validated Emp emp){
        log.info("更新员工={}", emp);
        empService.updateEmp(emp);
        return Result.success();
    }

}
