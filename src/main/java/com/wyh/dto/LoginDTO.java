package com.wyh.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数，避免直接复用 Emp 实体
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
