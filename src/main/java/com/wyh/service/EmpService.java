package com.wyh.service;

import com.wyh.dto.LoginDTO;
import com.wyh.pojo.*;

import java.util.List;

public interface EmpService {


    PageResult<Emp> getEmps(EmpQueryParam empQueryParam);

    void addEmp(Emp emp);

    void deleteEmpByIds(List<Integer> ids);

    Emp getEmpById(Integer id);

    void updateEmp(Emp emp);

    LoginResult login(LoginDTO loginDTO);

    LoginResult refresh(String refreshToken);

    void logout(String accessToken, String refreshToken);

    Emp getEmpByName(String name);
}
