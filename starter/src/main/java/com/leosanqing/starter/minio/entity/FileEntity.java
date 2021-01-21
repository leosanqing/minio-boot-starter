package com.leosanqing.starter.minio.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.InputStream;

/**
 * @author leosanqing
 * @description 文件对象
 * @date 2020/12/9 11:02
 */
@ToString
@Getter
@Setter
@Builder
public class FileEntity {
    private String fileName;
    private InputStream inputStream;
}
