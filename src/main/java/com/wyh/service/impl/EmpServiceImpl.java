package com.wyh.service.impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wyh.dto.LoginDTO;
import com.wyh.exception.BusinessException;
import com.wyh.exception.ErrorCode;
import com.wyh.mapper.EmpExprMapper;
import com.wyh.mapper.EmpMapper;
import com.wyh.pojo.*;
import com.wyh.service.AuthTokenService;
import com.wyh.service.EmpService;
import com.wyh.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmpServiceImpl implements EmpService {
    @Autowired
    private EmpMapper empMapper;
    @Autowired
    private EmpExprMapper empExprMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthTokenService authTokenService;
    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public PageResult<Emp> getEmps(EmpQueryParam empQueryParam) {
        Integer pageNum = empQueryParam.getPageNum();
        Integer pageSize = empQueryParam.getPageSize();
        pageNum = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        pageSize = Math.min(pageSize, 100);
        PageHelper.startPage(pageNum, pageSize);
        List<Emp> emps = empMapper.getEmps(empQueryParam);
        Page<Emp> page = (Page<Emp>) emps;
        return new PageResult<>(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addEmp(Emp emp) {
        emp.setPassword(passwordEncoder.encode("123456")); // 默认密码加密后入库
        emp.setCreateTime(LocalDateTime.now());
        emp.setUpdateTime(LocalDateTime.now());
        empMapper.insert(emp);
        List<EmpExpr> empExprs = emp.getExprList();
        if (empExprs != null && !empExprs.isEmpty()){
            empExprs.forEach(empExpr -> empExpr.setEmpId(emp.getId()));
            empExprMapper.insertBatch(empExprs);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmpByIds(List<Integer> ids) {

        empExprMapper.deleteEmpExprByIds(ids);
        empMapper.deleteEmpByIds(ids);
    }

    @Override
    public Emp getEmpById(Integer id) {
        Emp emp = empMapper.getEmpById(id);
        if (emp == null) {
            return null;
        }
        emp.setExprList(empExprMapper.getEmpExprById(id));
        return emp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmp(Emp emp) {
        emp.setUpdateTime(LocalDateTime.now());
        empExprMapper.deleteEmpExprByIds(List.of(emp.getId())); // Changed from empExprMapper.deleteEmpExprByIds(emp.getId());
        empMapper.updateEmp(emp);
        if (emp.getExprList() != null && !emp.getExprList().isEmpty()){
            emp.getExprList().forEach(empExpr -> empExpr.setEmpId(emp.getId()));
            empExprMapper.insertBatch(emp.getExprList()); // Insert the updated experience list
        }
    }
    @Override
    public LoginResult login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        if (loginAttemptService.isLocked(username)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        Emp dbEmp = empMapper.getAuthByUsername(username);
        if (dbEmp == null || !passwordMatches(loginDTO.getPassword(), dbEmp.getPassword())) {
            loginAttemptService.onFailure(username);
            throw new BusinessException("用户名或密码错误");
        }
        loginAttemptService.onSuccess(username);

        // 兼容历史明文密码：首次登录成功后自动升级为 BCrypt
        if (!isBcrypt(dbEmp.getPassword())) {
            empMapper.updatePassword(dbEmp.getId(), passwordEncoder.encode(loginDTO.getPassword()));
        }

        AuthTokenService.TokenPair pair = authTokenService.issue(dbEmp);
        return new LoginResult(dbEmp.getId(), dbEmp.getName(), dbEmp.getUsername(),
                dbEmp.getRole(), pair.accessToken(), pair.refreshToken());
    }

    @Override
    public LoginResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        Integer empId = authTokenService.resolveRefresh(refreshToken);
        if (empId == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Emp emp = empMapper.getAuthById(empId);
        if (emp == null) {
            authTokenService.revokeRefresh(refreshToken);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // refresh token 一次性使用，成功后立即作废旧令牌
        authTokenService.revokeRefresh(refreshToken);
        AuthTokenService.TokenPair pair = authTokenService.issue(emp);
        return new LoginResult(emp.getId(), emp.getName(), emp.getUsername(),
                emp.getRole(), pair.accessToken(), pair.refreshToken());
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        authTokenService.blacklistAccessToken(accessToken);
        authTokenService.revokeRefresh(refreshToken);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        return isBcrypt(storedPassword)
                ? passwordEncoder.matches(rawPassword, storedPassword)
                : storedPassword.equals(rawPassword);
    }

    private boolean isBcrypt(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    @Override
    public Emp getEmpByName(String username) {
        return empMapper.getEmpByName(username);
    }
}
