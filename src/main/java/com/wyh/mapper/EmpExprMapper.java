package com.wyh.mapper;


import com.wyh.pojo.Emp;
import com.wyh.pojo.EmpExpr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmpExprMapper {
    void insertBatch(@Param("empExprs") List<EmpExpr> empExprs);

    void deleteEmpExprByIds(@Param("ids") List<Integer> ids);

    List<EmpExpr> getEmpExprById(@Param("id") Integer id);
}
