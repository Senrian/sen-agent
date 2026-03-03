#!/bin/bash
# sen-agent 一键部署脚本

set -e

echo "========== sen-agent 部署脚本 =========="

# 1. 安装必要软件
echo "[1/6] 检查环境..."
if ! command -v java &> /dev/null; then
    echo "安装 Java 17..."
    apt-get update && apt-get install -y openjdk-17-jdk
fi

if ! command -v maven &> /dev/null; then
    echo "安装 Maven..."
    apt-get install -y maven
fi

# 2. 克隆项目
echo "[2/6] 克隆项目..."
cd /opt
if [ -d "sen-agent" ]; then
    cd sen-agent
    git pull
else
    git clone https://github.com/Senrian/sen-agent.git
    cd sen-agent
fi

# 3. 设置环境变量
echo "[3/6] 配置环境变量..."
export DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY:-}"
if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "请设置 DEEPSEEK_API_KEY 环境变量"
    echo "export DEEPSEEK_API_KEY=sk-your-api-key"
    read -p "请输入 DeepSeek API Key: " API_KEY
    export DEEPSEEK_API_KEY=$API_KEY
fi

# 4. 编译
echo "[4/6] 编译项目..."
mvn clean package -DskipTests

# 5. 启动
echo "[5/6] 启动服务..."
nohup java -jar target/sen-agent-*.jar --server.port=8080 > /var/log/sen-agent.log 2>&1 &
echo $! > /var/run/sen-agent.pid

# 6. 等待启动
echo "[6/6] 等待服务启动..."
sleep 10

# 检查
if curl -s http://localhost:8080/api/health > /dev/null; then
    echo "✅ 部署成功!"
    echo "访问地址: http://$(hostname -I | awk '{print $1}'):8080"
    echo "Demo: http://$(hostname -I | awk '{print $1}'):8080/index.html"
else
    echo "❌ 启动失败，查看日志:"
    tail -50 /var/log/sen-agent.log
fi

echo "========== 部署完成 =========="
