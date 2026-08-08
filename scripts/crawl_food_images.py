"""
从 data.sql 提取食物名 → Google 图片搜索 → 下载图片
用法：先 pip install icrawler，再 python scripts/crawl_food_images.py
"""
import re
import os
import time
from icrawler.builtin import BingImageCrawler

# ===== 配置 =====
SQL_PATH = "../calorie-server/src/main/resources/data.sql"
SAVE_DIR = "../food_images"
MAX_IMG = 5
DELAY = 1.5

# ===== 辅助函数 =====
def clean_search_name(food):
    """去掉中英文括号及括号内容"""
    name = re.sub(r'（[^）]*）', '', food)
    name = re.sub(r'\([^)]*\)', '', name)
    return name.strip()

# ===== 1. 提取食物名 =====
script_dir = os.path.dirname(os.path.abspath(__file__))
sql_path = os.path.join(script_dir, SQL_PATH)
save_dir = os.path.join(script_dir, SAVE_DIR)

with open(sql_path, "r", encoding="utf-8") as f:
    content = f.read()

# 只提取 is_common=true 的食物
foods = re.findall(
    r"\('([^']+)',\s*\d+,\s*[\d.]+,\s*[\d.]+,\s*[\d.]+,\s*[\d.]+,\s*true,\s*true",
    content
)
foods = list(dict.fromkeys(foods))

print(f"共提取 {len(foods)} 种食物")
print(f"保存路径: {save_dir}")
print(f"每种最多 {MAX_IMG} 张，间隔 {DELAY}s\n")

# ===== 2. 逐个爬取 =====
os.makedirs(save_dir, exist_ok=True)

for i, food in enumerate(foods):
    food_dir = os.path.join(save_dir, food)
    os.makedirs(food_dir, exist_ok=True)

    # 已有图片就跳过
    files = os.listdir(food_dir) if os.path.exists(food_dir) else []
    existing = len([f for f in files if f.endswith(('.jpg', '.png', '.jpeg'))])
    if existing >= MAX_IMG:
        print(f"[{i+1}/{len(foods)}] {food} — 已有 {existing} 张，跳过")
        continue

    # 清洗括号，只用食物名本身
    keyword = clean_search_name(food)

    try:
        crawler = BingImageCrawler(
            storage={"root_dir": food_dir},
            downloader_threads=2,
        )
        crawler.crawl(
            keyword=keyword,
            max_num=MAX_IMG,
            file_idx_offset="auto",
        )
        print(f"[{i+1}/{len(foods)}] {food} →「{keyword}」完成")
    except Exception as e:
        print(f"[{i+1}/{len(foods)}] {food} — 失败: {e}")

    time.sleep(DELAY)

print(f"\n全部完成！图片保存在 {save_dir}/")
