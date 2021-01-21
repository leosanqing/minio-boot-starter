package com.leosanqing.starter.minio.component;

import com.google.common.base.Strings;
import com.leosanqing.starter.minio.entity.FileEntity;
import com.leosanqing.starter.minio.exception.CommonMinioException;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author: rtliu
 * @date: 2021/1/20 11:04 上午
 * @package: minio
 * @description: Minio组件
 * @version: v1.0.0
 */
@Slf4j
public class MinioComponent {
    private final MinioClient minioClient;
    private final String bucketName;

    public MinioComponent(MinioClient minioClient, String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        try {
            if (!this.minioClient.bucketExists(this.bucketName)) {
                this.minioClient.makeBucket(this.bucketName);
            }
        } catch (Exception e) {
            log.error("MinioComponent bucket create", e);
            throw new CommonMinioException(e.getMessage());
        }
    }

    public String fileUpload(File file) {
        return this.fileUpload(file, null);

    }

    public String fileUpload(File file, String storeName) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            MultipartFile multipartFile = new MockMultipartFile(
                    file.getName(),
                    file.getName(),
                    ContentType.APPLICATION_OCTET_STREAM.toString(),
                    fileInputStream);
            return this.fileUpload(multipartFile, storeName);
        } catch (Exception e) {
            log.error("MinioComponent fileUpload", e);
            throw new CommonMinioException(e.getMessage());
        }
    }

    public String fileUpload(MultipartFile file) {
        return this.fileUpload(file, null);
    }

    /**
     * 指定文件存储路径并且上传文件
     *
     * @param file
     * @param storeName
     * @return
     */
    public String fileUpload(MultipartFile file, String storeName) {
        //设置minio中该文件的实际存储名称，暂时定为由系统生成唯一值的方式
        if (Strings.isNullOrEmpty(storeName)) {
            storeName = SnowFlakeUtil.getId() + "_" + file.getOriginalFilename();
        }

        try {
            this.minioClient.putObject(
                    this.bucketName,
                    storeName,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
            return Base64.getEncoder().encodeToString(storeName.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("MinioComponent fileUpload", e);
            throw new CommonMinioException(e.getMessage());
        }
    }

    /**
     * minio获取文件并且存储在本地地址
     *
     * @param storeName 文件存储路径
     * @param filePath  文件本地磁盘路径
     */
    public String fileGetter(String storeName, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdirs();
            }
            storeName = new String(Base64.getDecoder().decode(storeName), StandardCharsets.UTF_8);
            String fileName = storeName.substring(storeName.indexOf("_") + 1);
            this.minioClient.getObject(this.bucketName, storeName, filePath + "/" + fileName);
            return fileName;
        } catch (Exception e) {
            log.error("MinioComponent fileGetter", e);
            throw new CommonMinioException(e.getMessage());
        }
    }

    /**
     * 根据文件存储路径获取FIleEntity对象
     *
     * @param storeName 文件名称
     * @return 文件对象
     */
    public FileEntity fileGetter(String storeName) {
        try {
            storeName = new String(Base64.getDecoder().decode(storeName), StandardCharsets.UTF_8);
            String fileName = storeName.substring(storeName.indexOf("_") + 1);
            InputStream inputStream = this.minioClient.getObject(this.bucketName, storeName);
            return FileEntity
                    .builder()
                    .fileName(fileName)
                    .inputStream(inputStream)
                    .build();
        } catch (Exception e) {
            log.error("MinioComponent fileGetter", e);
            throw new CommonMinioException(e.getMessage());
        }
    }

    /**
     * 删除文件
     * @param storeName 文件名称
     */
    public void fileRemove(String storeName) {
        try {
            storeName = new String(Base64.getDecoder().decode(storeName), StandardCharsets.UTF_8);
            this.minioClient.removeObject(this.bucketName, storeName);
        } catch (Exception e) {
            log.error("MinioComponent fileRemove", e);
            throw new CommonMinioException(e.getMessage());
        }
    }
}
