package com.wyh.utils;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.wyh.pojo.AliyunOSSProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
public class AliyunOSSUtils {
    @Autowired
    private AliyunOSSProperties aliyunOSSProperties;


    private OSSClient ossClient;

    @PostConstruct
    public void init() {
        // 从环境变量中读取AccessKey，自动创建客户端
        this.ossClient = OSSClient.newBuilder()
                .credentialsProvider(new EnvironmentVariableCredentialsProvider())
                .region(aliyunOSSProperties.getRegion())
                .build();
    }

    public String upload(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = UUID.randomUUID().toString().replace("-", "") + extension;

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.newBuilder()
                    .bucket(aliyunOSSProperties.getBucketName())
                    .key(objectName)
                    .body(BinaryData.fromStream(inputStream))
                    .build();

            ossClient.putObject(request);
        }

        String url = "https://" + aliyunOSSProperties.getBucketName() + ".oss-" + aliyunOSSProperties.getRegion() + ".aliyuncs.com/" + objectName;
        log.info("文件上传成功，访问地址：{}", url);
        return url;
    }
}