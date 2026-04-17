# 本地开发说明：后端与前端最小闭环

本文档记录阶段 3 和阶段 4 的本地运行方式。阶段 3 跑通 Spring Boot 后端与 MySQL、Redis，阶段 4 跑通 React 前端到后端健康检查接口。

## 阶段 3：后端最小闭环

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

## 阶段 4：前端最小闭环

本阶段目标是创建 React + Vite 前端，并通过环境变量配置后端 API 入口。前端只做健康检查页面，不实现登录、注册和任务管理业务。

前端访问链路：

```text
Browser -> Vite Dev Server -> /api proxy -> Spring Boot Backend
```

本地开发时浏览器只访问 Vite 的 `http://localhost:5173`。页面请求 `/api/health/dependencies`，Vite 再把 `/api` 请求代理到后端 `http://localhost:8080`。这样可以避免浏览器跨域问题，也和后续 Ingress 统一入口的 `/api` 路由保持一致。

### 前端目录

```text
frontend/
  .env.example
  index.html
  package.json
  vite.config.js
  src/
    App.jsx
    api.js
    main.jsx
    styles.css
```

### 前端环境变量

复制前端环境变量模板：

```powershell
cd E:\learn\k8s-learn\kubernetes-learn\frontend
Copy-Item .env.example .env.local
```

`.env.local` 示例：

```text
VITE_API_BASE_URL=/api
VITE_BACKEND_TARGET=http://localhost:8080
```

变量含义：

- `VITE_API_BASE_URL` 是浏览器请求的 API 基础路径。
- `VITE_BACKEND_TARGET` 只用于 Vite 本地开发代理，指向 Spring Boot 后端。

如果 `VITE_BACKEND_TARGET` 缺失，`npm run dev` 会直接失败并提示缺少配置，不会静默切换到默认地址。

### Windows 本机运行

先启动后端：

```powershell
cd E:\learn\k8s-learn\kubernetes-learn\backend
$env:SPRING_DATASOURCE_URL="jdbc:mysql://<服务器公网IP>:3306/task_manager?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
$env:SPRING_DATASOURCE_USERNAME="task_app"
$env:SPRING_DATASOURCE_PASSWORD="<你的 MySQL 密码>"
$env:SPRING_REDIS_HOST="<服务器公网IP>"
$env:SPRING_REDIS_PORT="6379"
$env:SPRING_REDIS_PASSWORD="<你的 Redis 密码>"
mvn spring-boot:run
```

再启动前端：

```powershell
cd E:\learn\k8s-learn\kubernetes-learn\frontend
npm install
npm run dev
```

浏览器打开：

```text
http://localhost:5173
```

### 验证方式

前端构建验证：

```powershell
cd E:\learn\k8s-learn\kubernetes-learn\frontend
npm run build
```

前后端联通验证：

```powershell
Invoke-RestMethod http://localhost:5173/api/health/dependencies
```

页面验收：

- 页面显示 `阶段 4 · 前端最小闭环`。
- `API 入口` 显示 `/api`。
- 后端启动且依赖正常时，`后端状态` 显示 `UP`。
- MySQL 和 Redis 依赖列表显示各自状态。
- 点击 `重新检查` 会重新请求后端健康检查接口。

### 常见问题

前端启动失败：

```text
VITE_BACKEND_TARGET is required for local dev proxy.
```

处理方式：检查 `frontend/.env.local` 是否存在，并确认包含 `VITE_BACKEND_TARGET=http://localhost:8080`。

页面显示请求失败：

```text
Health check failed with HTTP 500.
```

处理方式：

- 先访问 `http://localhost:8080/api/health/dependencies`，确认后端接口自身是否正常。
- 检查后端环境变量是否完整。
- 检查服务器 MySQL 和 Redis 是否允许当前 Windows 本机访问。

页面无法访问后端：

```text
ECONNREFUSED
```

处理方式：

- 确认后端已经在 `localhost:8080` 启动。
- 确认 `frontend/.env.local` 的 `VITE_BACKEND_TARGET` 指向正确端口。
- 修改 `.env.local` 后重启 `npm run dev`。

### 阶段验收

- `frontend/` 中有 React + Vite 前端项目。
- 前端 API 地址通过环境变量配置。
- 本地开发通过 Vite proxy 处理跨域。
- 首页可以调用 `/api/health/dependencies`。
- 页面可以显示后端、MySQL、Redis 的连接状态。
- `npm run build` 可以通过。

## 参考资料

- Spring Boot 2.7 文档：https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/
- MyBatis-Plus 安装文档：https://baomidou.com/en/getting-started/install/
- MyBatis Spring Boot 兼容性说明：https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/
- Vite 环境变量文档：https://vite.dev/guide/env-and-mode
- Vite 开发服务器代理文档：https://vite.dev/config/server-options#server-proxy
