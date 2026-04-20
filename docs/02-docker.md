# Docker 镜像构建说明

本文档记录阶段 6 的镜像构建和容器运行方式。本阶段只处理前端、后端应用镜像，不引入 Kubernetes YAML，也不把真实数据库密码写入仓库。

如果 Windows 本机没有安装 Docker，可以直接在 Linux 服务器上完成镜像构建和容器运行。本项目后续会进入 K3s 阶段，因此在服务器上熟悉 Docker 命令也更贴近后续部署流程。

## 阶段目标

- 为 Spring Boot 后端构建可运行镜像。
- 为 React + Vite 前端构建静态资源镜像，并用 Nginx 托管。
- 支持在 Linux 服务器上构建和运行前端、后端容器。
- 后端容器继续通过环境变量连接 MySQL 和 Redis。
- 前端容器通过 Nginx 把 `/api` 请求转发到后端容器。

## 产出文件

- `backend/Dockerfile`
- `backend/.dockerignore`
- `frontend/Dockerfile`
- `frontend/.dockerignore`
- `frontend/nginx.conf`
- `docs/02-docker.md`

## 镜像设计

后端使用两阶段构建：

1. `maven:3.8.8-eclipse-temurin-8` 下载依赖并执行 Maven 打包。
2. `eclipse-temurin:8-jre` 只负责运行最终的 Spring Boot jar。

前端同样使用两阶段构建：

1. `node:24-alpine` 执行 `npm ci` 和 `npm run build`。
2. `nginx:1.29-alpine` 托管 `dist/` 静态文件。

后端容器监听端口：

```text
8080
```

前端容器监听端口：

```text
80
```

生产构建默认使用：

```text
VITE_API_BASE_URL=/api
```

浏览器访问前端时，请求路径仍然是 `/api`。Nginx 会把它转发到 Docker 网络中的后端容器：

```text
http://backend:8080
```

因此本地或服务器 Docker 运行时，后端容器名称需要是 `backend`，并且前后端容器需要在同一个 Docker 网络里。

## 后端运行配置

后端运行时必须显式注入这些环境变量：

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_REDIS_HOST
SPRING_REDIS_PORT
SPRING_REDIS_PASSWORD
```

如果环境变量缺失，应用会启动失败并暴露错误，这是预期行为。不要为了让容器启动成功而写默认数据库地址、默认密码或模拟成功逻辑。

## 服务器构建运行总流程

服务器上完整流程如下：

```text
Windows 本机写代码
  |
  | git push 或 scp 上传
  v
Linux 服务器获取代码
  |
  | docker build
  v
服务器本地镜像
  |
  | docker run
  v
服务器容器运行前端和后端
```

## 登录服务器

在 Windows PowerShell 中执行：

```powershell
ssh root@你的服务器公网IP
```

登录后确认系统类型：

```bash
cat /etc/os-release
```

后续命令默认使用 `sudo docker`，这样不需要额外配置 Docker 用户组。

## 在服务器安装 Docker

如果服务器已经安装 Docker，可以跳过本节，直接验证：

```bash
sudo docker version
sudo docker run hello-world
```

### Ubuntu 安装 Docker

适用于 Ubuntu 22.04、24.04 等系统：

```bash
sudo apt update
sudo apt install -y ca-certificates curl

sudo install -m 0755 -d /etc/apt/keyrings

sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc

sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update

sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

启动 Docker：

```bash
sudo systemctl enable --now docker
```

验证安装：

```bash
sudo docker run hello-world
```

### CentOS / Rocky / AlmaLinux 安装 Docker

适用于使用 `dnf` 的系统：

```bash
sudo dnf -y install dnf-plugins-core

sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo systemctl enable --now docker
```

验证安装：

```bash
sudo docker run hello-world
```

## 把项目代码放到服务器

推荐使用 Git。服务器上执行：

```bash
mkdir -p /opt/apps
cd /opt/apps
```

如果代码已经推到远程仓库：

```bash
git clone 你的Git仓库地址 kubernetes-learn
cd kubernetes-learn
```

如果暂时还没有远程仓库，也可以从 Windows 上传整个项目目录。Windows PowerShell 执行：

```powershell
scp -r E:\learn\k8s-learn\kubernetes-learn root@你的服务器公网IP:/opt/apps/
```

然后在服务器上进入目录：

```bash
cd /opt/apps/kubernetes-learn
```

确认阶段 6 文件存在：

```bash
ls backend/Dockerfile
ls frontend/Dockerfile
ls frontend/nginx.conf
ls docs/02-docker.md
```

## 在服务器构建镜像

在服务器项目根目录执行：

```bash
sudo docker build -t task-manager-backend:0.1.0 ./backend
sudo docker build -t task-manager-frontend:0.1.0 ./frontend
```

查看镜像：

```bash
sudo docker images task-manager-backend
sudo docker images task-manager-frontend
```

如果构建失败，直接查看 Docker 输出的失败步骤。依赖下载失败通常是网络或镜像源问题；代码编译失败则回到 Maven 或 Vite 的错误日志定位。

## 创建 Docker 网络

前端 Nginx 需要通过容器名 `backend` 访问后端，因此两个容器需要加入同一个网络：

```bash
sudo docker network create task-manager-net
```

如果提示网络已存在，可以继续下一步。

## 运行后端容器

先删除旧后端容器，避免同名冲突：

```bash
sudo docker rm -f backend
```

运行后端容器：

```bash
sudo docker run -d \
  --name backend \
  --network task-manager-net \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://你的服务器公网IP:3306/task_manager?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai" \
  -e SPRING_DATASOURCE_USERNAME="task_app" \
  -e SPRING_DATASOURCE_PASSWORD="你的MySQL密码" \
  -e SPRING_REDIS_HOST="你的服务器公网IP" \
  -e SPRING_REDIS_PORT="6379" \
  -e SPRING_REDIS_PASSWORD="你的Redis密码" \
  task-manager-backend:0.1.0
```

注意：如果 MySQL 和 Redis 安装在服务器宿主机上，容器里的 `127.0.0.1` 不是宿主机，而是容器自己。所以这里不要写 `127.0.0.1`。可以先写服务器公网 IP 或内网 IP，后续再根据网络和安全组情况优化。

查看后端状态：

```bash
sudo docker ps
sudo docker logs backend
```

验证后端接口：

```bash
curl http://127.0.0.1:8080/api/health
curl http://127.0.0.1:8080/api/health/dependencies
```

## 运行前端容器

先删除旧前端容器：

```bash
sudo docker rm -f frontend
```

如果服务器 80 端口没有被占用，可以直接映射到 80：

```bash
sudo docker run -d \
  --name frontend \
  --network task-manager-net \
  -p 80:80 \
  task-manager-frontend:0.1.0
```

浏览器访问：

```text
http://你的服务器公网IP/
```

验证前端 Nginx 代理：

```bash
curl http://127.0.0.1/api/health
curl http://127.0.0.1/api/health/dependencies
```

如果服务器 80 端口已经被占用，先使用 8081：

```bash
sudo docker run -d \
  --name frontend \
  --network task-manager-net \
  -p 8081:80 \
  task-manager-frontend:0.1.0
```

浏览器访问：

```text
http://你的服务器公网IP:8081/
```

验证代理：

```bash
curl http://127.0.0.1:8081/api/health
curl http://127.0.0.1:8081/api/health/dependencies
```

## 一套完整执行顺序

假设代码已经位于 `/opt/apps/kubernetes-learn`，可以按下面顺序执行：

```bash
cd /opt/apps/kubernetes-learn

sudo docker build -t task-manager-backend:0.1.0 ./backend
sudo docker build -t task-manager-frontend:0.1.0 ./frontend

sudo docker network create task-manager-net

sudo docker rm -f backend
sudo docker rm -f frontend

sudo docker run -d \
  --name backend \
  --network task-manager-net \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://你的服务器公网IP:3306/task_manager?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai" \
  -e SPRING_DATASOURCE_USERNAME="task_app" \
  -e SPRING_DATASOURCE_PASSWORD="你的MySQL密码" \
  -e SPRING_REDIS_HOST="你的服务器公网IP" \
  -e SPRING_REDIS_PORT="6379" \
  -e SPRING_REDIS_PASSWORD="你的Redis密码" \
  task-manager-backend:0.1.0

sudo docker run -d \
  --name frontend \
  --network task-manager-net \
  -p 80:80 \
  task-manager-frontend:0.1.0

curl http://127.0.0.1:8080/api/health/dependencies
curl http://127.0.0.1/api/health/dependencies
```

成功标准：

```text
sudo docker ps 能看到 backend 和 frontend 都是 Up
curl http://127.0.0.1:8080/api/health/dependencies 返回后端、MySQL、Redis 状态
curl http://127.0.0.1/api/health/dependencies 能通过前端 Nginx 代理访问后端
浏览器可以打开 http://服务器公网IP/
注册、登录、任务 CRUD 能正常使用
```

## Windows 本地 Docker 命令参考

如果以后 Windows 本机安装了 Docker，也可以在仓库根目录执行：

```powershell
docker build -t task-manager-backend:0.1.0 .\backend
docker build -t task-manager-frontend:0.1.0 .\frontend
```

创建网络：

```powershell
docker network create task-manager-net
```

运行后端：

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

运行前端：

```powershell
docker run --name frontend --network task-manager-net -p 8081:80 task-manager-frontend:0.1.0
```

访问地址：

```text
http://localhost:8081
```

## 常用排查命令

查看运行中的容器：

```bash
sudo docker ps
```

查看所有容器，包括已经退出的：

```bash
sudo docker ps -a
```

查看后端日志：

```bash
sudo docker logs backend
```

持续跟踪后端日志：

```bash
sudo docker logs -f backend
```

查看前端 Nginx 日志：

```bash
sudo docker logs frontend
```

进入后端容器：

```bash
sudo docker exec -it backend sh
```

进入前端容器：

```bash
sudo docker exec -it frontend sh
```

查看容器网络信息：

```bash
sudo docker inspect backend
sudo docker inspect frontend
```

删除旧容器：

```bash
sudo docker rm -f backend
sudo docker rm -f frontend
```

重新构建镜像：

```bash
sudo docker build -t task-manager-backend:0.1.0 ./backend
sudo docker build -t task-manager-frontend:0.1.0 ./frontend
```

## 常见问题

### 后端启动失败并提示环境变量未解析

检查 `docker run` 是否传入了所有 `SPRING_*` 环境变量。本项目不提供默认数据库配置，缺失配置时直接失败。

### 后端连不上 MySQL 或 Redis

先看容器日志：

```bash
sudo docker logs backend
```

再确认服务器安全组、防火墙、数据库监听地址、Redis 访问密码和 IP 地址是否正确。

如果 MySQL 或 Redis 只监听 `127.0.0.1`，容器通常无法通过服务器公网 IP 访问它们。需要调整数据库监听地址、防火墙规则，或者后续在 Kubernetes 阶段改为集群内部服务。

### 前端页面能打开但接口失败

确认后端容器名称是 `backend`，并且两个容器在同一个 Docker 网络中：

```bash
sudo docker inspect frontend
sudo docker inspect backend
```

也可以直接从前端容器内访问后端：

```bash
sudo docker exec -it frontend sh
```

进入容器后执行：

```bash
wget -qO- http://backend:8080/api/health
```

### 80 端口无法绑定

检查端口占用：

```bash
sudo ss -lntp | grep ':80'
```

如果 80 已被占用，可以临时用 `-p 8081:80` 运行前端。

### 镜像构建失败

直接查看 Docker 输出的失败步骤。不要添加 mock 或跳过真实构建。常见方向：

- 拉取基础镜像失败：检查服务器网络、DNS、镜像源。
- Maven 依赖下载失败：检查 Maven 仓库访问。
- `npm ci` 失败：检查 `package-lock.json` 和 npm 网络访问。
- `npm run build` 失败：回到 Vite 输出日志定位。
