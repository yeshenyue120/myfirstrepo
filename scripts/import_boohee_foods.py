#!/usr/bin/env python3
"""
将薄荷健康 CSV 食物数据转为 MySQL INSERT SQL。

用法:
    python import_boohee_foods.py

输入:  D:/Project/all.csv  (GBK 编码)
输出:  D:/Project/import_boohee_foods.sql  (UTF-8 编码)

输出文件可直接导入 MySQL:
    mysql -u root -p123456 calorie_tracker < import_boohee_foods.sql
"""

import csv
import os

CSV_PATH = os.path.join(os.path.dirname(__file__), "..", "all.csv")
OUT_PATH = os.path.join(os.path.dirname(__file__), "..", "import_boohee_foods.sql")

# 薄荷分类 → 我们的 category_id
# data.sql: 1=主食 2=蔬果 3=肉蛋奶 4=豆类坚果 5=零食饮料 6=中西菜肴
CATEGORY_MAP = {
    # 1 - 主食
    "谷薯芋、杂豆、主食": 1,

    # 2 - 蔬果
    "蔬果和菌藻": 2,

    # 3 - 肉蛋奶
    "蛋类、肉类及制品": 3,
    "奶类及制品": 3,
    "鸡蛋类": 3,
    "鹌鹑蛋类": 3,
    "奶粉类": 3,

    # 4 - 豆类坚果
    "坚果、大豆及制品": 4,

    # 5 - 零食饮料
    "饮料": 5,
    "零食、点心、冷饮": 5,
    "调味品": 5,
    "食用油、油脂及制品": 5,
    "棒冰、冰激凌类": 5,
    "固体饮料类": 5,
    "盐、味精及其它类": 5,
    "小吃类": 5,
    "醋类": 5,
    "蜂蜜": 5,
    "糖类": 5,
    "碳酸饮料类": 5,
    "快餐食品类": 5,
    "咸菜类": 5,

    # 6 - 中西菜肴（各地菜系、私家菜、家常菜、西餐、日韩等）
    "家常菜": 6,
    "私家菜": 6,
    "其它": 6,
    "其他菜肴": 6,
    "山东菜": 6, "北京菜": 6, "湖南菜": 6, "上海菜": 6,
    "四川菜": 6, "广州菜": 6, "广东菜": 6, "滇黔菜": 6,
    "河南菜": 6, "浙江菜": 6, "东北菜": 6, "福建菜": 6,
    "湖北菜": 6, "江西菜": 6, "山西菜": 6, "江苏菜": 6,
    "陕西菜": 6, "安徽菜": 6, "甘肃菜": 6, "台湾菜": 6,
    "天津菜": 6, "新疆菜": 6, "广西菜": 6, "海南菜": 6,
    "青海菜": 6, "宁夏菜": 6, "少数民族菜": 6,
    "其他西餐": 6, "法国菜": 6, "意大利菜": 6,
    "日本料理": 6, "韩国料理": 6, "东南亚风味": 6,
    "素斋菜": 6, "清真菜": 6,
    # 烹饪方式（归入菜肴）
    "砂锅、煮": 6, "凉拌": 6, "煎": 6, "烤": 6,
    "炸": 6, "炖": 6, "炒": 6, "清蒸": 6,
    "干煸": 6, "锅塌": 6,
}

DEFAULT_CATEGORY = 6  # 未知分类默认放中西菜肴


def escape_sql(s: str) -> str:
    """转义单引号，去掉首尾空白"""
    return s.strip().replace("'", "''")


def safe_float(val: str) -> str:
    """返回数值字符串，解析失败返回 NULL"""
    val = val.strip()
    if not val:
        return "NULL"
    try:
        return str(float(val))
    except ValueError:
        return "NULL"


def main():
    valid = 0
    skipped_empty_cal = 0
    skipped_name_too_long = 0
    sql_lines = []

    sql_lines.append("-- ============================================================")
    sql_lines.append("-- 薄荷健康食物数据导入（自动生成）")
    sql_lines.append("-- 用法: mysql -u root -p123456 calorie_tracker < import_boohee_foods.sql")
    sql_lines.append("-- ============================================================")
    sql_lines.append("")
    sql_lines.append("SET NAMES utf8mb4;")
    sql_lines.append("")

    with open(CSV_PATH, "r", encoding="gbk", errors="replace") as f:
        reader = csv.reader(f)
        header = next(reader)

        for row in reader:
            if len(row) < 11:
                skipped_empty_cal += 1
                continue

            name = row[2].strip()
            category_str = row[3].strip()
            cal_str = row[7].strip()
            carb_str = row[8].strip()
            fat_str = row[9].strip()
            protein_str = row[10].strip()

            # ---- 校验 ----
            if not name:
                skipped_empty_cal += 1
                continue
            if len(name) > 100:
                skipped_name_too_long += 1
                continue
            if not cal_str or cal_str == "0":
                skipped_empty_cal += 1
                continue

            calories = safe_float(cal_str)
            carbs = safe_float(carb_str)
            fat = safe_float(fat_str)
            protein = safe_float(protein_str)

            if calories == "NULL":
                skipped_empty_cal += 1
                continue

            category_id = CATEGORY_MAP.get(category_str, DEFAULT_CATEGORY)

            escaped_name = escape_sql(name)

            # INSERT ... ON DUPLICATE KEY UPDATE: 名字冲突时更新营养素和分类
            sql = (
                f"INSERT INTO foods (name, category_id, calories_per100g, "
                f"protein_per100g, fat_per100g, carbs_per100g, "
                f"is_public, is_common, created_at) VALUES ("
                f"'{escaped_name}', {category_id}, {calories}, "
                f"{protein}, {fat}, {carbs}, "
                f"true, false, NOW()) "
                f"ON DUPLICATE KEY UPDATE "
                f"category_id = {category_id}, "
                f"calories_per100g = {calories}, "
                f"protein_per100g = {protein}, "
                f"fat_per100g = {fat}, "
                f"carbs_per100g = {carbs};"
            )
            sql_lines.append(sql)
            valid += 1

    # 写 SQL 文件
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines))
        f.write("\n")

    print(f"=== 转换完成 ===")
    print(f"有效导入:   {valid} 条")
    print(f"跳过(无热量): {skipped_empty_cal} 条")
    print(f"跳过(名称过长): {skipped_name_too_long} 条")
    print(f"输出文件:   {OUT_PATH}")


if __name__ == "__main__":
    main()
