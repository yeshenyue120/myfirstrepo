# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## User Rules

1. 修改代码前先向我确认再进行代码更改，更改后告诉我更改了哪些文件
2. 当需要开启多个子进程时，先向我询问过后再进行

## Project Overview

卡路里追踪应用（Calorie Tracker）— 前后端分离的全栈项目。用户通过 14 步引导流程输入身体数据和生活习惯，系统使用 Mifflin-St Jeor 公式计算 BMR、根据评估数据估算 TDEE，再用百分比热量缺口（10%-25% TDEE）计算每日推荐摄入热量（类似薄荷健康的方式）。支持体重记录追踪、饮食记录和三餐管理、运动记录（MET 估算消耗）、食物收藏与自定义食物。

## Commands

### Backend（`calorie-server/`）

```bash
# 启动（需要 MySQL，默认端口 8080）
cd calorie-server && ./mvnw spring-boot:run

# 打包
./mvnw clean package -DskipTests
```

- Java 21, Spring Boot 4.1.0, Maven
- 数据库初始化脚本：`src/main/resources/data.sql`（建表 + 分类 + 461 种食物，`INSERT IGNORE` 幂等）
- 开发时使用 `spring-boot-devtools` 热重载
- 配置在 `src/main/resources/application.yaml`：数据库 `calorie_tracker`（root/123456），`sql.init.mode: always` 导致 data.sql 每次启动执行，`ddl-auto: update` 由 Hibernate 自动建表/改表

### Frontend（`calorie-tracker-web/`）

```bash
cd calorie-tracker-web && npm run dev    # Vite dev server，默认端口 5173
npm run build                            # 生产构建
```

- Vue 3 (Composition API + `<script setup>`), Vite 8, Element Plus 2
- Vite proxy：`/api` → `http://localhost:8080`（后端地址）
- ECharts 依赖声明在根目录 `package.json`（`echarts ^6.1.0`），`Home.vue` 使用

### Scripts（`scripts/`）

```bash
# 爬取食物图片（依赖 icrawler，Bing 搜索，只爬 is_common=true 的食物）
pip install icrawler && cd scripts && python crawl_food_images.py

python gen_data_sql.py      # 生成 data.sql
python remap_categories.py  # 食物分类重映射
```

## Architecture

```
D:\Project
├── calorie-server/          # Spring Boot 后端
│   └── src/main/java/com/example/calorieserver/
│       ├── controller/      # REST 控制器
│       │   ├── AuthController.java      # /api/auth/*  注册/登录/onboarding提交
│       │   ├── FoodController.java      # /api/foods/*  搜索/分类/收藏/自定义食物
│       │   ├── WeightController.java    # /api/weights/*  体重记录
│       │   ├── MealRecordController.java # /api/records/*  饮食记录（含批量/汇总/趋势/营养素）
│       │   └── ExerciseController.java  # /api/exercises/*  运动记录
│       ├── service/         # 业务逻辑
│       │   ├── UserService / WeightService / MealRecordService
│       │   ├── FoodService        # 搜索、收藏、自定义食物
│       │   └── ExerciseService    # 运动记录 + 热量消耗计算
│       ├── repository/      # Spring Data JPA
│       ├── entity/          # JPA 实体
│       │   ├── User.java         # 用户（含身体数据、评估数据、系统计算结果）
│       │   ├── Food.java / FoodCategory.java / UserFavorite.java
│       │   ├── MealRecord.java   # 饮食记录
│       │   ├── WeightRecord.java # 体重记录
│       │   └── ExerciseRecord.java # 运动记录
│       ├── dto/             # 请求/响应 DTO
│       └── util/
│           └── CalorieCalculator.java  # 核心计算引擎（BMR/TDEE/热量目标/营养素）
├── calorie-tracker-web/     # Vue 3 前端
│   └── src/
│       ├── api/index.js          # Axios 实例（base /api，response 拦截器解包 data）
│       ├── router/index.js       # Vue Router 路由（/login /register /onboarding /home /profile /edit-profile /foods）
│       ├── styles/               # 设计 Token + 全局样式（见下方"设计 Token 体系"）
│       ├── views/
│       │   ├── Onboarding.vue    # 14 步引导流程容器
│       │   ├── Onboarding/       # Step1~Step14 各步骤组件
│       │   ├── Home.vue          # 主页（体重仪表盘 + 热量概览 + 三餐 + 运动 + 图表 + FAB）
│       │   ├── EditProfile.vue   # 编辑个人资料（实时预览热量）
│       │   ├── Foods.vue         # 食物库（常见/分类/收藏/自定义/我的上传）
│       │   ├── Login.vue / Register.vue / Profile.vue
│       │   └── ...
│       └── components/NavBar.vue
├── food_images/             # 爬虫下载的食物图片（按食物名分目录），前端当前未接入（用 emoji 图标）
└── scripts/
    ├── crawl_food_images.py # Bing 爬取食物图片，只爬 is_common=true 的食物
    ├── gen_data_sql.py      # 生成 data.sql
    └── remap_categories.py  # 分类重映射
```

## Key Design Decisions

### 热量计算流程（`CalorieCalculator.java`）

1. **BMR** — Mifflin-St Jeor 公式：`10×体重 + 6.25×身高 − 5×年龄 + 5(−161女)`
2. **TDEE** — BMR × 活动系数（从评估数据推算，非用户直接输入）：
   - 基准 1.2（久坐）
   - 应酬多 +0.15，有时 +0.075
   - 容易饿 +0.1，有时 +0.05
   - 有体脂秤 +0.05
   - 上限 1.725
3. **每日热量目标** — `calculateCaloriesByDays()`：百分比缺口模式
   - 所需缺口比例 = `减重公斤数 × 7700 / 目标天数 / TDEE`
   - 减重：缺口 = clamp(所需%, 10%, 25%) × TDEE → 目标 = TDEE × (1 − 缺口%)
   - 增重：盈余 = min(TDEE × 10%~20%, 500) → 目标 = TDEE + 盈余
   - 安全钳位：[1500(男)/1200(女), TDEE+500]
4. **推荐天数** — `calculateRecommendedDays()`：默认 15% TDEE 缺口，保守对象 10%，下限 14 天
5. **保守减脂对象** — 用过减肥药 / 甲减 / 控制不了饮食 / 经常应酬 / 容易饿
6. **营养素比例** — 减重默认 蛋白质30%/脂肪25%/碳水45%；容易饿→蛋白提至 35~40%；糖尿病→低碳水 35%
7. 其他工具方法：BMI、BMI 颜色分级、体脂率估算、腰臀比、推荐餐次（控制不住 5 餐 / 容易饿 4~5 餐 / 默认 3 餐）

### 运动消耗计算（`ExerciseService.calculateCalories()`）

- 公式：`MET 值 × 体重(kg) × 时长(小时) × 1.05`（`KCAL_PER_KG_PER_MET_HOUR = 1.05`），保留 1 位小数
- 后端按记录时刻的用户体重重算消耗，不信任前端传入值；`ExerciseRecord` 存 `exerciseType / durationMin / metValue / caloriesBurned / recordDate`
- 前端 `Home.vue` 的运动区块调用 `/api/exercises/{userId}/summary` 展示今日消耗合计

### 体重追踪

- `WeightService.addWeight()`：同一天多次记录会更新最新一条而非新增，自动清理历史重复
- `WeightRecordRepository`：查询用 `OrderByRecordedDateDescIdDesc` 双字段排序（日期 + ID），避免同日多条顺序不确定
- 前端主页使用进度条仪表盘（非 SVG），显示起始体重→目标体重的进度

### 食物数据库

- `data.sql` 使用 `INSERT IGNORE`，可重复执行不会重复插入（且 `sql.init.mode: always` 每次启动都会跑）
- 6 个分类：主食🍚、蔬果🥬、肉蛋奶🥩、豆类坚果🫘、零食饮料🍪、中西菜肴🍲（每行含 emoji `icon`）
- 每 100g 数据：`calories / protein / fat / carbs`
- 两列布尔标志：`is_public=true` 为系统种子食物（461 种，全员可见）；`is_common=true` 为高频展示食物（70 种，前端"常见"标签页 + 爬虫只爬这些）
- 自定义食物：`is_public=false, is_common=false`，`creator` 关联创建用户

### 食物收藏与自定义

- 收藏用 `UserFavorite` 实体（User↔Food 关联），接口在 `FoodService`
- 前端 `Foods.vue` 提供 常见⭐ / 分类 / 收藏❤️ / 自定义✏️ / 我的上传📤 标签页
- 自定义食物可删除（仅创建者可删），`FoodController` 校验归属

### 前端状态管理

- 无 Vuex/Pinia，使用 `localStorage` 存储 user 对象
- `Home.vue` 的 `onMounted` 每次进入页面重新从 localStorage 读取 user（确保 EditProfile 修改后数据同步）
- `EditProfile.vue` 的 `previewCalories()` 实时使用当前表单数据计算 BMR/TDEE，不依赖 localStorage 缓存
- `api/index.js`：axios baseURL `/api`，response 拦截器自动解包 `response.data`，请求失败打印并 reject

### 设计 Token 与样式体系（`src/styles/`）

- `tokens.css`：根级 CSS 自定义属性定义全套设计 Token——健康绿主色（`--color-primary: #059669`）、强调橙（`--color-accent: #ea580c`）、语义色（danger/warning/info）、暖绿底调中性色、文字色层级、圆角/间距/阴影
- `global.css`（全局基础样式）、`animations.css`（动画）、`auth-animations.css`（登录注册动画）、`onboarding-step.css`（引导步骤样式）
- `main.js` 按序引入 `tokens.css → animations.css → global.css`，改动主题颜色优先改 `tokens.css`

### 图表

- `Home.vue` 用 `echarts/core` 按需引入渲染 3 个图表：体重历史趋势、热量摄入趋势、营养素摄入 vs 目标
- 数据不足 2 个点时显示空态提示而非图表

### API 约定

- 前端 Axios 响应拦截器自动解包 `response.data`
- 后端返回 `ResponseEntity<T>`，HTTP 状态码语义化
- 路由：
  - `/api/auth/*`：`POST /register`、`POST /login`、`PUT /onboarding/{userId}`
  - `/api/foods/*`：`GET /categories`、`/common`、`/category/{id}`、`/search?keyword=`、`/favorites/{userId}`、`/favorites/ids/{userId}`；`POST|DELETE /favorite`、`POST /custom`、`DELETE /custom/{foodId}`、`GET /creator/{userId}`
  - `/api/weights/*`：`POST /{userId}`、`GET /latest/{userId}`、`GET /history/{userId}`
  - `/api/records/*`：`POST /{userId}`、`POST /batch/{userId}`、`GET /{userId}`（按日期）、`GET /{userId}/meal`（按日期+餐次）、`GET /{userId}/total`、`GET /summary/{userId}`（三餐汇总）、`GET /{userId}/trend`（近 7 天热量趋势）、`GET /nutrition/{userId}`（营养素摄入 vs 目标）；`PUT /{recordId}`、`DELETE /{recordId}`、`DELETE /{userId}/date`
  - `/api/exercises/*`：`POST /{userId}`、`GET /{userId}`（按日期）、`GET /{userId}/summary`（今日汇总）、`PUT /record/{recordId}`、`DELETE /record/{recordId}`
