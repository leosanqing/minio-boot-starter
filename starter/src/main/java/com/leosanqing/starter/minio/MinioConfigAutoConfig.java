package com.leosanqing.starter.minio;

import com.leosanqing.starter.minio.component.MinioComponent;
import com.leosanqing.starter.minio.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author: rtliu
 * @date: 2021/1/21 11:07 上午
 * @package: com.leosanqing.starter.minio
 * @description: 自动配置类
 * @version: v1.0.0
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnExpression("${leosanqing.minio.enabled:true}")
@ComponentScan(
        basePackages = {"com.leosanqing.starter.minio"}
)

public class MinioConfigAutoConfig {
    @Autowired
    MinioProperties minioProperties;

    @Bean
    public MinioComponent minioInit() throws InvalidPortException, InvalidEndpointException {
        return new MinioComponent(
                new MinioClient(
                        minioProperties.getEndpoint(),
                        minioProperties.getAccessKey(),
                        minioProperties.getSecretKey()
                ),
                minioProperties.getBucketName()
        );
    }
}
