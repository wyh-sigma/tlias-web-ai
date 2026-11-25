package com.wyh.controller;


import com.wyh.anno.Log;
import com.wyh.pojo.Clazz;
import com.wyh.pojo.ClazzQueryParam;
import com.wyh.pojo.Result;
import com.wyh.service.ClazzService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/clazzs")
public class ClazzController {

    @Autowired
    private ClazzService clazzService;
    @GetMapping
    public Result findAll(ClazzQueryParam clazzQueryParam) {
        return Result.success(clazzService.findAll(clazzQueryParam));
    }
    @GetMapping("/{id}")
    public Result findById(@PathVariable("id") Long id) {
        return Result.success(clazzService.findById(id));
    }
    @GetMapping("/list")
    public Result findAllTeacher() {
        return Result.success(clazzService.findAllTeacher());
    }
    @PostMapping
    @Log
    public Result add(@RequestBody Clazz clazz) {
        log.info("添加班级，{}", clazz);
        clazzService.add(clazz);
        return Result.success();
    }
    @PutMapping
    @Log
    public Result update(@RequestBody Clazz clazz) {
        log.info("修改班级，{}", clazz);
        clazzService.update(clazz);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log
    public Result delete(@PathVariable("id") Long id) {
        log.info("删除班级，id：{}", id);
        clazzService.deleteById(id);
        return Result.success();
    }
}
