package com.wyh.utils;


import com.wyh.pojo.*;
import com.wyh.service.ClazzService;
import com.wyh.service.DeptService;
import com.wyh.service.EmpService;
import com.wyh.service.StudentService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 可调用的业务查询工具。
 * <p>
 * ToolContext 由 ChatClient 在调用时注入（不进入模型参数），其中包含当前登录用户的 id 和角色，
 * 用于做工具级的数据权限控制：
 * - ADMIN：可查询部门、员工、班级、学生等全部数据；
 * - USER：只能查询本人员工信息，其他数据一律拒绝。
 */
@Component
public class TliasAIQueryUtils {

    private static final String ADMIN = "ADMIN";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ROLE = "role";

    private DeptService deptService;
    private EmpService empService;
    private ClazzService clazzService;
    private StudentService studentService;

    @Autowired
    public TliasAIQueryUtils(DeptService deptService, EmpService empService, ClazzService clazzService, StudentService studentService) {
        this.deptService = deptService;
        this.empService = empService;
        this.clazzService = clazzService;
        this.studentService = studentService;
    }

    @Tool(description = "查询所有部门")
    public List<Dept> deptQuery(ToolContext toolContext) {
        requireAdmin(toolContext, "查询部门");
        return deptService.findAll();
    }

    @Tool(description = "根据部门名称查询部门详情，参数name为部门名称，返回部门对象")
    public Dept deptQueryByName(String name, ToolContext toolContext) {
        requireAdmin(toolContext, "查询部门");
        return deptService.findDeptByName(name);
    }

    @Tool(description = "查询员工：管理员返回所有员工，普通用户只返回本人信息")
    public List<Emp> empQuery(ToolContext toolContext) {
        if (isAdmin(toolContext)) {
            EmpQueryParam empQueryParam = new EmpQueryParam();
            empQueryParam.setPageNum(1);
            empQueryParam.setPageSize(100);
            return empService.getEmps(empQueryParam).getRows();
        }
        Emp self = currentEmp(toolContext);
        return self == null ? List.of() : List.of(self);
    }

    @Tool(description = "根据员工名称查询员工详情，参数name为员工名称，返回员工对象")
    public Emp empQueryByName(String name, ToolContext toolContext) {
        if (isAdmin(toolContext)) {
            return empService.getEmpByName(name);
        }
        Emp self = currentEmp(toolContext);
        if (self == null || name == null || !name.equals(self.getName())) {
            throw new IllegalStateException("无权限：普通用户只能查询本人信息");
        }
        return self;
    }

    @Tool(description = "查询所有班级")
    public List<Clazz> clazzQuery(ToolContext toolContext) {
        requireAdmin(toolContext, "查询班级");
        ClazzQueryParam clazzQueryParam = new ClazzQueryParam();
        clazzQueryParam.setPageNum(1);
        clazzQueryParam.setPageSize(100);
        return clazzService.findAll(clazzQueryParam).getRows();
    }

    @Tool(description = "根据班级名称查询班级详情，参数name为班级名称，返回班级对象")
    public Clazz clazzQueryByName(String name, ToolContext toolContext) {
        requireAdmin(toolContext, "查询班级");
        return clazzService.getClazzByName(name);
    }

    @Tool(description = "查询所有学生")
    public List<Student> studentQuery(ToolContext toolContext) {
        requireAdmin(toolContext, "查询学生");
        StudentQueryParam studentQueryParam = new StudentQueryParam();
        studentQueryParam.setPageNum(1);
        studentQueryParam.setPageSize(100);
        return studentService.getAllStudents(studentQueryParam).getRows();
    }

    @Tool(description = "根据学生名称查询学生详情，参数name为学生名称，返回学生对象")
    public Student studentQueryByName(String name, ToolContext toolContext) {
        requireAdmin(toolContext, "查询学生");
        return studentService.getStudentByName(name);
    }

    private void requireAdmin(ToolContext toolContext, String action) {
        if (!isAdmin(toolContext)) {
            throw new IllegalStateException("无权限：当前用户不是管理员，无法" + action);
        }
    }

    private boolean isAdmin(ToolContext toolContext) {
        return ADMIN.equalsIgnoreCase(currentRole(toolContext));
    }

    private String currentRole(ToolContext toolContext) {
        Object role = toolContext == null || toolContext.getContext() == null
                ? null : toolContext.getContext().get(KEY_ROLE);
        return role == null ? "USER" : role.toString();
    }

    private Emp currentEmp(ToolContext toolContext) {
        Object userId = toolContext == null || toolContext.getContext() == null
                ? null : toolContext.getContext().get(KEY_USER_ID);
        if (!(userId instanceof Number number)) {
            return null;
        }
        return empService.getEmpById(number.intValue());
    }
}
