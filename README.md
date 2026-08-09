# 神月健康 · 卡路里追踪 App

前后端分离的全栈卡路里追踪应用。用户通过 14 步引导流程输入身体数据和生活习惯，系统用 **Mifflin-St Jeor 公式**计算 BMR、根据评估数据估算 TDEE，再用百分比热量缺口（10%–25% TDEE）算出每日推荐摄入热量。支持体重追踪、饮食记录与三餐管理、运动记录（MET 估算消耗）、食物收藏与自定义食物。

专注工程化：**单元/集成测试 + Docker 一键部署 + GitHub Actions CI + Redis 缓存 + Actuator 健康检查 + Swagger 接口文档**。

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21 · Spring Boot 4.1 · Spring Security(JWT) · Spring Data JPA · Spring Cache(Redis) · Actuator · springdoc-openapi |
| 数据库 | MySQL 8 · Redis 7 |
| 前端 | Vue 3 (Composition API) · Vite 8 · Element Plus · ECharts |
| 工程化 | Maven · Docker Compose · GitHub Actions · JaCoCo · Vitest |

---

## 快速开始（Docker 一键部署）

> 前置：安装 Docker + Docker Compose。

```bash
# 1. 配置环境变量（复制示例并填写）
cp .env.example .env

# 2. 一键启动（MySQL + Redis + 后端 + 前端）
docker compose up -d --build
```

启动后访问：

| 入口 | 地址 |
|---|---|
| 前端 | http://localhost:8081 |
| 后端 | http://localhost:8080 |
| Swagger 接口文档 | http://localhost:8080/swagger-ui/index.html |
| 健康检查 | http://localhost:8080/actuator/health |

数据库数据保存在 `mysql-data` volume，重建容器不丢数据。

---

## 本地开发

### 后端（需先启动 MySQL 和 Redis）

```bash
# 启动 MySQL（本机 MySQL 8，库名 calorie_tracker，root/123456）
# 启动 Redis（Windows 无 Docker 时可用 WSL 或 docker run）
docker run -d -p 6379:6379 redis:7

# 启动后端（默认端口 8080）
cd calorie-server
./mvnw spring-boot:run
```

开发时改代码用 `spring-boot-devtools` 热重载；**改动 pom.xml 新增依赖后需完全重启**。

### 前端

```bash
cd calorie-tracker-web
npm install
npm run dev   # Vite dev server，默认 5173，/api 代理到 8080
```

---

## 测试

```bash
# 后端：255 个用例（Service + Controller + Util），含 JaCoCo 覆盖率报告
cd calorie-server && ./mvnw test
# 覆盖率报告：calorie-server/target/site/jacoco/index.html

# 前端：Vitest 12 个用例
cd calorie-tracker-web && npm test
```

CI（GitHub Actions，`.github/workflows/ci.yml`）每次 push 自动跑：后端测试（含 MySQL 服务容器）、前端构建 + 测试、Docker 镜像构建。

---

## API 文档

启动后访问 Swagger UI：`http://localhost:8080/swagger-ui/index.html`

- 公共接口：注册 `/api/auth/register`、登录 `/api/auth/login`
- 其余接口需 JWT——在 Swagger 右上角 **Authorize** 填入登录返回的 token 即可试调
- 路由总览见下方「项目结构」或 Swagger

---

## 生产部署环境变量

所有敏感配置通过环境变量注入，**不得写死在代码或提交到仓库**。生产部署前必须设置：

```bash
# 数据库（必设）
DB_PASSWORD=<强密码>

# JWT 密钥（必设，生成长随机串，泄露=可伪造登录，泄露旧 secret 会导致所有已签发 token 可被伪造）
JWT_SECRET=$(openssl rand -base64 48)

# 邮件 SMTP（用于"忘记密码"，必设）
MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=<你的发件邮箱>
MAIL_PASSWORD=<SMTP 授权码，不是登录密码>
```

> ⚠️ 生产切不可用仓库里的测试默认密钥。`.env` 已在 `.gitignore` 排除，不会进代码库。

---

## 项目结构

```
├── calorie-server/            # Spring Boot 后端
│   └── src/main/java/com/example/calorieserver/
│       ├── controller/        # REST 控制器（/api/auth /foods /weights /records /exercises ...）
│       ├── service/           # 业务逻辑（含核心热量计算）
│       ├── util/CalorieCalculator.java  # 核心引擎：BMR/TDEE/热量缺口/营养素配比
│       ├── repository/        # Spring Data JPA
│       ├── entity/ dto/ config/ security/
├── calorie-tracker-web/       # Vue 3 前端
├── scripts/                   # data.sql 生成、食物图片爬虫等工具
├── docker-compose.yml         # MySQL + Redis + 后端 + 前端 一键编排
└── .github/workflows/ci.yml   # GitHub Actions CI
```

---

## 健康检查

`/actuator/health` 免登录返回后端及其依赖（MySQL / Redis / SMTP / 磁盘）的健康状态，供 Docker healthcheck、监控与负载均衡探活。Docker 编排中 backend 容器每 30s 探测一次，异常自动重启。

> 仅暴露 `health, info` 两个端点，`env`/`heapdump` 等敏感端点默认不暴露。