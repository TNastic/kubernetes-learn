# Docker 镜像构建说明

本文档记录阶段 6 的镜像构建和本地容器运行方式。本阶段只处理前端、后端应用镜像，不引入 Kubernetes YAML，也不把真实数据库密码写入仓库。

## 阶段目标

- 为 Spring Boot 后端构建可运行镜像。
- 为 React + Vite 前端构建静态资源镜像，并用 Nginx 托管。
- 在本地 Docker 中运行前端和后端容器。
- 后端容器继续通过环境变量连接服务器上的 MySQL 和 Redis。
- 前端容器通过 Nginx 把 `/api` 请求转发到后端容器。

## 产出文件

- `backend/Dockerfile`
- `backend/.dockerignore`
- `frontend/Dockerfile`
- `frontend/.dockerignore`
- `frontend/nginx.conf`
- `docs/02-docker.md`

## 后端镜像

后端使用两阶段构建：

1. `maven:3.8.8-eclipse-temurin-8` 负责下载依赖并执行 Maven 打包。
2. `eclipse-temurin:8-jre` 只负责运行最终的 Spring Boot jar。

这样做的原因是运行镜像不需要 Maven、源码和本地构建缓存，可以减少镜像内容，也更接近真实部署。

后端容器监听端口：

```text
8080
```

后端运行时必须显式注入这些环境变量：

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_REDIS_HOST
SPRING_REDIS_PORT
SPRING_REDIS_PASSWORD
```

如果环境变量缺失，应用会启动失败并暴露错误，这是预期行为。

## 前端镜像

前端同样使用两阶段构建：

1. `node:24-alpine` 执行 `npm ci` 和 `npm run build`。
2. `nginx:1.29-alpine` 托管 `dist/` 静态文件。

生产构建默认使用：

```text
VITE_API_BASE_URL=/api
```

浏览器访问前端容器时，请求路径仍然是 `/api`。Nginx 会把它转发到 Docker 网络中的后端容器：

```text
http://backend:8080
```

因此本地运行时后端容器名称需要是 `backend`，或者后续 Kubernetes Service 名称也需要与 Nginx 配置保持一致。

## Windows 本地构建镜像

在仓库根目录执行：

```powershell
docker build -t task-manager-backend:0.1.0 .\backend
docker build -t task-manager-frontend:0.1.0 .\frontend
```

查看镜像：

```powershell
docker images task-manager-backend
docker images task-manager-frontend
```

## Windows 本地运行容器

创建一个专用网络，让前端容器可以通过容器名访问后端容器：

```powershell
docker network create task-manager-net
```

运行后端容器。请把占位值替换为真实服务器连接信息，不要写入仓库文件：

```powershell
docker run --name backend --network task-manager-net -p 8080:8080 `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://<服务器公网IP>:3306/task_manager?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai" `
  -e SPRING_DATASOURCE_USERNAME="task_app" `
  -e SPRING_DATASOURCE_PASSWORD="<你的 MySQL 密码>" `
  -e SPRING_REDIS_HOST="<服务器公网IP>" `
  -e SPRING_REDIS_PORT="6379" `
  -e SPRING_REDIS_PASSWORD="<你的 Redis 密码>" `
  task-manager-backend:0.1.0
```

另开一个 PowerShell 窗口运行前端容器：

```powershell
docker run --name frontend --network task-manager-net -p 8081:80 task-manager-frontend:0.1.0
```

访问地址：

```text
http://localhost:8081
```

## 验证方式

后端健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/health/dependencies
```

前端反向代理验证：

```powershell
Invoke-RestMethod http://localhost:8081/api/health
Invoke-RestMethod http://localhost:8081/api/health/dependencies
```

查看容器日志：

```powershell
docker logs backend
docker logs frontend
```

查看容器状态：

```powershell
docker ps
```

## Linux 服务器命令参考

构建镜像：

```bash
docker build -t task-manager-backend:0.1.0 ./backend
docker build -t task-manager-frontend:0.1.0 ./frontend
```

运行容器时使用 `--env` 注入真实配置：

```bash
docker run --name backend --network task-manager-net -p 8080:8080 \
  --env SPRING_DATASOURCE_URL="jdbc:mysql://<服务器公网IP>:3306/task_manager?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai" \
  --env SPRING_DATASOURCE_USERNAME="task_app" \
  --env SPRING_DATASOURCE_PASSWORD="<你的 MySQL 密码>" \
  --env SPRING_REDIS_HOST="<服务器公网IP>" \
  --env SPRING_REDIS_PORT="6379" \
  --env SPRING_REDIS_PASSWORD="<你的 Redis 密码>" \
  task-manager-backend:0.1.0
```

## 常见问题

### 后端启动失败并提示环境变量未解析

检查 `docker run` 是否传入了所有 `SPRING_*` 环境变量。这个项目不提供默认数据库配置，缺失配置时直接失败。

### 后端连不上 MySQL 或 Redis

先看容器日志：

```powershell
docker logs backend
```

再确认服务器安全组、防火墙、数据库监听地址、Redis 访问密码和公网 IP 是否正确。

### 前端页面能打开但接口失败

确认后端容器名称是 `backend`，并且两个容器在同一个 Docker 网络中：

```powershell
docker inspect frontend
docker inspect backend
```

### 镜像构建失败

直接查看 Docker 输出的失败步骤。依赖下载失败通常是网络或镜像源问题，代码编译失败则应回到 Maven 或 Vite 的错误日志定位。
