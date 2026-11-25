package com.wyh.mapper;


import com.wyh.pojo.Emp;
import com.wyh.pojo.EmpQueryParam;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmpMapper {
    List<Emp> getEmps(@Param("empQueryParam") EmpQueryParam empQueryParam);

    void insert(Emp emp);

    void deleteEmpByIds(@Param("ids") List<Integer> ids);

    Emp getEmpById(@Param("id") Integer id);

    void updateEmp(@Param("emp") Emp emp);

    Emp getAuthByUsername(@Param("username") String username);

    Emp getAuthById(@Param("id") Integer id);

    void updatePassword(@Param("id") Integer id, @Param("password") String password);

    Emp getEmpByName(String username);
}
