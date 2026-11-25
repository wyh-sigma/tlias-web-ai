package com.wyh.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResult {
    private Integer id;
    private String name;
    private String username;
    private String role;
    private String token;
    /** 访问令牌过期后用它换新令牌，有效期 7 天 */
    private String refreshToken;
}
