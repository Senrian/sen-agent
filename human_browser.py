"""
模拟人类浏览网页行为 - 反爬虫对抗技术
Human-like Browser Automation

功能：
- 随机滚动页面，模拟阅读
- 随机鼠标移动轨迹
- 随机停留时间
- 模拟人类操作行为
"""

import asyncio
import random
import time
from typing import Optional

try:
    from playwright.async_api import async_playwright, Page, Browser
except ImportError:
    print("请安装 playwright: pip install playwright && playwright install chromium")


class HumanBrowser:
    """模拟人类浏览行为的浏览器自动化类"""
    
    def __init__(self, headless: bool = False):
        self.headless = headless
        self.playwright = None
        self.browser: Optional[Browser] = None
        self.page: Optional[Page] = None
        
    async def start(self):
        """启动浏览器"""
        self.playwright = await async_playwright().start()
        self.browser = await self.playwright.chromium.launch(
            headless=self.headless,
            args=['--disable-blink-features=AutomationControlled']
        )
        context = await self.browser.new_context(
            viewport={'width': 1920, 'height': 1080},
            user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        )
        self.page = await context.new_page()
        
        # 注入脚本隐藏自动化特征
        await self.page.add_init_script("""
            Object.defineProperty(navigator, 'webdriver', {
                get: () => undefined
            });
            window.navigator.chrome = {
                runtime: {}
            };
            Object.defineProperty(navigator, 'plugins', {
                get: () => [1, 2, 3, 4, 5]
            });
            Object.defineProperty(navigator, 'languages', {
                get: () => ['zh-CN', 'zh', 'en']
            });
        """)
        
    async def human_scroll(self, min_pause: float = 0.5, max_pause: float = 2.0):
        """模拟人类滚动页面 - 随机停顿分段滚动"""
        if not self.page:
            raise RuntimeError("浏览器未启动")
            
        # 获取页面高度
        scroll_height = await self.page.evaluate("document.body.scrollHeight")
        viewport_height = await self.page.evaluate("window.innerHeight")
        
        current_scroll = 0
        while current_scroll < scroll_height:
            # 随机滚动距离 (模拟阅读一段内容)
            scroll_amount = random.randint(100, 400)
            current_scroll += scroll_amount
            
            # 随机滚动（带缓动效果）
            await self.page.evaluate(f"""
                window.scrollTo({{
                    top: {current_scroll},
                    behavior: '{random.choice(['smooth', 'auto'])}'
                }})
            """)
            
            # 随机停顿时间（模拟阅读时间）
            pause_time = random.uniform(min_pause, max_pause)
            await asyncio.sleep(pause_time)
            
            # 偶尔回滚一点（模拟重新阅读）
            if random.random() < 0.1:
                back_scroll = random.randint(20, 100)
                await self.page.evaluate(f"window.scrollBy(0, -{back_scroll})")
                await asyncio.sleep(random.uniform(0.3, 0.8))
                
    async def human_mouse_move(self, duration: float = 1.0):
        """模拟人类鼠标移动轨迹"""
        if not self.page:
            raise RuntimeError("浏览器未启动")
            
        # 获取视口大小
        viewport = self.page.viewport_size
        if not viewport:
            return
            
        width, height = viewport['width'], viewport['height']
        
        # 随机起点和终点
        start_x = random.randint(0, width // 2)
        start_y = random.randint(0, height // 2)
        end_x = random.randint(width // 2, width)
        end_y = random.randint(height // 2, height)
        
        # 生成中间点（模拟曲线运动）
        points = []
        num_points = random.randint(5, 15)
        for i in range(num_points + 1):
            progress = i / num_points
            # 添加随机偏移
            x = start_x + (end_x - start_x) * progress + random.randint(-30, 30)
            y = start_y + (end_y - start_y) * progress + random.randint(-30, 30)
            x = max(0, min(width, x))
            y = max(0, min(height, y))
            points.append((x, y))
            
        # 逐步移动鼠标
        await self.page.mouse.move(points[0][0], points[0][1])
        for i in range(1, len(points)):
            await self.page.mouse.move(points[i][0], points[i][1])
            # 随机延迟
            await asyncio.sleep(duration / len(points))
            
    async def human_click(self, selector: str = None, x: int = None, y: int = None):
        """模拟人类点击（带随机延迟和移动）"""
        if not self.page:
            raise RuntimeError("浏览器未启动")
            
        # 随机移动鼠标到目标位置
        if selector:
            # 先移动到元素附近
            await self.page.hover(selector)
            await asyncio.sleep(random.uniform(0.1, 0.3))
            
        if x and y:
            await self.page.mouse.move(x, y)
            await asyncio.sleep(random.uniform(0.1, 0.3))
            
        # 模拟点击（带微小移动）
        await self.page.mouse.down()
        await asyncio.sleep(random.uniform(0.05, 0.15))
        await self.page.mouse.up()
        
    async def random_delay(self, min_sec: float = 1.0, max_sec: float = 5.0):
        """随机延迟"""
        delay = random.uniform(min_sec, max_sec)
        await asyncio.sleep(delay)
        
    async def visit_and_read(self, url: str, read_time: float = 10.0):
        """访问页面并模拟阅读"""
        print(f"访问: {url}")
        await self.page.goto(url, wait_until="domcontentloaded")
        
        # 等待页面加载完成
        await asyncio.sleep(random.uniform(1.0, 2.0))
        
        # 模拟人类阅读滚动
        print("模拟人类滚动阅读...")
        await self.human_scroll(min_pause=0.8, max_pause=2.5)
        
        # 随机停留一段时间
        print(f"停留阅读中...")
        await asyncio.sleep(read_time)
        
    async def close(self):
        """关闭浏览器"""
        if self.browser:
            await self.browser.close()
        if self.playwright:
            await self.playwright.stop()


async def demo():
    """演示用法"""
    browser = HumanBrowser(headless=False)
    await browser.start()
    
    try:
        # 访问示例页面
        await browser.visit_and_read("https://www.example.com", read_time=5.0)
        
        # 模拟更多交互
        await browser.random_delay(2, 4)
        await browser.human_mouse_move(duration=1.5)
        
    finally:
        await browser.close()


if __name__ == "__main__":
    asyncio.run(demo())
