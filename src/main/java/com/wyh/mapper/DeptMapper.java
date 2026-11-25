package com.wyh.mapper;


import com.wyh.pojo.Dept;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeptMapper {

    @Select("select id, name, create_time, update_time from dept order by id asc")
    List<Dept> findAll();

    /**
     * 根据部门ID统计员工数量
     */
    @Select("SELECT COUNT(*) FROM emp WHERE dept_id = #{deptId}")
    Long countByDeptId(@Param("deptId") Integer deptId);

    @Delete("delete from dept where id = #{id}")
    void deleteById(Integer id);

    @Insert("insert into dept (name, create_time, update_time) values (#{name}, #{createTime}, #{updateTime})")
    void insert(Dept dept);

    @Select("select id, name, create_time, update_time from dept where id = #{id}")
    Dept findDeptById(Integer id);

    @Update("update dept set name = #{name}, update_time = #{updateTime} where id = #{id}")
    void updateDept(Dept dept);

    @Select("select id, name, create_time, update_time from dept where name = #{name}")
    Dept findDeptByName(String name);
}
