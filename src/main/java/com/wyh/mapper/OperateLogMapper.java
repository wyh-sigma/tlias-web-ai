package com.wyh.mapper;

import com.wyh.pojo.OperateLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface OperateLogMapper {
    @Insert("INSERT INTO operate_log(operate_user, operate_time, class_name, method_name, method_params, return_value, status, cost_time) " +
            "VALUES(#{operateUser}, #{operateTime}, #{className}, #{methodName}, #{methodParams}, #{returnValue}, #{status}, #{costTime})")
    void insert(OperateLog operateLog);
}