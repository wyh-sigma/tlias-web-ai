package com.wyh.dto;

import lombok.Data;

/**
 * 刷新/退出登录参数：refreshToken 可选，退出登录允许只带 access token。
 */
@Data
public class TokenDTO {

    private String refreshToken;
}
