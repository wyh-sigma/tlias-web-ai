package com.wyh.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatDTO {

    /** 会话ID，同一会话共享上下文；不传则使用 default */
    private String conversationId;

    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000字")
    private String message;
}
