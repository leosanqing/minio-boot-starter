package com.leosanqing.starter.minio.exception;

import lombok.Getter;

/**
 * @author: rtliu
 * @date: 2021/1/20 11:16 上午
 * @package: minio.exception
 * @description: Minio 异常类
 * @version: v1.0.0
 */
@Getter
public class CommonMinioException extends RuntimeException {
    public CommonMinioException(String message) {
        super(message);
    }
}
