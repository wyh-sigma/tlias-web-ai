package com.wyh.mapper;


import com.wyh.pojo.Clazz;
import com.wyh.pojo.ClazzQueryParam;
import com.wyh.pojo.Emp;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ClazzMapper {
    /**
     * 查询所有班级
     */
    List<Clazz> selectAll(ClazzQueryParam clazzQueryParam);

    /**
     * 根据id查询班级
     */
    Clazz selectById(@Param("id") Long id);

    /**
     * 添加班级
     */
    void insert(@Param("clazz") Clazz clazz);

    /**
     * 修改班级
     */
    void update(@Param("clazz") Clazz clazz);
    /**
     * 删除班级
     */
    void deleteById(@Param("id") Long id);

    List<Emp> findAllTeacher();

    Integer countByClazzId(Long id);

    Clazz getClazzByName(String name);
}
