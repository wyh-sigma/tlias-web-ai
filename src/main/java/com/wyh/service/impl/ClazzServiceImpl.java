package com.wyh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wyh.exception.BusinessException;
import com.wyh.mapper.ClazzMapper;
import com.wyh.pojo.Clazz;
import com.wyh.pojo.ClazzQueryParam;
import com.wyh.pojo.Emp;
import com.wyh.pojo.PageResult;
import com.wyh.service.ClazzService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClazzServiceImpl implements ClazzService {

    @Autowired
    private ClazzMapper clazzMapper;
    @Override
    public PageResult<Clazz> findAll(ClazzQueryParam clazzQueryParam) {
        Integer pageNum = clazzQueryParam.getPageNum();
        Integer pageSize = clazzQueryParam.getPageSize();
        pageNum = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        pageSize = Math.min(pageSize, 100);
        PageHelper.startPage(pageNum, pageSize);
        List<Clazz> clazzList = clazzMapper.selectAll(clazzQueryParam);
        Page<Clazz> page = (Page<Clazz>) clazzList;
        return new PageResult<>(page.getTotal(), page.getResult());
    }

    @Override
    public Clazz findById(Long id) {
        return clazzMapper.selectById(id);
    }
    @Override
    @Transactional
    public void add(Clazz clazz) {
        checkDate(clazz);
        clazz.setCreateTime(LocalDateTime.now());
        clazz.setUpdateTime(LocalDateTime.now());
        clazzMapper.insert(clazz);
    }

    @Override
    @Transactional
    public void update(Clazz clazz) {
        checkDate(clazz);
        clazz.setUpdateTime(LocalDateTime.now());

        clazzMapper.update(clazz);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {

        int count = clazzMapper.countByClazzId(id);
        if (count > 0){
            throw new BusinessException("对不起，该班级下有学生，不能直接删除");
        }
        clazzMapper.deleteById(id);
    }

    @Override
    public List<Emp> findAllTeacher() {
        return clazzMapper.findAllTeacher();
    }

    /**
     * 校验开课/结课时间
     */
    private void checkDate(Clazz clazz) {
        if (clazz.getBeginDate() == null || clazz.getEndDate() == null) {
            throw new BusinessException("开课时间和结课时间不能为空");
        }
        if (!clazz.getEndDate().isAfter(clazz.getBeginDate())) {
            throw new BusinessException("结课时间必须晚于开课时间");
        }
    }

    @Override
    public Clazz getClazzByName(String name) {
        return clazzMapper.getClazzByName(name);
    }
}
