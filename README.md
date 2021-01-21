

# 前言

SpringBoot 基于配置开发，封装了非常多的Starter ，比如我们最常用的 `spring-boot-starter-web`，`spring-boot-starter-data-redis`，SpringBoot 是怎么加载这些配置的，我们就通过自己封装 Starter 来理解下吧

项目地址: [https://github.com/leosanqing/minio-boot-starter](https://github.com/leosanqing/minio-boot-starter)



我们使用 MinIO 作为封装对象，MinIO 是一个分布式的存储器，跟FastDFS 和 HDFS 差不多

项目结构

![](img/Xnip2021-01-21_09-48-58.jpg)



# maven依赖

```xml
<parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.4.0</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>


<dependencies>
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <version>3.0.10</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
        <version>3.8</version>
    </dependency>
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.3.7</version>
    </dependency>
</dependencies>
```

# 基础类

```java
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
```



```java
/**
 * @author: rtliu
 * @date: 2021/1/20 10:54 上午
 * @package: com.leosanqing.starter.minio.config
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
```

```java
/**
 * @author: rtliu
 * @date: 2021/1/20 11:20 上午
 * @package: com.leosanqing.starter.minio.component
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
```

```java
/**
 * @author: rtliu
 * @date: 2021/1/20 11:04 上午
 * @package: com.leosanqing.starter.minio
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
```

```java
/**
 * @author: rtliu
 * @date: 2021/1/20 11:01 上午
 * @package: com.leosanqing.starter.minio
 * @description: Minio 自动配置类
 * @version: v1.0.0
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnExpression("${cetcxl.minio.enabled:true}")
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
```

创建Spring.factories文件

```yaml
org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.leosanqing.starter.minio.MinioConfigAutoConfig
```

#  打包

直接使用 Maven 插件，打包到本地仓库

![](img/Xnip2021-01-21_10-31-45.jpg)

# 新建项目引入starter

```xml
<dependencies>
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>minio-boot-starter</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-core</artifactId>
        <version>9.0.35</version>
    </dependency>
</dependencies>
```

引入我们的配置

```yaml
leosanqing:
  minio:
    endpoint: http://127.0.0.1:9000
    secretKey: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
    accessKey: AKIAIOSFODNN7EXAMPLE
    bucketName: face
```



##  controller

```java
/**
 * @author: rtliu
 * @date: 2021/1/21 10:14 上午
 * @package: com.leosanqing.minio.demo.controller
 * @description: 工具类
 * @version: v1.0.0
 */

@Validated
@RestController
public class UtilController  {

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
```

## 调用上传与下载

![](img/Xnip2021-01-21_10-40-09.jpg)

![Xnip2021-01-21_10-42-30](img/Xnip2021-01-21_10-42-30.jpg)

![](img/Xnip2021-01-21_10-42-37.jpg)

# 总结

至此，starter 是已经 封装完成并且能启动，但是 starter 启动的原理是什么呢？