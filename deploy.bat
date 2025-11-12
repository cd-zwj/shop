@echo off
chcp 65001 >nul
echo ==========================================
echo 支付系统部署脚本 (Windows)
echo ==========================================
echo.

REM 设置颜色
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "NC=[0m"

REM 检查 Node.js
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo %RED%错误: 未安装 Node.js%NC%
    pause
    exit /b 1
)

REM 检查 Maven
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo %RED%错误: 未安装 Maven%NC%
    pause
    exit /b 1
)

echo.
echo 1. 构建后端项目...
cd payment-system
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo %RED%后端构建失败%NC%
    cd ..
    pause
    exit /b 1
)
echo %GREEN%后端构建成功%NC%
cd ..

echo.
echo 2. 构建商家管理后台...
cd payment-frontend-admin
call npm install
call npm run build
if %errorlevel% neq 0 (
    echo %RED%商家管理后台构建失败%NC%
    cd ..
    pause
    exit /b 1
)
echo %GREEN%商家管理后台构建成功%NC%
cd ..

echo.
echo 3. 构建用户端应用...
cd payment-frontend-user
call npm install
call npm run build
if %errorlevel% neq 0 (
    echo %RED%用户端应用构建失败%NC%
    cd ..
    pause
    exit /b 1
)
echo %GREEN%用户端应用构建成功%NC%
cd ..

echo.
echo 4. 部署到 Nginx...

REM 设置 Nginx 目录（请根据实际情况修改）
set "NGINX_DIR=C:\nginx"
set "NGINX_HTML_DIR=%NGINX_DIR%\html"

if not exist "%NGINX_DIR%" (
    echo %YELLOW%警告: Nginx 目录不存在: %NGINX_DIR%%NC%
    echo %YELLOW%请修改脚本中的 NGINX_DIR 变量或手动部署%NC%
    goto :manual_deploy
)

REM 创建目录
if not exist "%NGINX_HTML_DIR%\admin" mkdir "%NGINX_HTML_DIR%\admin"
if not exist "%NGINX_HTML_DIR%\user" mkdir "%NGINX_HTML_DIR%\user"

REM 复制前端文件
echo 复制商家管理后台...
xcopy /E /I /Y payment-frontend-admin\dist\* "%NGINX_HTML_DIR%\admin\"

echo 复制用户端应用...
xcopy /E /I /Y payment-frontend-user\dist\* "%NGINX_HTML_DIR%\user\"

REM 复制 Nginx 配置
if exist "%NGINX_DIR%\conf\nginx.conf" (
    echo 备份原 Nginx 配置...
    copy /Y "%NGINX_DIR%\conf\nginx.conf" "%NGINX_DIR%\conf\nginx.conf.backup"
)

echo 复制新 Nginx 配置...
copy /Y nginx.conf "%NGINX_DIR%\conf\nginx.conf"

echo 重启 Nginx...
cd /d "%NGINX_DIR%"
nginx -t
if %errorlevel% equ 0 (
    nginx -s reload
    echo %GREEN%Nginx 配置已更新%NC%
) else (
    echo %RED%Nginx 配置测试失败，请检查配置文件%NC%
)
cd /d "%~dp0"

goto :deploy_complete

:manual_deploy
echo.
echo 请手动执行以下步骤：
echo 1. 将 payment-frontend-admin\dist 目录内容复制到 Nginx 的 html\admin 目录
echo 2. 将 payment-frontend-user\dist 目录内容复制到 Nginx 的 html\user 目录
echo 3. 将 nginx.conf 复制到 Nginx 的 conf 目录
echo 4. 重启 Nginx

:deploy_complete
echo.
echo ==========================================
echo %GREEN%部署完成！%NC%
echo ==========================================
echo.
echo 访问地址：
echo   商家管理后台: http://localhost (或 http://admin.payment.local)
echo   用户端应用:   http://user.payment.local
echo.
echo 后端服务：
echo   启动命令: cd payment-system ^&^& java -jar target\payment-system-1.0.0.jar
echo   访问地址: http://localhost:8080
echo.
echo 配置 hosts 文件 (C:\Windows\System32\drivers\etc\hosts):
echo   127.0.0.1  admin.payment.local
echo   127.0.0.1  user.payment.local
echo.
pause
