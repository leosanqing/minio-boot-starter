package com.leosanqing.starter.minio.component;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

/**
 * @author: rtliu
 * @date: 2021/1/20 11:20 上午
 * @package: minio.component
 * @description: 雪花算法工具类
 * @version: v1.0.0
 */
@Slf4j
class SnowFlakeUtil {

    private static Snowflake snowflake;

    /**
     * 提供日志方便查看
     */
    static {
        Random rand = new Random();
        long machineId = rand.nextInt(32);
        long dataCenterId = rand.nextInt(32);
        log.info("雪花算法机器id：{}", machineId);
        log.info("雪花算法数据中心id：{}", dataCenterId);
        snowflake = IdUtil.createSnowflake(machineId, dataCenterId);
    }

    public static synchronized Long getId() {
        return snowflake.nextId();
    }

}

