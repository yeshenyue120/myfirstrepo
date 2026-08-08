"""
将 data.sql 中的食物分类重映射为新的 6 分类体系。
新分类：1主食 2蔬果 3肉蛋奶 4豆类坚果 5零食饮料 6中西菜肴
"""
import re

INPUT = r"D:\Project\calorie-server\src\main\resources\data.sql"
OUTPUT = INPUT  # 直接覆盖，如需备份改路径

# ---- 需要特殊处理的物品名单（按名称精确匹配） ----

# 旧 cat2 → 新 cat4（豆类坚果）：豆制品
TO_CAT4_FROM_CAT2 = {
    '嫩豆腐', '老豆腐', '豆腐干', '千张(百叶)', '腐竹(干)',
    '豆浆(无糖)', '豆腐皮', '素鸡', '毛豆(煮)', '黄豆(干)',
    '纳豆', '炸豆腐', '奶豆腐',
}

# 旧 cat6 → 新 cat4（豆类坚果）：坚果种子
TO_CAT4_FROM_CAT6 = {
    '核桃', '杏仁', '腰果', '开心果', '花生(炒)', '花生酱',
    '瓜子(葵花籽)', '南瓜子', '松子', '榛子', '夏威夷果',
    '板栗(炒)', '琥珀核桃', '盐焗腰果', '蚕豆(炸)', '青豌豆(膨化)',
}

# 旧 cat1 → 新 cat6（中西菜肴）：熟食、加工菜肴
TO_CAT6_FROM_CAT1 = {
    '蛋炒饭', '皮蛋瘦肉粥',
    '肉包子', '菜包子', '小笼包', '饺子(猪肉白菜)', '馄饨',
    '油条', '葱油饼', '烧饼',
    '汉堡面包', '披萨饼底',
    '河粉(炒)', '年糕(炒)', '凉皮', '肠粉(鸡蛋)', '烧麦(糯米)',
    '煎饼果子', '手抓饼', '春卷(炸)', '窝窝头', '发糕', '烙饼',
    '炒面', '意大利面(煮)', '螺蛳粉(煮)', '酸辣粉', '冷面',
    '热干面', '担担面',
}

# 旧 cat2 → 新 cat6（中西菜肴）：加工肉制品
TO_CAT6_FROM_CAT2 = {
    '羊肉串(烤)', '鸡块(炸)', '鸡翅(炸)', '牛排(煎)',
    '培根', '火腿肠', '香肠(熟)', '腊肉', '午餐肉', '肉松',
    '鱼丸', '虾滑', '蟹棒',
}

# 旧 cat6 → 新 cat6（中西菜肴）：方便速食
TO_CAT6_FROM_CAT6 = {
    '速冻水饺', '汤圆(黑芝麻)', '粽子(肉)', '月饼(莲蓉)',
    '自热米饭', '方便面(泡)',
}

# ---- 基础映射（old_cat → new_cat） ----
BASE_MAP = {
    1: 1,   # 主食 → 主食（大部分保留，部分移至6）
    2: 3,   # 肉蛋奶豆 → 肉蛋奶（大部分保留，豆制品移至4）
    3: 2,   # 蔬菜 → 蔬果
    4: 2,   # 水果 → 蔬果
    5: 5,   # 饮品 → 零食饮料
    6: 5,   # 零食糕点 → 零食饮料（大部分保留，坚果移至4）
}

# ---- 读取 ----
with open(INPUT, 'r', encoding='utf-8') as f:
    content = f.read()

# ---- 更新分类 INSERT 语句 ----
old_cats = """INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('主食', 'staple', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('肉蛋奶豆', 'meat', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('蔬菜', 'vegetable', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('水果', 'fruit', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('饮品', 'drink', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('零食糕点', 'snack', NOW());"""

new_cats = """INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('主食', 'staple', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('蔬果', 'veg_fruit', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('肉蛋奶', 'meat', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('豆类坚果', 'bean_nut', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('零食饮料', 'snack_drink', NOW());
INSERT IGNORE INTO food_categories (name, icon, created_at) VALUES ('中西菜肴', 'dish', NOW());"""

content = content.replace(old_cats, new_cats)

# ---- 更新章节标题注释 ----
content = content.replace("-- ==================== 2. 肉蛋奶豆 (category_id=2) ====================",
                          "-- ==================== 3. 肉蛋奶 (category_id=3) ====================")
content = content.replace("-- ==================== 3. 蔬菜 (category_id=3) ====================",
                          "-- ==================== 2. 蔬果 — 蔬菜 (category_id=2) ====================")
content = content.replace("-- ==================== 4. 水果 (category_id=4) ====================",
                          "-- ==================== 2. 蔬果 — 水果 (category_id=2) ====================")
content = content.replace("-- ==================== 5. 饮品 (category_id=5) ====================",
                          "-- ==================== 5. 零食饮料 — 饮品 (category_id=5) ====================")
content = content.replace("-- ==================== 6. 零食糕点 (category_id=6) ====================",
                          "-- ==================== 5. 零食饮料 — 零食糕点 (category_id=5) ====================")

# ---- 逐行处理食物 INSERT ----
lines = content.split('\n')
new_lines = []
current_section = None  # 记录当前处理的食物章节
in_foods_insert = False
pending_cat6_lines = []  # 收集需要写入中西菜肴章节的食物

for line in lines:
    # 检测食物 INSERT 开始
    if 'INSERT IGNORE INTO foods' in line:
        in_foods_insert = True

    # 处理包含 category_id 的食物数据行
    if in_foods_insert and re.search(r"'\s*,\s*\d+\s*,", line):
        # 提取食物名称和 category_id
        m = re.match(r"\s*\('(.+?)'\s*,\s*(\d+)\s*,", line)
        if m:
            name = m.group(1)
            old_cat = int(m.group(2))
            new_cat = BASE_MAP.get(old_cat, old_cat)

            # 检查是否需要特殊处理
            if old_cat == 1 and name in TO_CAT6_FROM_CAT1:
                new_cat = 6
            elif old_cat == 2 and name in TO_CAT4_FROM_CAT2:
                new_cat = 4
            elif old_cat == 2 and name in TO_CAT6_FROM_CAT2:
                new_cat = 6
            elif old_cat == 6 and name in TO_CAT4_FROM_CAT6:
                new_cat = 4
            elif old_cat == 6 and name in TO_CAT6_FROM_CAT6:
                new_cat = 6

            # 替换 category_id
            line = re.sub(r"'\s*,\s*\d+\s*,", f"', {new_cat}, ", line, count=1)

    # 检测 INSERT 结束
    if in_foods_insert and line.rstrip().endswith(';'):
        in_foods_insert = False

    new_lines.append(line)

# ---- 写入 ----
with open(OUTPUT, 'w', encoding='utf-8') as f:
    f.write('\n'.join(new_lines))

print("Done: category remapping complete!")
print("新分类：1主食 2蔬果 3肉蛋奶 4豆类坚果 5零食饮料 6中西菜肴")

# ---- 统计 ----
cat_counts = {}
for line in new_lines:
    m = re.search(r"'\s*,\s*(\d+)\s*,", line)
    if m:
        c = int(m.group(1))
        cat_counts[c] = cat_counts.get(c, 0) + 1
for c in sorted(cat_counts):
    names = {1:'主食', 2:'蔬果', 3:'肉蛋奶', 4:'豆类坚果', 5:'零食饮料', 6:'中西菜肴'}
    print(f"  cat {c} ({names.get(c, '?')}): {cat_counts[c]} 种")
