# 个人任务管理系统 Kubernetes 实战项目计划

## 0. 当前进度

当前阶段：阶段 2，服务器数据库准备。

进度条：

```text
[=>---------] 1 / 11 个完整阶段已完成
```

阶段状态：

| 阶段 | 主题 | 状态 |
| --- | --- | --- |
| 1 | 项目规划与仓库初始化 | 已完成 |
| 2 | 服务器数据库准备 | 进行中 |
| 3 | 后端最小闭环 | 未开始 |
| 4 | 前端最小闭环 | 未开始 |
| 5 | 业务 MVP 开发 | 未开始 |
| 6 | Docker 镜像构建 | 未开始 |
| 7 | K3s 安装与集群准备 | 未开始 |
| 8 | 部署 MySQL 和 Redis | 未开始 |
| 9 | 部署后端和前端服务 | 未开始 |
| 10 | Ingress、域名与 HTTPS | 未开始 |
| 11 | 上线、日志排查、滚动更新与回滚 | 未开始 |

说明：

- 阶段 1 已经完成项目计划文档、`README.md`、`.gitignore` 和基础目录。
- 阶段 2 开始准备服务器 MySQL 和 Redis。
- 后续每完成一个阶段，都要更新本进度表和进度条。

## 1. 项目目标

本项目的目标不是快速完成一个任务管理系统，而是通过一个接近真实上线流程的应用，系统学习从本地开发到云服务器部署的完整链路。

最终希望你能掌握：

- 使用 Docker 为前端、后端服务构建镜像。
- 使用 K3s 搭建轻量级 Kubernetes 环境。
- 编写和理解 Kubernetes YAML，而不是只会复制命令。
- 使用 Deployment 管理无状态服务。
- 使用 Service 暴露集群内部服务。
- 使用 ConfigMap 管理非敏感配置。
- 使用 Secret 管理密码、Token、证书等敏感信息。
- 使用 PVC 为 MySQL、Redis 等有状态组件提供持久化存储。
- 使用 Ingress 通过域名访问服务。
- 配置 HTTPS，让服务可以通过安全连接访问。
- 使用日志、事件、Pod 状态排查部署问题。
- 理解滚动更新、版本发布和回滚。
- 在 2 核 2G Linux 云服务器上进行资源受限环境下的部署取舍。

这个项目的重点是学习部署、运维和排障能力。业务功能只做最小可用版本，避免把精力消耗在复杂业务逻辑上。

## 2. 最小可用功能 MVP

MVP 只实现个人任务管理系统最核心的功能：

- 用户注册。
- 用户登录。
- 用户退出登录。
- 创建任务。
- 查看任务列表。
- 修改任务标题、描述、状态。
- 删除任务。
- 按任务状态筛选：
  - 待完成。
  - 已完成。
- 后端提供健康检查接口。
- 前端可以通过环境配置访问后端 API。
- 后端可以通过环境配置连接 MySQL 和 Redis。

暂不实现：

- 多人协作。
- 复杂权限系统。
- 邮件通知。
- 文件上传。
- 第三方登录。
- 复杂标签、项目、日历、提醒功能。
- 微服务拆分。

## 3. 技术架构

整体架构如下：

```text
Browser
  |
  | HTTPS + Domain
  v
Ingress Controller
  |
  v
Frontend Service
  |
  v
React Frontend Pod
  |
  | HTTP API
  v
Backend Service
  |
  v
Spring Boot Backend Pod
  |                 |
  | JDBC            | Redis Client
  v                 v
MySQL Service      Redis Service
  |                 |
  v                 v
MySQL Pod + PVC    Redis Pod + PVC
```

推荐技术选型：

- 前端：
  - React。
  - Vite。
  - Nginx 容器托管静态文件。
- 后端：
  - Java 8。
  - Spring Boot 2.x，选择兼容 Java 8 的版本。
  - Spring Web。
  - Spring Security 或简单 JWT 登录机制。
  - Spring Data JPA 或 MyBatis。
  - MySQL Driver。
  - Redis Client。
- 数据库：
  - MySQL 8。
- 缓存：
  - Redis 7。
- 容器：
  - Docker。
  - 多阶段构建。
- Kubernetes：
  - K3s。
  - Deployment。
  - Service。
  - ConfigMap。
  - Secret。
  - PersistentVolumeClaim。
  - Ingress。
  - Namespace。
  - Resource requests / limits。
  - Readiness Probe。
  - Liveness Probe。
- HTTPS：
  - Ingress Controller。
  - cert-manager 或云厂商证书。
  - Let's Encrypt。

后端兼容性约束：

- 后端代码必须以 Java 8 为目标运行环境。
- 不使用 Java 9 及以上语法，例如 `var`、`record`、`sealed class`、`switch` 表达式、文本块。
- Maven 编译目标使用 `source=1.8` 和 `target=1.8`。
- Spring Boot 不使用要求 Java 17 的 3.x 版本，优先使用 Spring Boot 2.7.x 或其他兼容 Java 8 的 2.x 版本。
- 依赖选择时优先检查 Java 8 兼容性。

数据库使用约束：

- Windows 本机不安装 MySQL 和 Redis。
- 开发阶段直接连接服务器上的 MySQL 和 Redis。
- 服务器数据库账号、密码、端口不写入 Git，只放在本地环境变量、服务器环境变量或 Kubernetes Secret 中。
- 为了学习 PVC，K3s 阶段仍会在服务器的 K3s 集群中部署 MySQL 和 Redis，并通过 PVC 保存数据。
- 如果后续你决定使用服务器宿主机上已有的 MySQL 和 Redis，而不是 K3s 内部数据库，则需要额外学习 Kubernetes 访问外部服务的方式，例如 `ExternalName` Service 或手动 Endpoint。

## 4. 项目目录结构

建议目录结构：

```text
kubernetes-learn/
  docs/
    00-project-plan.md
    01-server-database.md
    01-local-development.md
    02-docker.md
    03-k3s-install.md
    04-kubernetes-yaml.md
    05-ingress-https.md
    06-observability-debugging.md
    07-rolling-update-rollback.md

  frontend/
    Dockerfile
    nginx.conf
    package.json
    src/

  backend/
    Dockerfile
    pom.xml
    src/

  deploy/
    k8s/
      namespace.yaml

      configmap.yaml
      secret.example.yaml

      mysql/
        mysql-pvc.yaml
        mysql-deployment.yaml
        mysql-service.yaml

      redis/
        redis-pvc.yaml
        redis-deployment.yaml
        redis-service.yaml

      backend/
        backend-deployment.yaml
        backend-service.yaml

      frontend/
        frontend-deployment.yaml
        frontend-service.yaml

      ingress/
        ingress.yaml
        certificate.yaml

    scripts/
      apply-all.ps1
      apply-all.sh
      rollback-backend.sh

  .env.example
  .gitignore
  README.md
```

目录设计原则：

- `frontend/` 只放前端代码和前端镜像构建文件。
- `backend/` 只放后端代码和后端镜像构建文件。
- `deploy/k8s/` 只放 Kubernetes 部署文件。
- `docs/` 记录每个阶段的学习笔记、操作步骤和问题复盘。
- `secret.example.yaml` 可以提交到 Git，但真实 `secret.yaml` 不提交。
- 每个阶段都要留下可复现的文件，而不是只靠命令历史。

## 5. 11 个阶段的学习计划

### 阶段 1：项目规划与仓库初始化

目标：

- 明确项目边界。
- 建立目录结构。
- 建立文档驱动的学习方式。

主要任务：

- 创建 `docs/00-project-plan.md`。
- 创建基础目录。
- 创建 `README.md`。
- 创建 `.gitignore`。

阶段产出文件：

- `docs/00-project-plan.md`
- `README.md`
- `.gitignore`

要学习的 Kubernetes 概念：

- Kubernetes 解决什么问题。
- 集群、节点、Pod、控制平面的基本关系。
- 为什么真实项目需要部署清单和文档。

### 阶段 2：服务器数据库准备

目标：

- 在服务器准备 MySQL 和 Redis。
- Windows 本机不安装 MySQL 和 Redis，只通过网络连接服务器数据库。
- 先建立最小的数据依赖，再开发后端。

主要任务：

- 在服务器安装或启动 MySQL。
- 在服务器安装或启动 Redis。
- 创建项目数据库和数据库用户。
- 配置 Redis 密码或访问限制。
- 配置服务器防火墙或安全组，只开放必要端口。
- 在 Windows 本机验证可以连接服务器 MySQL。
- 在 Windows 本机验证可以连接服务器 Redis。
- 记录数据库连接信息模板，但不提交真实密码。

阶段产出文件：

- `.env.example`
- `docs/01-server-database.md`

要学习的 Kubernetes 概念：

- ConfigMap 和 Secret 的用途边界。
- 为什么数据库地址、用户名、密码不能写死在代码里。
- 应用依赖外部服务时，部署配置要如何表达。

### 阶段 3：后端最小闭环

目标：

- 创建 Java 8 兼容的 Spring Boot 后端。
- 后端直接连接服务器 MySQL 和 Redis。
- 先跑通后端健康检查和基础依赖连接，不做完整前端。

主要任务：

- 创建 Spring Boot 2.x 后端项目。
- 配置 Maven Java 8 编译目标。
- 添加 Web、MySQL、Redis 相关依赖。
- 后端连接服务器 MySQL。
- 后端连接服务器 Redis。
- 实现健康检查接口。
- 实现数据库连接验证接口或启动时检查。
- 实现 Redis 连接验证接口或启动时检查。

阶段产出文件：

- `backend/`
- `docs/01-local-development.md`

要学习的 Kubernetes 概念：

- 应用配置和运行环境分离。
- 为什么 Kubernetes 中不应该把配置写死在代码里。
- 健康检查对容器编排的重要性。

### 阶段 4：前端最小闭环

目标：

- 创建 React 前端。
- 前端调用后端健康检查接口。
- 先完成前后端最小联通，不做完整业务页面。

主要任务：

- 创建 React + Vite 项目。
- 配置前端环境变量。
- 创建最简单的首页。
- 调用后端健康检查接口。
- 显示后端连接状态。
- 处理跨域问题。

阶段产出文件：

- `frontend/`
- `frontend/src/`
- `docs/01-local-development.md` 更新

要学习的 Kubernetes 概念：

- Service 稳定入口的意义。
- 前端配置和后端地址分离。
- 跨域问题和 Ingress 统一入口之间的关系。

### 阶段 5：业务 MVP 开发

目标：

- 实现最小可用任务管理功能。
- 控制业务复杂度，只为后续部署提供真实服务。

主要任务：

- 实现用户注册和登录。
- 实现任务 CRUD。
- 实现任务状态筛选。
- 增加后端 API 基础校验。
- 前端完成基础页面。

阶段产出文件：

- `backend/src/`
- `frontend/src/`
- `docs/01-local-development.md` 更新

要学习的 Kubernetes 概念：

- 无状态服务和有状态服务的区别。
- 前端、后端、数据库、缓存在集群中的职责边界。
- 为什么登录状态不应该只依赖单个后端 Pod 的本地内存。

### 阶段 6：Docker 镜像构建

目标：

- 为前端和后端构建 Docker 镜像。
- 理解镜像、容器、端口、环境变量和日志输出。

主要任务：

- 编写前端 `Dockerfile`。
- 编写后端 `Dockerfile`。
- 使用 Docker 本地运行前端和后端。
- 本地容器中的后端继续连接服务器 MySQL 和 Redis。
- 验证容器到服务器数据库的网络访问。

阶段产出文件：

- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `backend/Dockerfile`
- `docs/02-docker.md`

要学习的 Kubernetes 概念：

- Kubernetes 运行的是容器，不是源码。
- 镜像标签和版本发布的关系。
- 容器标准输出日志为什么重要。
- 容器内端口和服务访问端口的区别。

### 阶段 7：K3s 安装与集群准备

目标：

- 在 2 核 2G Linux 云服务器上安装 K3s。
- 能使用 `kubectl` 查看集群状态。

主要任务：

- 准备 Linux 服务器。
- 安装 K3s。
- 配置本地或服务器上的 `kubectl`。
- 创建项目 Namespace。
- 确认默认 Ingress Controller 状态。

阶段产出文件：

- `docs/03-k3s-install.md`
- `deploy/k8s/namespace.yaml`

要学习的 Kubernetes 概念：

- Node。
- Namespace。
- Pod。
- kubectl。
- K3s 与标准 Kubernetes 的关系。
- Ingress Controller 在 K3s 中的默认安装情况。

### 阶段 8：部署 MySQL 和 Redis

目标：

- 在 K3s 中部署有状态组件。
- 理解 PVC 和数据持久化。

主要任务：

- 编写 MySQL PVC。
- 编写 MySQL Deployment。
- 编写 MySQL Service。
- 编写 Redis PVC。
- 编写 Redis Deployment。
- 编写 Redis Service。
- 验证 Pod 删除后数据是否保留。

阶段产出文件：

- `deploy/k8s/mysql/mysql-pvc.yaml`
- `deploy/k8s/mysql/mysql-deployment.yaml`
- `deploy/k8s/mysql/mysql-service.yaml`
- `deploy/k8s/redis/redis-pvc.yaml`
- `deploy/k8s/redis/redis-deployment.yaml`
- `deploy/k8s/redis/redis-service.yaml`
- `docs/04-kubernetes-yaml.md`

要学习的 Kubernetes 概念：

- PersistentVolume。
- PersistentVolumeClaim。
- StorageClass。
- Deployment 管理有状态服务的局限。
- Service 的 ClusterIP。
- Pod 重建和数据持久化。

说明：

- 生产环境中 MySQL 更适合使用云数据库或专门的 Operator。
- 本项目为了学习 PVC，会先在集群内部署 MySQL。

### 阶段 9：部署后端和前端服务

目标：

- 将 Spring Boot 后端部署到 K3s。
- 将 React 前端部署到 K3s。
- 通过 ConfigMap 和 Secret 注入配置。

主要任务：

- 编写后端 Deployment。
- 编写后端 Service。
- 编写前端 Deployment。
- 编写前端 Service。
- 编写 ConfigMap。
- 编写 Secret 示例文件。
- 配置数据库连接。
- 配置 Redis 连接。
- 配置健康检查。
- 验证后端 Pod 可以连接 MySQL 和 Redis。
- 配置 Nginx。
- 配置前端 API 地址。
- 验证前端可以访问后端。

阶段产出文件：

- `deploy/k8s/configmap.yaml`
- `deploy/k8s/secret.example.yaml`
- `deploy/k8s/backend/backend-deployment.yaml`
- `deploy/k8s/backend/backend-service.yaml`
- `deploy/k8s/frontend/frontend-deployment.yaml`
- `deploy/k8s/frontend/frontend-service.yaml`
- `frontend/nginx.conf`
- `docs/04-kubernetes-yaml.md` 更新

要学习的 Kubernetes 概念：

- Deployment。
- ReplicaSet。
- Pod template。
- ConfigMap。
- Secret。
- env。
- envFrom。
- readinessProbe。
- livenessProbe。
- `kubectl logs`。
- `kubectl describe pod`。
- `kubectl exec`。
- 前端静态资源容器化。
- Service 到 Pod 的流量转发。
- label selector。
- 集群内部 DNS。
- 前后端分离部署时的 API 路由策略。

### 阶段 10：Ingress、域名与 HTTPS

目标：

- 使用域名访问前端。
- 配置 HTTPS。
- 将 `/api` 请求转发到后端。

主要任务：

- 解析域名到云服务器公网 IP。
- 编写 Ingress。
- 配置前端路由。
- 配置后端 API 路由。
- 安装或配置 cert-manager。
- 申请 Let's Encrypt 证书。
- 验证 HTTPS 访问。

阶段产出文件：

- `deploy/k8s/ingress/ingress.yaml`
- `deploy/k8s/ingress/certificate.yaml`
- `docs/05-ingress-https.md`

要学习的 Kubernetes 概念：

- Ingress。
- Ingress Controller。
- TLS。
- Certificate。
- ClusterIssuer。
- HTTP 到 HTTPS 跳转。
- 域名、DNS、Service、Pod 之间的访问链路。

### 阶段 11：上线、日志排查、滚动更新与回滚

目标：

- 完成一次真实发布流程。
- 学会观察系统状态、排查问题、更新版本和回滚版本。

主要任务：

- 设置合理资源限制。
- 执行一次后端滚动更新。
- 执行一次前端滚动更新。
- 模拟错误镜像版本。
- 观察发布失败状态。
- 执行回滚。
- 查看日志和事件。
- 编写上线验收清单。

阶段产出文件：

- `deploy/scripts/apply-all.ps1`
- `deploy/scripts/apply-all.sh`
- `deploy/scripts/rollback-backend.sh`
- `docs/06-observability-debugging.md`
- `docs/07-rolling-update-rollback.md`

要学习的 Kubernetes 概念：

- resource requests。
- resource limits。
- rolling update。
- rollout status。
- rollout history。
- rollout undo。
- events。
- logs。
- describe。
- imagePullBackOff。
- CrashLoopBackOff。
- readiness 和滚动发布的关系。

## 6. 每个阶段要产出的文件汇总

| 阶段 | 主题 | 主要产出文件 |
| --- | --- | --- |
| 1 | 项目规划 | `docs/00-project-plan.md`, `README.md`, `.gitignore` |
| 2 | 服务器数据库准备 | `.env.example`, `docs/01-server-database.md` |
| 3 | 后端最小闭环 | `backend/`, `docs/01-local-development.md` |
| 4 | 前端最小闭环 | `frontend/`, `docs/01-local-development.md` |
| 5 | MVP 业务 | `frontend/src/`, `backend/src/` |
| 6 | Docker | `frontend/Dockerfile`, `frontend/nginx.conf`, `backend/Dockerfile`, `docs/02-docker.md` |
| 7 | K3s | `docs/03-k3s-install.md`, `deploy/k8s/namespace.yaml` |
| 8 | MySQL / Redis | `deploy/k8s/mysql/*.yaml`, `deploy/k8s/redis/*.yaml` |
| 9 | 应用部署 | `deploy/k8s/configmap.yaml`, `deploy/k8s/secret.example.yaml`, `deploy/k8s/backend/*.yaml`, `deploy/k8s/frontend/*.yaml` |
| 10 | Ingress / HTTPS | `deploy/k8s/ingress/*.yaml`, `docs/05-ingress-https.md` |
| 11 | 上线与回滚 | `deploy/scripts/*`, `docs/06-observability-debugging.md`, `docs/07-rolling-update-rollback.md` |

## 7. Kubernetes 学习概念路线

建议按下面顺序学习，不要一开始就追求复杂架构：

1. Pod：Kubernetes 中最小调度单位。
2. Deployment：如何声明和维护无状态应用副本。
3. Service：如何给 Pod 提供稳定访问入口。
4. Namespace：如何隔离项目资源。
5. ConfigMap：如何管理普通配置。
6. Secret：如何管理敏感配置。
7. PVC：如何给有状态服务提供持久化存储。
8. Ingress：如何从集群外部访问服务。
9. TLS / HTTPS：如何为域名启用安全访问。
10. Probe：如何让 Kubernetes 判断服务是否可接收流量。
11. Resource requests / limits：如何控制资源申请和上限。
12. Logs / Events / Describe：如何排查问题。
13. Rolling Update：如何平滑发布新版本。
14. Rollback：如何快速恢复到旧版本。

## 8. 最终上线验收标准

项目最终上线时，需要满足以下标准：

- 域名可以正常访问前端页面。
- HTTPS 证书有效，浏览器不提示不安全。
- 用户可以注册、登录、退出。
- 用户可以创建、查看、修改、删除任务。
- 后端数据可以持久化到 MySQL。
- Redis 可以被后端正常连接。
- 删除并重建后端 Pod 后，系统仍能正常工作。
- 删除并重建前端 Pod 后，系统仍能正常访问。
- 删除并重建 MySQL Pod 后，已有任务数据不丢失。
- 前端、后端、MySQL、Redis 都有明确的 Kubernetes YAML。
- 非敏感配置放在 ConfigMap。
- 敏感配置通过 Secret 注入。
- 后端 Deployment 配置了 readinessProbe 和 livenessProbe。
- 每个主要容器配置了资源 requests 和 limits。
- 可以通过 `kubectl logs` 查看后端日志。
- 可以通过 `kubectl describe pod` 定位常见启动失败原因。
- 可以执行一次后端滚动更新。
- 可以查看后端发布历史。
- 可以将后端回滚到上一个版本。
- 文档中记录了部署步骤、踩坑记录和排障过程。

## 9. 适合 2 核 2G 服务器的资源限制建议

2 核 2G 服务器资源非常紧张，目标是学习和小流量演示，不适合承载生产级访问。

建议总体策略：

- 所有服务副本数先使用 `1`。
- MySQL、Redis 运行在服务器上，只用于学习，不追求高可用。
- 不在服务器上同时运行太多构建任务。
- 镜像尽量在本地或 CI 中构建，再推送到镜像仓库。
- 避免安装过多集群插件。
- 给每个容器设置 requests 和 limits，防止单个服务吃满资源。

建议资源配置：

| 组件 | replicas | requests.cpu | requests.memory | limits.cpu | limits.memory |
| --- | ---: | ---: | ---: | ---: | ---: |
| frontend | 1 | 50m | 64Mi | 200m | 128Mi |
| backend | 1 | 200m | 384Mi | 800m | 768Mi |
| mysql | 1 | 300m | 512Mi | 1000m | 900Mi |
| redis | 1 | 50m | 64Mi | 200m | 128Mi |

后端 JVM 建议：

```text
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=40
```

注意：

- `MaxRAMPercentage` 在较新的 Java 8 update 版本中可用。
- 如果服务器或镜像中的 Java 8 版本不支持该参数，改用 `-Xms256m -Xmx512m` 这类固定堆大小配置。

MySQL 建议：

- PVC 初始大小：`2Gi` 到 `5Gi`。
- 降低 buffer 配置，避免默认配置占用过高内存。
- 学习阶段可以关闭不必要的性能配置。

Redis 建议：

- PVC 初始大小：`1Gi`。
- 设置 `maxmemory`，例如 `64mb` 或 `96mb`。
- 设置合理淘汰策略，例如 `allkeys-lru`。

K3s 服务器建议：

- 开启 swap 需要谨慎。Kubernetes 默认不建议依赖 swap。
- 保留至少 300Mi 到 500Mi 内存给系统和 K3s。
- 避免在同一台机器上运行数据库管理界面、监控大套件、CI Runner 等额外服务。

## 10. 后续每次协作时你应该如何帮助我

后续协作时，助手应该遵循以下方式：

- 一次只推进一个阶段，不提前生成大量暂时用不到的业务代码。
- 每次协作开始时先对照进度条，确认当前阶段和本次目标。
- 每次阶段完成后更新 `docs/00-project-plan.md` 中的当前进度。
- 每个阶段先说明目标、边界和验收标准。
- 优先让我理解为什么这样做，再给出命令和 YAML。
- 编写 YAML 时解释关键字段，而不是只给最终文件。
- 对 Secret、域名、证书、镜像仓库密码等敏感信息，只提供模板，不要求写入仓库。
- 每次修改文件前说明将修改哪些文件。
- 每次完成后给出验证命令。
- 如果命令失败，优先通过日志、事件和资源状态定位原因。
- 对 Windows 本地开发和 Linux 服务器命令分别标注。
- 遇到 2 核 2G 资源限制时，优先选择简单、可控、低资源占用的方案。
- 不为了炫技引入 Helm、Operator、Service Mesh、复杂监控系统，除非当前阶段确实需要。
- 每个阶段结束后补充文档，记录操作步骤、问题和解决方法。
- 业务代码保持简单，避免偏离 Kubernetes 学习主线。
- 对每个 Kubernetes 对象，都说明它解决的问题、依赖关系和常见排错方式。

推荐每次协作格式：

```text
当前阶段：
本次目标：
会修改的文件：
执行步骤：
验证方式：
可能的问题：
下一步：
```

本项目的学习原则：

- 先跑通，再优化。
- 先理解，再抽象。
- 先手写 YAML，再考虑工具。
- 先学会排错，再追求自动化。
- 每个阶段都要留下可复现的文件和文档。
