# 阶段 3：后端最小闭环

本阶段目标是创建 Java 8 兼容的 Spring Boot 后端，并让后端通过环境变量连接腾讯云服务器上的 MySQL 和 Redis。

后端分层：

```text
Controller -> Service -> Mapper -> MySQL
                    |
                    v
                  Redis
```

## 技术选型

- Java 8
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.7
- MySQL Connector/J 8.0.33
- Spring Data Redis
- Maven

MyBatis-Plus 官方文档说明 3.x 基于 JDK 8，Spring Boot 2 使用 `mybatis-plus-boot-starter`。本项目不额外引入 `mybatis-spring-boot-starter`，避免 MyBatis 相关依赖版本冲突。

## 后端目录

```text
backend/
  pom.xml
  src/main/java/com/example/taskmanager/
    TaskManagerApplication.java
    controller/
      HealthController.java
    dto/
      DependencyStatusResponse.java
      HealthResponse.java
    mapper/
      HealthMapper.java
    service/
      HealthService.java
      impl/
        HealthServiceImpl.java
  src/main/resources/
    application.yml
```

## 配置原则

数据库地址、用户名、密码、Redis 密码都不写死在代码里，只通过环境变量注入：

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  redis:
    host: ${SPRING_REDIS_HOST}
    port: ${SPRING_REDIS_PORT}
    password: ${SPRING_REDIS_PASSWORD}
```

如果环境变量缺失，应用应该启动失败并暴露错误，而不是静默使用默认数据库。

## Windows 本机运行

进入后端目录：

```powershell
cd E:\learn\k8s-learn\kubernetes-learn\backend
```

设置环境变量。把占位值替换成你的真实服务器信息：

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://<服务器公网IP>:3306/task_manager?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
$env:SPRING_DATASOURCE_USERNAME="task_app"
$env:SPRING_DATASOURCE_PASSWORD="<你的 MySQL 密码>"
$env:SPRING_REDIS_HOST="<服务器公网IP>"
$env:SPRING_REDIS_PORT="6379"
$env:SPRING_REDIS_PASSWORD="<你的 Redis 密码>"
```

启动：

```powershell
mvn spring-boot:run
```

## 验证接口

后端启动后访问：

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/health/mysql
Invoke-RestMethod http://localhost:8080/api/health/redis
Invoke-RestMethod http://localhost:8080/api/health/dependencies
```

也可以用浏览器打开：

```text
http://localhost:8080/api/health
http://localhost:8080/api/health/mysql
http://localhost:8080/api/health/redis
http://localhost:8080/api/health/dependencies
```

期望结果：

- `/api/health` 返回后端自身状态。
- `/api/health/mysql` 通过 MyBatis-Plus 管理的 Mapper 执行 `SELECT 1`。
- `/api/health/redis` 通过 Redis 连接执行 `PING`。
- `/api/health/dependencies` 同时验证 MySQL 和 Redis。

## 当前接口设计

`GET /api/health`

```json
{
  "status": "UP",
  "checkedAt": "2026-04-17T00:00:00Z",
  "dependencies": []
}
```

`GET /api/health/mysql`

```json
{
  "name": "mysql",
  "status": "UP",
  "detail": "SELECT 1"
}
```

`GET /api/health/redis`

```json
{
  "name": "redis",
  "status": "UP",
  "detail": "PONG"
}
```

## 常见问题

环境变量缺失：

```text
Could not resolve placeholder 'SPRING_DATASOURCE_URL'
```

处理方式：重新检查当前 PowerShell 窗口是否设置了所有环境变量。

MySQL 连接失败：

```text
Communications link failure
Access denied for user
```

处理方式：

- 检查腾讯云安全组是否允许当前 Windows 公网 IP 访问 `3306`。
- 检查 `task_app` 密码是否正确。
- 检查 MySQL 是否监听 `0.0.0.0:3306`。

Redis 连接失败：

```text
Unable to connect to Redis server
NOAUTH Authentication required
```

处理方式：

- 检查腾讯云安全组是否允许当前 Windows 公网 IP 访问 `6379`。
- 检查 Redis `requirepass` 和环境变量是否一致。
- 检查 Redis 是否监听 `0.0.0.0:6379`。

## 阶段验收

- `mvn test` 可以通过。
- `mvn spring-boot:run` 可以启动后端。
- `/api/health` 返回 `UP`。
- `/api/health/mysql` 能访问服务器 MySQL。
- `/api/health/redis` 能访问服务器 Redis。
- 后端代码按 `controller -> service -> mapper` 分层。
- 数据库访问层使用 MyBatis-Plus starter 管理。

## 参考资料

- Spring Boot 2.7 文档：https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/
- MyBatis-Plus 安装文档：https://baomidou.com/en/getting-started/install/
- MyBatis Spring Boot 兼容性说明：https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/
