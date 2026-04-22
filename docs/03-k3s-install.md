# 阶段 7：K3s 安装与集群准备

## 当前阶段

当前阶段：阶段 7，K3s 安装与集群准备。

本阶段目标：

- 在云服务器上安装 K3s。
- 能在服务器上使用 `kubectl` 查看集群状态。
- 理解 `Node`、`Pod`、`Namespace`、`kubectl` 的基本作用。
- 创建项目自己的命名空间，为后续部署 MySQL、Redis、后端、前端做准备。
- 确认 K3s 自带的 Ingress Controller 是否正常运行。

本阶段产出文件：

- `docs/03-k3s-install.md`
- `deploy/k8s/namespace.yaml`

## 这一阶段到底在学什么

到第六阶段为止，你已经完成了：

- 应用代码开发。
- Docker 镜像构建。
- 用 Docker + Nginx 在服务器上直接跑起来。

第七阶段开始，重点从“容器能跑”切换到“集群如何管理容器”。

这时需要先把下面几个基础概念建立起来：

- `Node`：运行 Kubernetes 工作负载的机器。你现在的云服务器就是一个节点。
- `Pod`：Kubernetes 调度的最小单位。应用最终不是直接“部署镜像”，而是部署到 Pod 里。
- `Namespace`：资源隔离的逻辑空间。后面你的前端、后端、MySQL、Redis 都会放进同一个项目命名空间里。
- `kubectl`：和 Kubernetes 集群交互的命令行工具。
- `Ingress Controller`：把外部请求转发到集群内部 Service 的入口控制器。K3s 默认自带 `Traefik`。

先记住一句最重要的话：

Docker 解决“如何把应用打包成容器”，Kubernetes 解决“如何管理、调度、暴露这些容器”。

## 为什么这里选 K3s

你的服务器配置是 2 核 2G，这种环境不适合跑比较重的标准 Kubernetes 发行版。  
K3s 是轻量级 Kubernetes，特别适合学习、小流量演示、边学边实践。

它的优点：

- 安装简单。
- 资源占用更低。
- 内置很多默认组件。
- 学到的核心 Kubernetes 概念仍然通用。

需要知道的一点：

- K3s 不是“缩水版玩具”。
- 它仍然是符合 Kubernetes 规范的发行版。
- 你后面写的 `Deployment`、`Service`、`ConfigMap`、`Secret`、`PVC`、`Ingress` YAML 都是标准 Kubernetes 思路。

## 本阶段验收标准

完成本阶段后，你应该能够做到：

- 在服务器上看到 `k3s` 服务正在运行。
- 通过 `kubectl get nodes` 看到节点是 `Ready`。
- 通过 `kubectl get pods -A` 看到系统 Pod 正常启动。
- 通过 `kubectl get pods -n kube-system` 找到默认的 `Traefik`。
- 通过 `kubectl apply -f deploy/k8s/namespace.yaml` 创建项目命名空间。
- 通过 `kubectl get ns` 看到你的项目命名空间。

## 一、安装前准备

建议先确认服务器环境：

- Linux 发行版正常可联网。
- 已安装 `curl`。
- 有 `root` 权限或可用 `sudo`。
- 80 和 443 端口没有被其他服务长期占用。

如果你之前用 Docker + Nginx 直接跑了前端页面，并且映射了服务器的 80 端口，那么安装 K3s 之前最好先检查一下：

- 当前是否有容器占用了 `80` 端口。
- 当前是否有容器占用了 `443` 端口。

这是因为 K3s 默认会启动 `Traefik`，它通常会监听 80 和 443。

Linux 服务器检查命令：

```bash
sudo ss -lntp | grep -E ':80|:443'
```

如果发现是你之前的 Docker 容器占用了这些端口，需要先停掉旧容器，再让 K3s 接管入口流量。

## 二、在 Linux 服务器安装 K3s

Linux 服务器执行：

```bash
curl -sfL https://get.k3s.io | sh -
```

安装完成后，先检查服务状态：

```bash
sudo systemctl status k3s
```

如果只想看是否正在运行，也可以用：

```bash
sudo systemctl is-active k3s
```

预期结果应该是：

- 服务存在。
- 状态是 `active` 或 `running`。

## 三、理解 K3s 自带的 kubectl

K3s 安装后通常会自带 `kubectl`，也可以直接用：

```bash
sudo kubectl get nodes
```

或者：

```bash
sudo k3s kubectl get nodes
```

两种写法都常见。为了后面更顺手，建议优先确认 `kubectl` 是否已可直接使用。

如果能直接用：

```bash
kubectl get nodes
```

那后面就统一使用 `kubectl`。

如果普通用户没有权限读取 kubeconfig，可以先临时使用：

```bash
sudo kubectl get nodes
```

后面再处理本地 kubeconfig。

## 四、第一次看懂 `kubectl get nodes`

Linux 服务器执行：

```bash
kubectl get nodes -o wide
```

你通常会看到类似输出：

```text
NAME         STATUS   ROLES                  AGE   VERSION
your-server  Ready    control-plane,master   ...   v1.xx.x+k3s1
```

这行信息重点看三件事：

- `NAME`：节点名，通常就是服务器主机名。
- `STATUS=Ready`：说明节点健康，可以调度 Pod。
- `VERSION`：说明当前 K3s/Kubernetes 版本。

如果不是 `Ready`，先不要继续部署业务，先排查集群基础状态。

## 五、第一次看懂 `kubectl get pods -A`

Linux 服务器执行：

```bash
kubectl get pods -A
```

这里的 `-A` 表示查看所有命名空间。

你会看到一些系统组件，通常包括：

- `coredns`
- `local-path-provisioner`
- `metrics-server`
- `traefik`

这一步的学习重点不是背名字，而是理解：

- Kubernetes 不只是跑你的业务 Pod。
- 集群本身也依赖很多系统 Pod。
- 后续如果系统 Pod 不健康，你的业务部署也会受到影响。

## 六、确认默认 Ingress Controller

K3s 默认通常会安装 `Traefik` 作为 Ingress Controller。

Linux 服务器执行：

```bash
kubectl get pods -n kube-system
```

再重点看 `traefik` 是否处于 `Running`。

也可以执行：

```bash
kubectl get svc -n kube-system
```

如果你看到 `traefik` 相关 Service，说明默认入口控制器已经存在。

这里你要建立一个关键认知：

- 现在通过服务器 IP 访问页面，是 Docker 直接把请求接到容器。
- 后面进入 Ingress 阶段时，请求路径会变成：
  外部流量 -> Ingress Controller -> Service -> Pod

## 七、创建项目 Namespace

为什么不把资源直接丢进 `default` 命名空间？

原因很简单：

- 便于隔离项目资源。
- 便于按项目查看 Pod、Service、ConfigMap、Secret。
- 后续排障更清晰。

本项目建议命名空间使用：

```text
task-manager
```

YAML 文件已经放在：

- `deploy/k8s/namespace.yaml`

创建命名空间：

Linux 服务器执行：

```bash
kubectl apply -f deploy/k8s/namespace.yaml
```

查看命名空间：

```bash
kubectl get ns
```

只看这个命名空间下的资源：

```bash
kubectl get all -n task-manager
```

这时通常还没有业务资源，所以结果可能是空的，这是正常现象。

## 八、建议把默认命名空间上下文切到项目空间

为了减少后面每次都写 `-n task-manager`，可以把当前上下文默认命名空间切过去：

Linux 服务器执行：

```bash
kubectl config set-context --current --namespace=task-manager
```

验证：

```bash
kubectl config view --minify | grep namespace
```

切完之后，再执行：

```bash
kubectl get pods
```

默认看到的就是 `task-manager` 命名空间。

注意：

- 这只是修改当前 kubeconfig 上下文默认值。
- 不会影响资源本身。

## 九、如果你想在 Windows 本地使用 kubectl

这一部分不是本阶段绝对必须，但非常推荐。  
因为后面写 YAML、排查部署、看日志时，在本地终端操作会更舒服。

### 1. 从服务器导出 kubeconfig

Linux 服务器执行：

```bash
sudo cat /etc/rancher/k3s/k3s.yaml
```

这份文件里默认 `server` 地址通常是：

```text
https://127.0.0.1:6443
```

如果你要在 Windows 本地使用，需要把它改成服务器公网 IP，例如：

```text
https://<你的服务器公网IP>:6443
```

注意两点：

- 6443 端口需要安全组和防火墙允许你自己的来源 IP 访问。
- 不建议把 6443 对全网开放。

### 2. Windows 本地保存 kubeconfig

Windows PowerShell 执行：

```powershell
mkdir $HOME\.kube -Force
notepad $HOME\.kube\config
```

把修改过的 kubeconfig 内容粘贴进去保存。

### 3. Windows 本地验证

Windows PowerShell 执行：

```powershell
kubectl get nodes
kubectl get pods -A
kubectl get ns
```

如果能正常返回，说明你的本地 `kubectl` 已经连上服务器集群。

## 十、这一阶段常见问题

### 1. `kubectl get nodes` 没反应或报错

先检查：

```bash
sudo systemctl status k3s
sudo journalctl -u k3s -n 100 --no-pager
```

重点看：

- 服务是否启动失败。
- 是否有端口冲突。
- 是否有磁盘、内存、网络异常。

### 2. 节点不是 `Ready`

先看节点详情：

```bash
kubectl describe node
```

再看系统 Pod：

```bash
kubectl get pods -A
```

如果系统 Pod 大量异常，优先解决系统层问题，不要继续部署业务。

### 3. `Traefik` 起不来

检查：

```bash
kubectl get pods -n kube-system
kubectl describe pod -n kube-system <traefik-pod-name>
kubectl logs -n kube-system <traefik-pod-name>
```

最常见原因：

- 80 或 443 被之前的 Docker 容器占用。
- 服务器资源不足。

### 4. Windows 本地 `kubectl` 连接不上

重点排查：

- kubeconfig 中的 `server` 地址是否还是 `127.0.0.1`
- 6443 端口是否开放给你的本机 IP
- 服务器防火墙是否放行
- 本地网络是否能直连云服务器

## 十一、本阶段建议你亲手执行的最小命令清单

Linux 服务器：

```bash
sudo ss -lntp | grep -E ':80|:443'
curl -sfL https://get.k3s.io | sh -
sudo systemctl status k3s
kubectl get nodes -o wide
kubectl get pods -A
kubectl get pods -n kube-system
kubectl apply -f deploy/k8s/namespace.yaml
kubectl get ns
kubectl get all -n task-manager
```

Windows PowerShell：

```powershell
kubectl get nodes
kubectl get pods -A
kubectl get ns
```

## 十二、本阶段结束时你应该真正理解的点

如果你完成了上面的实操，你就不只是“把 K3s 装上了”，而是已经建立了下面这些认知：

- 服务器现在既是你的机器，也是 Kubernetes 的单节点集群。
- 以后部署应用的目标不再是“直接 docker run”，而是“创建 Kubernetes 资源让集群调度 Pod”。
- `Namespace` 是项目资源隔离的起点。
- `Traefik` 是后面做 Ingress、域名、HTTPS 的基础入口。
- `kubectl` 是后续所有部署、排障、更新、回滚的核心工具。

## 十三、完成标记

当你满足下面条件时，可以认为阶段 7 完成：

- K3s 已安装成功。
- `kubectl get nodes` 显示节点 `Ready`。
- 系统 Pod 基本正常。
- 默认 `Traefik` 正常。
- `task-manager` 命名空间已创建。
- 你已经能在服务器上，或者在 Windows 本地，通过 `kubectl` 操作这个集群。
