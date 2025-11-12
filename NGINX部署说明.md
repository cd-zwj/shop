# Nginx 部署说明

## 架构说明

```
客户端浏览器
    ↓
Nginx (80端口)
    ↓
    ├─→ 静态资源 (前端页面)
    └─→ /api/* → 后端服务 (8080端口)
```

## 部署步骤

### 1. 安装 Nginx

#### Windows
下载 Nginx: http://nginx.org/en/download.html
解压到目录，例如: `C:\nginx`

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install nginx
```

#### Linux (CentOS/RHEL)
```bash
sudo yum install nginx
```

#### macOS
```bash
brew install nginx
```

### 2. 配置 Nginx

将项目根目录的 `nginx.conf` 复制到 Nginx 配置目录：

#### Windows
```bash
copy nginx.conf C:\nginx\conf\nginx.conf
```

#### Linux
```bash
sudo cp nginx.conf /etc/nginx/nginx.conf
```

### 3. 构建前端项目

#### 商家管理后台
```bash
cd payment-frontend-admin
npm install
npm run build
```
构建产物在 `payment-frontend-admin/dist` 目录

#### 用户端应用
```bash
cd payment-frontend-user
npm install
npm run build
```
构建产物在 `payment-frontend-user/dist` 目录

### 4. 部署前端文件

将构建产物复制到 Nginx 静态文件目录：

#### Windows
```bash
# 创建目录
mkdir C:\nginx\html\admin
mkdir C:\nginx\html\user

# 复制文件
xcopy /E /I payment-frontend-admin\dist\* C:\nginx\html\admin\
xcopy /E /I payment-frontend-user\dist\* C:\nginx\html\user\
```

#### Linux
```bash
# 创建目录
sudo mkdir -p /usr/share/nginx/html/admin
sudo mkdir -p /usr/share/nginx/html/user

# 复制文件
sudo cp -r payment-frontend-admin/dist/* /usr/share/nginx/html/admin/
sudo cp -r payment-frontend-user/dist/* /usr/share/nginx/html/user/
```

### 5. 启动后端服务

```bash
cd payment-system
mvn clean package -DskipTests
java -jar target/payment-system-1.0.0.jar
```

后端服务运行在 `http://localhost:8080`

### 6. 启动 Nginx

#### Windows
```bash
cd C:\nginx
start nginx
```

停止 Nginx:
```bash
nginx -s stop
```

重新加载配置:
```bash
nginx -s reload
```

#### Linux
```bash
# 启动
sudo systemctl start nginx

# 停止
sudo systemctl stop nginx

# 重启
sudo systemctl restart nginx

# 重新加载配置
sudo nginx -s reload

# 设置开机自启
sudo systemctl enable nginx
```

### 7. 配置 hosts（可选）

如果要使用域名访问，需要配置 hosts 文件：

#### Windows
编辑 `C:\Windows\System32\drivers\etc\hosts`

#### Linux/macOS
编辑 `/etc/hosts`

添加以下内容：
```
127.0.0.1  admin.payment.local
127.0.0.1  user.payment.local
127.0.0.1  platform.payment.local
```

### 8. 访问应用

- 商家管理后台: http://localhost 或 http://admin.payment.local
- 用户端应用: http://user.payment.local
- 后端 API: http://localhost/api (通过 Nginx 代理)

## 快速部署（Linux）

使用提供的部署脚本：

```bash
chmod +x deploy.sh
./deploy.sh
```

## 配置说明

### API 代理配置

Nginx 配置中的关键部分：

```nginx
location /api/ {
    proxy_pass http://payment_backend/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

这个配置将所有 `/api/*` 的请求代理到后端服务 `http://localhost:8080/api/*`

### 前端环境变量

#### 开发环境 (`.env.development`)
```
VITE_API_BASE_URL=http://localhost:8080/api
```
直接连接后端，方便调试

#### 生产环境 (`.env.production`)
```
VITE_API_BASE_URL=/api
```
使用相对路径，通过 Nginx 代理

## 负载均衡（可选）

如果有多个后端实例，可以配置负载均衡：

```nginx
upstream payment_backend {
    server 127.0.0.1:8080 weight=1;
    server 127.0.0.1:8081 weight=1;
    server 127.0.0.1:8082 weight=1;
}
```

## HTTPS 配置（生产环境推荐）

```nginx
server {
    listen       443 ssl;
    server_name  admin.payment.com;

    ssl_certificate      /path/to/cert.pem;
    ssl_certificate_key  /path/to/cert.key;

    ssl_session_cache    shared:SSL:1m;
    ssl_session_timeout  5m;

    ssl_ciphers  HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers  on;

    location / {
        root   html/admin;
        index  index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://payment_backend/api/;
        # ... 其他代理配置
    }
}

# HTTP 重定向到 HTTPS
server {
    listen       80;
    server_name  admin.payment.com;
    return 301 https://$server_name$request_uri;
}
```

## 故障排查

### 1. Nginx 启动失败
```bash
# 检查配置文件语法
nginx -t

# 查看错误日志
tail -f /var/log/nginx/error.log
```

### 2. API 请求 404
- 检查后端服务是否启动
- 检查 Nginx 代理配置是否正确
- 查看 Nginx 访问日志: `tail -f /var/log/nginx/access.log`

### 3. 前端页面空白
- 检查前端文件是否正确部署
- 打开浏览器开发者工具查看控制台错误
- 检查 Nginx 静态文件路径配置

### 4. 跨域问题
确保 Nginx 配置中包含正确的代理头：
```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

## 性能优化

### 1. 启用 Gzip 压缩
已在配置文件中启用，可以减少传输数据量

### 2. 静态资源缓存
```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 7d;
    add_header Cache-Control "public, immutable";
}
```

### 3. 连接池优化
```nginx
upstream payment_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}
```

## 监控和日志

### 访问日志
```bash
tail -f /var/log/nginx/access.log
```

### 错误日志
```bash
tail -f /var/log/nginx/error.log
```

### Nginx 状态监控
```nginx
location /nginx_status {
    stub_status on;
    access_log off;
    allow 127.0.0.1;
    deny all;
}
```

访问: http://localhost/nginx_status
