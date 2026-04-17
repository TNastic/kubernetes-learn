# 阶段 2：服务器数据库准备

本阶段目标是在腾讯云 Linux 服务器上准备 MySQL 8 和 Redis 7，让 Windows 本机只通过网络连接服务器数据库，不在本机安装数据库服务。

真实密码、服务器公网 IP、数据库账号不要提交到 Git。仓库只保留 `.env.example` 这种模板文件。

## 边界

- 本阶段只准备服务器 MySQL 和 Redis。
- 不安装 K3s，不编写 Kubernetes YAML。
- 不把真实 `.env`、数据库密码、Redis 密码写入仓库。
- MySQL 和 Redis 临时跑在服务器宿主机上，后续 K3s 阶段会再学习集群内部部署。

## 需要准备的信息

```text
腾讯云公网 IP：<你的服务器公网 IP>
Linux 发行版：Ubuntu 22.04/24.04 或 TencentOS/CentOS/Rocky
允许访问数据库的本机公网 IP：<你的 Windows 出口公网 IP>/32
MySQL 项目库名：task_manager
MySQL 项目用户：task_app
Redis 访问密码：自行生成强密码
```

查看服务器系统：

```bash
cat /etc/os-release
uname -m
```

生成强密码：

```bash
openssl rand -base64 24
```

## 腾讯云安全组

在腾讯云控制台进入：云服务器 CVM -> 实例 -> 目标服务器 -> 安全组。

入站规则建议只保留必要端口：

| 协议端口 | 来源 | 用途 |
| --- | --- | --- |
| TCP:22 | 你的 Windows 公网 IP/32 | SSH 登录 |
| TCP:3306 | 你的 Windows 公网 IP/32 | 本机连接 MySQL |
| TCP:6379 | 你的 Windows 公网 IP/32 | 本机连接 Redis |

不要把 `3306` 或 `6379` 开给 `0.0.0.0/0`。数据库端口暴露给全网很容易被扫描和爆破。

## Ubuntu 安装 MySQL 8

如果你的服务器是 Ubuntu，优先用 MySQL 官方 APT 仓库，并在配置界面选择 `mysql-8.0`。

```bash
sudo apt-get update
sudo apt-get install -y wget lsb-release gnupg
wget https://dev.mysql.com/get/mysql-apt-config_0.8.36-1_all.deb
sudo dpkg -i mysql-apt-config_0.8.36-1_all.deb
sudo apt-get update
sudo apt-get install -y mysql-server
sudo systemctl enable --now mysql
systemctl status mysql --no-pager
mysql --version
```

安装时如果提示设置 root 密码，请记录在你自己的密码管理器或本机私密笔记里，不要写进仓库。

## TencentOS/CentOS/Rocky 安装 MySQL 8

如果你的服务器是 TencentOS、CentOS 或 Rocky，使用 MySQL 官方 Yum/DNF 仓库。官方仓库现在默认可能启用 MySQL 8.4；本项目规划使用 MySQL 8，所以需要显式启用 8.0 仓库。

```bash
sudo dnf install -y dnf-plugins-core
sudo dnf install -y https://dev.mysql.com/get/mysql84-community-release-el8-1.noarch.rpm
sudo dnf config-manager --disable mysql-8.4-lts-community
sudo dnf config-manager --disable mysql-tools-8.4-lts-community
sudo dnf config-manager --enable mysql80-community
sudo dnf config-manager --enable mysql-tools-community
sudo dnf module disable -y mysql
sudo dnf install -y mysql-community-server
sudo systemctl enable --now mysqld
systemctl status mysqld --no-pager
mysql --version
```

首次安装后查看临时 root 密码：

```bash
sudo grep 'temporary password' /var/log/mysqld.log
```

然后执行安全初始化：

```bash
sudo mysql_secure_installation
```

## 创建项目数据库和用户

进入 MySQL：

```bash
mysql -uroot -p
```

执行 SQL。把 `<你的强密码>` 替换成真实密码。

```sql
CREATE DATABASE task_manager
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER 'task_app'@'%' IDENTIFIED BY '<你的强密码>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP
  ON task_manager.*
  TO 'task_app'@'%';

FLUSH PRIVILEGES;
```

验证：

```sql
SHOW DATABASES;
SELECT user, host FROM mysql.user WHERE user = 'task_app';
SHOW GRANTS FOR 'task_app'@'%';
EXIT;
```

## 允许 MySQL 监听网络连接

Ubuntu 配置文件通常是：

```bash
sudo grep -R "bind-address" /etc/mysql/mysql.conf.d /etc/mysql/conf.d
sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf
```

TencentOS/CentOS/Rocky 通常是：

```bash
sudo grep -R "bind-address" /etc/my.cnf /etc/my.cnf.d
sudo nano /etc/my.cnf
```

确认或添加：

```ini
bind-address = 0.0.0.0
```

重启并确认监听：

```bash
sudo systemctl restart mysql || sudo systemctl restart mysqld
sudo ss -lntp | grep 3306
```

## 安装 Redis 7

Ubuntu/Debian 使用 Redis 官方 APT 源：

```bash
sudo apt-get install -y lsb-release curl gpg
curl -fsSL https://packages.redis.io/gpg | sudo gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg
sudo chmod 644 /usr/share/keyrings/redis-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/redis.list
sudo apt-get update
sudo apt-get install -y redis
sudo systemctl enable --now redis-server
redis-server --version
```

TencentOS/CentOS/Rocky：

```bash
sudo dnf install -y redis
sudo systemctl enable --now redis
redis-server --version
```

如果 `dnf install redis` 安装不到 Redis 7，先用当前仓库版本完成阶段目标；后续 K3s 阶段会用容器镜像明确 Redis 7。

## 配置 Redis 密码和监听地址

Ubuntu 配置文件通常是 `/etc/redis/redis.conf`，RHEL 系通常是 `/etc/redis.conf`。

```bash
sudo nano /etc/redis/redis.conf
```

或：

```bash
sudo nano /etc/redis.conf
```

设置：

```conf
bind 0.0.0.0
protected-mode yes
requirepass <你的 Redis 强密码>
maxmemory 96mb
maxmemory-policy allkeys-lru
```

重启并确认监听：

```bash
sudo systemctl restart redis-server || sudo systemctl restart redis
sudo ss -lntp | grep 6379
redis-cli -a '<你的 Redis 强密码>' ping
```

期望输出：

```text
PONG
```

## 服务器本机验证

MySQL：

```bash
mysql -utask_app -p -h127.0.0.1 -P3306 task_manager -e "SELECT 1;"
```

Redis：

```bash
redis-cli -h 127.0.0.1 -p 6379 -a '<你的 Redis 强密码>' ping
```

## Windows 本机验证

先确认本机公网 IP，并把它填到腾讯云安全组来源里：

```powershell
(Invoke-WebRequest -UseBasicParsing https://ifconfig.me).Content
```

如果 Windows 已安装 MySQL 客户端：

```powershell
mysql -h <服务器公网IP> -P 3306 -u task_app -p task_manager -e "SELECT 1;"
```

如果 Windows 已安装 Redis CLI：

```powershell
redis-cli -h <服务器公网IP> -p 6379 -a "<你的 Redis 强密码>" ping
```

没有客户端也可以先测端口：

```powershell
Test-NetConnection <服务器公网IP> -Port 3306
Test-NetConnection <服务器公网IP> -Port 6379
```

`TcpTestSucceeded : True` 只说明端口能连通，不代表账号密码正确。最终仍要用 MySQL 和 Redis 客户端验证。

## 本地私密环境文件

复制模板，但不要提交真实文件：

```powershell
Copy-Item .env.example .env
```

把 `.env` 中的占位值替换成真实连接信息。`.gitignore` 已经忽略 `.env`。

## 常见问题

MySQL 连不上：

```bash
sudo systemctl status mysql --no-pager || sudo systemctl status mysqld --no-pager
sudo ss -lntp | grep 3306
sudo tail -n 80 /var/log/mysql/error.log || sudo tail -n 80 /var/log/mysqld.log
```

Redis 连不上：

```bash
sudo systemctl status redis-server --no-pager || sudo systemctl status redis --no-pager
sudo ss -lntp | grep 6379
sudo journalctl -u redis-server -n 80 --no-pager || sudo journalctl -u redis -n 80 --no-pager
```

端口通但认证失败：

- 检查 MySQL 用户是否是 `'task_app'@'%'`。
- 检查腾讯云安全组来源是否是当前 Windows 公网 IP。
- 检查 Redis `requirepass` 是否和客户端参数一致。

## 阶段验收

- 服务器上 `mysql --version` 能看到 MySQL 8。
- 服务器上 `redis-server --version` 能看到 Redis。
- MySQL 服务开机自启，并且 `task_manager` 数据库存在。
- `task_app` 用户能从 Windows 连接 MySQL。
- Redis 已设置密码，Windows 能收到 `PONG`。
- 仓库只有 `.env.example`，没有提交真实 `.env`。

## 参考资料

- MySQL APT 仓库官方文档：https://dev.mysql.com/doc/mysql-apt-repo-quick-guide/en/
- MySQL Yum 仓库官方文档：https://dev.mysql.com/doc/mysql/8.0/en/linux-installation-yum-repo.html
- Redis Linux 安装官方文档：https://redis.io/docs/latest/operate/oss_and_stack/install/archive/install-redis/install-redis-on-linux/
- 腾讯云安全组概述：https://cloud.tencent.com/document/product/213/112610
- 腾讯云添加安全组规则：https://cloud.tencent.com/document/product/213/39740
