# sen-agent 部署指南

## 方式一: 一键部署 (推荐)

SSH登录服务器后执行:

```bash
# 1. 下载部署脚本
cd /opt
curl -O https://raw.githubusercontent.com/Senrian/sen-agent/main/deploy.sh
chmod +x deploy.sh

# 2. 运行部署
./deploy.sh

# 3. 输入 DeepSeek API Key
# 部署脚本会自动提示输入
```

## 方式二: 手动部署

```bash
# 1. SSH登录
ssh root@124.223.177.67

# 2. 安装依赖
apt-get update && apt-get install -y openjdk-17-jdk maven git

# 3. 克隆项目
cd /opt
git clone https://github.com/Senrian/sen-agent.git
cd sen-agent

# 4. 设置API Key
export DEEPSEEK_API_KEY=sk-ec738a517c1f4d7dbf6efc3cab081f7a

# 5. 编译
mvn clean package -DskipTests

# 6. 启动
java -jar target/sen-agent-*.jar --server.port=8080 &

# 7. 访问
# http://124.223.177.67:8080
# http://124.223.177.67:8080/index.html
```

## API端点

- `GET /api/health` - 健康检查
- `POST /api/chat` - 对话
- `POST /api/agent/create` - 创建Agent
- `GET /api/skills` - 技能列表

## 常见问题

### 端口被占用
```bash
lsof -i:8080
kill -9 <PID>
```

### 重启
```bash
cd /opt/sen-agent
git pull
mvn clean package -DskipTests
pkill -f sen-agent
java -jar target/sen-agent-*.jar &
```
