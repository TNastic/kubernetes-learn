# 阶段 8：部署 MySQL 和 Redis

## 当前阶段

当前阶段：阶段 8，部署 MySQL 和 Redis。

本阶段目标：

- 在 K3s 集群里部署 MySQL 和 Redis。
- 理解 `PVC`、`Deployment`、`Service` 在有状态组件中的分工。
- 让后续后端可以通过集群内服务名访问数据库和缓存。
- 验证 Pod 删除重建后，数据仍然保留。

本阶段产出文件：

- `deploy/k8s/mysql/mysql-pvc.yaml`
- `deploy/k8s/mysql/mysql-deployment.yaml`
- `deploy/k8s/mysql/mysql-service.yaml`
- `deploy/k8s/redis/redis-pvc.yaml`
- `deploy/k8s/redis/redis-deployment.yaml`
- `deploy/k8s/redis/redis-service.yaml`
- `docs/04-kubernetes-yaml.md`

## 一、这一阶段在学什么

第七阶段你学的是：

- 集群怎么装起来。
- `kubectl` 怎么看节点和系统 Pod。
- 默认 `Traefik` 是什么。

第八阶段开始，重点变成：

- 数据库和缓存如何在集群里落地。
- 为什么有状态组件不能只写一个 `Deployment` 就完事。
- 为什么需要持久化存储。

一句话先记住：

`Pod` 会重建，容器文件系统也会重置，所以 MySQL 和 Redis 的数据必须放到持久化卷里。

## 二、三个对象各自解决什么问题

### 1. PVC

`PersistentVolumeClaim` 解决的是：

- “我需要一块持久化存储”

在你的 K3s 环境里，默认 `StorageClass` 是：

```text
local-path
```

所以 PVC 会通过 `local-path-provisioner` 在这台单节点服务器上分配本地目录存储。

### 2. Deployment

`Deployment` 解决的是：

- “我要把这个 Pod 跑起来，并保持副本数”

虽然生产里 MySQL 常常更适合 `StatefulSet`，但这个学习项目在阶段 8 先使用 `Deployment`，重点先掌握：

- Pod 如何挂载 PVC
- Pod 如何通过环境变量拿到配置
- Pod 如何暴露端口

### 3. Service

`Service` 解决的是：

- “集群里的其他 Pod 怎样稳定访问它”

后面后端不会去找某个具体 MySQL Pod IP，而是直接连：

```text
mysql:3306
redis:6379
```

这里的 `mysql` 和 `redis` 就是 Service 名称，也是集群内 DNS 名。

## 三、为什么这里不用把密码写进 YAML

本仓库不提交真实密码，所以这一阶段的 YAML 直接引用一个已经存在的 Secret：

```text
task-manager-secret
```

也就是说：

- 仓库里放“资源结构”
- 真实密码放集群里

这不是偷懒，而是把“配置结构”和“敏感值”分开。

本阶段先手动创建 Secret，下一阶段再补 `secret.example.yaml` 模板文件。

## 四、先创建 Secret

Linux 服务器执行：

```bash
sudo kubectl create secret generic task-manager-secret \
  -n task-manager \
  --from-literal=MYSQL_ROOT_PASSWORD='<你的MySQL root密码>' \
  --from-literal=MYSQL_DATABASE='task_manager' \
  --from-literal=MYSQL_USER='task_app' \
  --from-literal=MYSQL_PASSWORD='<你的MySQL应用密码>' \
  --from-literal=REDIS_PASSWORD='<你的Redis密码>'
```

查看 Secret 是否创建成功：

```bash
sudo kubectl get secret -n task-manager
```

注意：

- 这里命令中的密码不会写进仓库。
- 如果后面想修改密码，先删掉原 Secret 再重新创建。

## 五、应用 MySQL 和 Redis 资源

建议按下面顺序应用：

Linux 服务器执行：

```bash
sudo kubectl apply -f deploy/k8s/mysql/mysql-pvc.yaml
sudo kubectl apply -f deploy/k8s/mysql/mysql-deployment.yaml
sudo kubectl apply -f deploy/k8s/mysql/mysql-service.yaml

sudo kubectl apply -f deploy/k8s/redis/redis-pvc.yaml
sudo kubectl apply -f deploy/k8s/redis/redis-deployment.yaml
sudo kubectl apply -f deploy/k8s/redis/redis-service.yaml
```

## 六、这 6 个 YAML 各自怎么看

### 1. `mysql-pvc.yaml`

重点字段：

- `storageClassName: local-path`
- `accessModes: ReadWriteOnce`
- `resources.requests.storage: 2Gi`

这里表示：

- 使用 K3s 默认本地存储类
- 这块卷只给一个节点上的一个挂载者使用
- 先申请 2Gi

### 2. `mysql-deployment.yaml`

重点字段：

- `image: mysql:8.0`
- `env` 通过 `secretKeyRef` 读取 MySQL 密码和库名
- `volumeMounts` 把 PVC 挂载到 `/var/lib/mysql`

最关键的理解点是：

- MySQL 真正的数据目录在 `/var/lib/mysql`
- 如果不挂载 PVC，Pod 一删数据就没了

### 3. `mysql-service.yaml`

重点字段：

- `selector.app: mysql`
- `port: 3306`
- `targetPort: 3306`

它的作用是把名字 `mysql` 映射到 MySQL Pod。

### 4. `redis-pvc.yaml`

Redis 也需要持久化，所以给它单独申请：

- `1Gi` PVC

### 5. `redis-deployment.yaml`

重点字段：

- `image: redis:7.2`
- 启动命令开启 `appendonly yes`
- 通过 `REDIS_PASSWORD` 启动鉴权
- 把数据目录挂载到 `/data`

这里的 `appendonly yes` 很重要，因为它让 Redis 把写入追加到持久化文件中。

### 6. `redis-service.yaml`

和 MySQL 一样，它提供一个稳定服务名：

```text
redis
```

后续后端会连：

```text
redis:6379
```

## 七、验证命令

Linux 服务器执行：

```bash
sudo kubectl get pvc -n task-manager
sudo kubectl get pods -n task-manager -o wide
sudo kubectl get svc -n task-manager
```

你应该重点看：

- `mysql-pvc`、`redis-pvc` 是否是 `Bound`
- `mysql`、`redis` Pod 是否是 `Running`
- `mysql` 和 `redis` Service 是否都存在

如果 Pod 没起来，再继续看：

```bash
sudo kubectl describe pod -n task-manager <pod-name>
sudo kubectl logs -n task-manager <pod-name>
```

## 八、验证集群内访问

等 Pod 正常后，可以用端口转发做最小验证。

### 1. 验证 MySQL 端口

Linux 服务器执行：

```bash
sudo kubectl port-forward -n task-manager svc/mysql 3306:3306
```

另开一个终端执行：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p
```

### 2. 验证 Redis 端口

Linux 服务器执行：

```bash
sudo kubectl port-forward -n task-manager svc/redis 6379:6379
```

另开一个终端执行：

```bash
redis-cli -h 127.0.0.1 -p 6379 -a '<你的Redis密码>' ping
```

如果返回：

```text
PONG
```

说明 Redis 服务连通。

## 九、验证 PVC 是否真的保住了数据

这一步很关键，因为这是阶段 8 的核心学习点。

建议你按这个顺序验证：

1. 连上 MySQL 创建一张测试表，插一条记录。
2. 连上 Redis 写一个测试 key。
3. 删除 MySQL Pod 和 Redis Pod。
4. 等 Pod 被 Deployment 自动重建。
5. 再次连接，确认数据还在。

删除 Pod：

```bash
sudo kubectl delete pod -n task-manager -l app=mysql
sudo kubectl delete pod -n task-manager -l app=redis
```

再次查看：

```bash
sudo kubectl get pods -n task-manager -w
```

这个实验的目的不是“删 Pod”，而是亲眼看到：

- Pod 可以换
- PVC 挂载的数据不会跟着一起丢

## 十、当前方案的边界

这套方案适合当前学习阶段，但你要知道它的边界：

- `local-path` 本质上还是单机本地盘
- 这不是高可用存储
- MySQL 用 `Deployment` 是为了先学习挂载和服务访问，不是生产最佳实践
- 生产环境里数据库通常会用云数据库、托管 Redis、或更专业的集群方案

但对于你现在的学习目标，它非常合适，因为你正在学习的是：

- 资源对象关系
- 持久化存储
- 集群内访问
- 出问题时如何通过 `kubectl` 排查

## 十一、本阶段完成标记

当你满足下面条件时，可以认为阶段 8 完成：

- `mysql-pvc` 和 `redis-pvc` 都是 `Bound`
- MySQL Pod 和 Redis Pod 都是 `Running`
- MySQL Service 和 Redis Service 都创建成功
- 能通过端口转发连接 MySQL 和 Redis
- 删除并重建 Pod 后，测试数据仍然存在
