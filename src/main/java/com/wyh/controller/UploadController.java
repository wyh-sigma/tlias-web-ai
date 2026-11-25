package com.wyh.controller;


import com.wyh.anno.Log;
import com.wyh.exception.BusinessException;
import com.wyh.exception.ErrorCode;
import com.wyh.pojo.Result;
import com.wyh.utils.AliyunOSSUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Slf4j
@RestController
public class UploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_SIZE = 10 * 1024 * 1024L;

    @Autowired
    private AliyunOSSUtils aliyunOSSUtils;

    @PostMapping("/upload")
    @Log
    public Result upload(MultipartFile file) throws Exception {
        validate(file);
        log.info("接收到文件上传请求：{}", file.getOriginalFilename());
        String url = aliyunOSSUtils.upload(file);
        return Result.success(url);
    }

    /**
     * 只允许图片、限制大小，并校验扩展名与 Content-Type
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片大小不能超过 10MB");
        }

        String filename = file.getOriginalFilename();
        int dotIndex = filename == null ? -1 : filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不合法");
        }
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 jpg/jpeg/png/gif 图片");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件类型不合法");
        }
    }
}
