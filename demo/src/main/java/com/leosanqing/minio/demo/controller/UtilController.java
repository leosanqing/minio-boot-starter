package com.leosanqing.minio.demo.controller;

import cn.hutool.core.io.IoUtil;
import com.leosanqing.starter.minio.component.MinioComponent;
import com.leosanqing.starter.minio.entity.FileEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 * @author: rtliu
 * @date: 2021/1/21 10:14 上午
 * @package: com.leosanqing.minio.demo.controller
 * @description: 工具类
 * @version: v1.0.0
 */

@Validated
@RestController
public class UtilController {

    @Autowired
    private MinioComponent minioComponent;


    @PostMapping("/api/util/upload/minio")
    public String upload(
            @RequestParam("file") MultipartFile file
    ) {
        return minioComponent.fileUpload(file);
    }

    @GetMapping("/api/util/download/minio")
    public void download(String storeName, HttpServletResponse response) {
        FileEntity fileEntity = minioComponent.fileGetter(storeName);
        InputStream inputStream = fileEntity.getInputStream();
        byte[] buffer = IoUtil.readBytes(inputStream);
        try {
            response.setCharacterEncoding("utf-8");
            response.setHeader(
                    "Content-disposition",
                    "attachment;filename=" + URLEncoder.encode(fileEntity.getFileName(), "UTF-8"));
            ServletOutputStream out = response.getOutputStream();
            out.write(buffer);
            inputStream.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("系统异常");
        }
    }
}
