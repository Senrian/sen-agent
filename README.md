# senAgent

自研Java Agent框架 - 对标LangGraph/LangChain/OpenClaw

## 特性

### 🤖 多Agent支持
- **MiniAgent** - 基础对话Agent
- **FnCallAgent** - 函数调用Agent  
- **ReActAgent** - ReAct推理Agent
- **CodeAgent** - 代码助手
- **BrowserAgent** - 浏览器助手
- **RunnableAgent** - LCEL风格Agent

### 🔗 LangGraph核心
- **StateGraph** - 状态图
- **Node/Edge** - 节点和边
- **条件路由** - Conditional Edge

### ⛓️ LangChain LCEL
- **Chain** - 链式调用
- **Pipe** - 管道组合
- **Parallel** - 并行执行
- **Batch** - 批量处理

### 🧠 记忆系统
- **ChatMemory** - 对话记忆
- **SummaryMemory** - 摘要记忆
- **短时/长时记忆**

### 📚 RAG/向量
- **VectorStore** - 向量存储
- **RAGEngine** - RAG引擎
- **DocumentLoader** - 文档加载

### 🔧 MCP协议
- **MCPServer** - MCP服务器
- **Tool/Resource/Prompt** - MCP组件

### 🛠️ 工具集
- Python沙箱
- 网页搜索
- 天气查询
- 新闻获取
- 文件操作

### ⚡ 工作流
- **WorkflowEngine** - 工作流引擎
- 顺序/并行/条件分支

## 快速开始

```bash
# 克隆
git clone https://github.com/Senrian/sen-agent.git
cd sen-agent

# 编译
mvn clean package -DskipTests

# 运行
export DEEPSEEK_API_KEY=your-api-key
java -jar target/sen-agent-0.0.1-SNAPSHOT.jar
```

## API

- `POST /api/chat` - 聊天
- `POST /api/agent` - 创建Agent
- `POST /api/graph` - 状态图
- `POST /api/stream/chat` - 流式聊天

## Demo

访问 http://localhost:8080

## 模块

```
sen-agent/
├── agent/        # Agent实现
├── graph/        # LangGraph
├── chain/        # LCEL
├── rag/          # RAG
├── memory/       # 记忆
├── mcp/          # MCP
├── workflow/     # 工作流
├── skill/        # Skill
├── tool/         # 工具
└── controller/   # API
```

## License

MIT
