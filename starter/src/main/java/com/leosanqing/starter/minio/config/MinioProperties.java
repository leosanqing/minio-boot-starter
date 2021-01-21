package com.leosanqing.starter.minio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: rtliu
 * @date: 2021/1/20 10:54 上午
 * @package: minio.config
 * @description: MinIO 配置
 * @version: v1.0.0
 */
@ConfigurationProperties(prefix = "leosanqing.minio")
@Data
public class MinioProperties {
    private String endpoint;
    private String secretKey;
    private String accessKey;
    private String bucketName;
}

