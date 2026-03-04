"""
MCP Code Execution - 简化版实现

核心思路（来自 Claude Code）：
1. 不要把所有工具定义塞进 Context（token 黑洞）
2. 按需加载工具定义
3. 让 AI 直接在沙箱里写代码执行

这个模块实现了一个轻量级的代码执行 Agent
"""

import asyncio
import subprocess
import tempfile
import os
import re
from typing import Optional, Dict, Any, List
from dataclasses import dataclass


@dataclass
class CodeExecutionResult:
    """代码执行结果"""
    success: bool
    stdout: str
    stderr: str
    return_code: int
    execution_time: float


class CodeExecutionSandbox:
    """
    代码执行沙箱 - 安全地执行 Python/JS 代码
    
    和传统 Agent 的区别：
    - 传统：AI 决定调用工具 → 工具定义进 Context → 执行 → 结果进 Context → 循环
    - 这个：AI 直接写代码 → 沙箱执行 → 返回结果 → 循环
    """
    
    def __init__(self, max_execution_time: int = 30, max_output_size: int = 100000):
        self.max_execution_time = max_execution_time
        self.max_output_size = max_output_size
        self.execution_history: List[CodeExecutionResult] = []
    
    async def execute_python(self, code: str) -> CodeExecutionResult:
        """执行 Python 代码"""
        import time
        start_time = time.time()
        
        # 创建临时文件
        with tempfile.NamedTemporaryFile(mode='w', suffix='.py', delete=False) as f:
            f.write(code)
            temp_file = f.name
        
        try:
            # 执行代码，捕获输出
            process = await asyncio.create_subprocess_exec(
                'python3', temp_file,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
                limit=self.max_output_size
            )
            
            try:
                stdout, stderr = await asyncio.wait_for(
                    process.communicate(),
                    timeout=self.max_execution_time
                )
            except asyncio.TimeoutError:
                process.kill()
                await process.wait()
                return CodeExecutionResult(
                    success=False,
                    stdout="",
                    stderr=f"Execution timeout ({self.max_execution_time}s)",
                    return_code=-1,
                    execution_time=time.time() - start_time
                )
            
            result = CodeExecutionResult(
                success=process.returncode == 0,
                stdout=stdout.decode('utf-8', errors='replace'),
                stderr=stderr.decode('utf-8', errors='replace'),
                return_code=process.returncode,
                execution_time=time.time() - start_time
            )
            
            self.execution_history.append(result)
            return result
            
        finally:
            # 清理临时文件
            if os.path.exists(temp_file):
                os.unlink(temp_file)
    
    async def execute_javascript(self, code: str) -> CodeExecutionResult:
        """执行 JavaScript 代码"""
        import time
        start_time = time.time()
        
        with tempfile.NamedTemporaryFile(mode='w', suffix='.js', delete=False) as f:
            f.write(code)
            temp_file = f.name
        
        try:
            process = await asyncio.create_subprocess_exec(
                'node', temp_file,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
                limit=self.max_output_size
            )
            
            try:
                stdout, stderr = await asyncio.wait_for(
                    process.communicate(),
                    timeout=self.max_execution_time
                )
            except asyncio.TimeoutError:
                process.kill()
                await process.wait()
                return CodeExecutionResult(
                    success=False,
                    stdout="",
                    stderr=f"Execution timeout ({self.max_execution_time}s)",
                    return_code=-1,
                    execution_time=time.time() - start_time
                )
            
            result = CodeExecutionResult(
                success=process.returncode == 0,
                stdout=stdout.decode('utf-8', errors='replace'),
                stderr=stderr.decode('utf-8', errors='replace'),
                return_code=process.returncode,
                execution_time=time.time() - start_time
            )
            
            self.execution_history.append(result)
            return result
            
        finally:
            if os.path.exists(temp_file):
                os.unlink(temp_file)


class MCPCodeExecutionAgent:
    """
    MCP 风格的代码执行 Agent
    
    对比传统 Agent Tool Calling:
    ┌─────────────────────────────────────────────────────────────┐
    │  传统方式 (Tool Calling)                                     │
    │  ─────────────────────                                      │
    │  1. AI 决定调用 "搜索工具"                                   │
    │  2. 系统把所有工具定义塞进 Context (可能几十K tokens)          │
    │  3. 工具执行，返回结果                                       │
    │  4. 结果塞回 Context                                        │
    │  5. 循环...                                                 │
    │                                                             │
    │  问题: token 消耗大、Context 塞满、延迟高                    │
    └─────────────────────────────────────────────────────────────┘
    
    ┌─────────────────────────────────────────────────────────────┐
    │  MCP Code Execution 方式                                     │
    │  ─────────────────────                                      │
    │  1. AI 直接写代码完成任务                                    │
    │  2. 沙箱执行代码                                             │
    │  3. 返回结果                                                │
    │  4. 循环...                                                 │
    │                                                             │
    │  优势: 轻量、快速、省 token                                  │
    └─────────────────────────────────────────────────────────────┘
    """
    
    def __init__(self):
        self.sandbox = CodeExecutionSandbox()
        self.conversation_history: List[Dict[str, Any]] = []
    
    def build_prompt(self, task: str) -> str:
        """构建系统提示词 - 只包含必要的指令，不包含大量工具定义"""
        
        # 关键：这里只提供简洁的指令，不需要像传统 Agent 那样列出所有工具
        system_prompt = """你是一个代码执行 Agent。

工作方式：
1. 直接编写代码来完成任务
2. 代码会在沙箱中执行
3. 根据结果决定下一步

可用语言：Python, JavaScript

约束：
- 每次只写适量代码，不要写整个程序
- 逐步执行，边试边改
- 如果出错，修复后重试

输出格式：
- 用 ```python 或 ```js 包裹代码
- 代码后面描述你希望执行什么"""
        
        return f"{system_prompt}\n\n用户任务：{task}"
    
    async def run(self, task: str, max_iterations: int = 10) -> str:
        """运行 Agent"""
        
        prompt = self.build_prompt(task)
        self.conversation_history.append({
            "role": "user", 
            "content": prompt
        })
        
        # 简化版：直接执行代码的循环
        # 实际使用时，这里可以接入 LLM 来决定代码
        print(f"开始任务: {task}")
        
        # 示例：让用户输入代码来执行
        # 实际生产环境中，这里是 LLM 生成代码
        return "Agent 已初始化。请提供代码执行。"
    
    def get_capabilities(self) -> Dict[str, Any]:
        """
        按需获取能力 - 这是 MCP 的核心思想！
        
        传统 Agent：启动时加载所有工具定义
        MCP 方式：只在需要时提供能力描述
        """
        return {
            "type": "code_execution",
            "languages": ["python", "javascript"],
            "max_execution_time": self.sandbox.max_execution_time,
            "sandboxed": True,
            # 关键：按需加载，不需要把所有能力描述都列出来
            "description": "在沙箱中执行代码"
        }


# 演示用法
async def demo():
    """演示代码执行"""
    
    # 创建沙箱
    sandbox = CodeExecutionSandbox()
    
    # 执行 Python 代码
    code = """
import random

# 模拟一个简单的数据处理
data = [random.randint(1, 100) for _ in range(10)]
print("原始数据:", data)
print("平均值:", sum(data) / len(data))
print("最大值:", max(data))
"""
    
    print("执行 Python 代码...")
    result = await sandbox.execute_python(code)
    
    print(f"成功: {result.success}")
    print(f"执行时间: {result.execution_time:.2f}s")
    print(f"输出:\n{result.stdout}")
    if result.stderr:
        print(f"错误:\n{result.stderr}")
    
    # 演示 Agent
    print("\n" + "="*50)
    print("初始化 MCP Code Execution Agent...")
    agent = MCPCodeExecutionAgent()
    capabilities = agent.get_capabilities()
    print(f"能力: {capabilities}")


if __name__ == "__main__":
    asyncio.run(demo())
