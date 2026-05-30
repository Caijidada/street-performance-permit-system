# 街头演出点位报备系统

> 武汉科技大学城市学院 毕业设计项目
> 作者：彭定坤

基于 Spring Boot 3 + Vue 3 的街头演出场地预约与审批管理系统，实现艺人报备、两级审批、信用管理、电子准演证生成与核验等完整业务闭环。

---

## 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.3.5 | 主框架 |
| MyBatis Plus | 3.x | ORM |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 6+ | 分布式锁 / 缓存 |
| Sa-Token | 1.39.0 | JWT 鉴权 |
| ZXing | 3.5.x | 二维码生成 |
| Hutool | 5.x | 工具库（AES加密等） |
| Knife4j | 4.x | API 文档（Swagger UI） |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.5.x | 主框架（Composition API） |
| Vite | 8.x | 构建工具 |
| Element Plus | 2.13.x | UI 组件库 |
| Pinia | 3.x | 状态管理 |
| Vue Router | 4.x | 路由 |
| ECharts | 6.x | 数据可视化 |
| 高德地图 JS API | 2.0 | 地图选点 / 地理编码 |
| jsQR | — | 前端二维码图片解析 |
| Axios | 1.x | HTTP 请求 |

---

## 功能模块

### 艺人端
- **地图选点报备**：高德地图选点，填写演出时段、类型、内容，系统自动进行时空冲突检测
- **我的报备**：查看报备历史及状态，已通过报备可查看 / 下载电子准演证二维码
- **信用记录**：查看信用分环形图、历史变动记录
- **个人中心**：实名信息展示、修改密码、上传演艺资质
- **消息通知**：顶栏铃铛图标，实时显示未读数；审批通过/驳回/撤销后自动推送通知

### 管理员端
- **数据大屏**：报备状态饼图、近 14 天日均趋势柱状图、热门点位热力图（高德地图）
- **报备审批**：两级审批流（一审 → 终审），Tab 筛选全部 / 各状态；同一审批人不可同时担任一审与终审；终审通过后自动生成电子准演证
- **点位管理**：高德地图选点，设置开放时段、开放星期（1-7）、允许类型、最大音量/人数；选点后显示 100m 时空冲突围栏圈
- **艺人管理**：查看艺人列表、信用分、资质审核状态，支持启用/禁用账号、信用手动操作
- **信用管理**：按艺人姓名搜索执行信用操作，支持 7 种内置事件类型及自定义事件（可管理增删，localStorage 持久化）；日志列表展示艺人姓名
- **扫码核验**：顶栏"扫码核验"按钮，上传二维码图片 → jsQR 解析 → 展示艺人信息 → 一键跳转信用评分

### 公开接口
- **证件核验页**：`/verify/:certCode`，扫码后展示证件详情，移动端自适应

---

## 核心业务逻辑

### 时空冲突检测
报备提交时后端检查：
1. 同点位在该时段是否已有报备
2. 100 米范围内邻近点位在同一时段是否有冲突

### 两级审批流
```
提交报备(待一审) → 一审通过(待终审) → 终审通过(已通过) → 自动生成准演证
                 ↓ 驳回(已驳回)      ↓ 驳回(已驳回)
```
- 同一管理员不可既做一审又做终审
- 管理员可撤销已通过报备，同步失效对应准演证

### 信用体系
| 事件 | 分值变化 |
|------|----------|
| 按时演出 | +2 |
| 迟到 15 分钟内 | -5 |
| 迟到超 15 分钟 | -10 |
| 超时演出 | -8 |
| 扰民投诉 | -15 |
| 爽约 | -20 |
| 系统奖励 | +10 |
| 自定义 | 管理员自定义 |

- 信用分 < 60：不可报备热门点位（最大观众 > 50 人）
- 信用分 < 40：账号受限

### 电子准演证
终审通过后用 ZXing 生成 QR 码 PNG，内容为系统核验 URL（`{base-url}/verify/{certCode}`）。管理员端可上传二维码图片通过 jsQR 解析核验。

---

## 项目结构

```
pdk/
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/pdk/
│       ├── common/             # 公共：Result、异常、枚举、配置
│       ├── module/
│       │   ├── auth/           # 用户注册登录、用户管理
│       │   ├── report/         # 报备单（提交、查询、撤销）
│       │   ├── approval/       # 两级审批流
│       │   ├── venue/          # 点位管理
│       │   ├── certificate/    # 电子准演证生成与核验
│       │   ├── credit/         # 信用分管理
│       │   ├── notice/         # 消息通知
│       │   └── statistics/     # 数据统计大屏
│       └── util/               # 工具类（QrCodeUtil、SnowflakeIdGenerator）
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── api/                # Axios 接口封装
│       ├── stores/             # Pinia 状态（用户信息）
│       ├── utils/              # constants、request 拦截器
│       └── views/
│           ├── admin/          # 管理员端页面
│           └── artist/         # 艺人端页面
└── database/
    ├── schema.sql              # 建表脚本
    ├── mock_data.sql           # 测试数据
    └── indexes.sql             # 索引优化
```

---

## 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6+
- Maven 3.8+

---

## 快速启动

### 1. 数据库初始化

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p pdk_db < database/mock_data.sql
```

默认数据库配置：`localhost:3306/pdk_db`，用户名 `root`，密码 `123456`。

### 2. 启动 Redis

```bash
redis-server
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。
API 文档：`http://localhost:8080/doc.html`（Knife4j）

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`。

---

## 配置说明

关键配置项位于 `backend/src/main/resources/application.yml`，生产环境建议通过环境变量覆盖：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `PDK_JWT_SECRET` | `pdk-street-performance-system-secret-2024` | JWT 签名密钥 |
| `PDK_AES_KEY` | `pdk@AES#Key20241` | 身份证 AES 加密密钥（必须 16 位） |
| `PDK_AMAP_KEY` | 请配置高德地图KEY | 高德地图 JS API Key |
| `PDK_BASE_URL` | `http://localhost:5173` | 前端地址（写入二维码 URL） |
| `PDK_UPLOAD_PATH` | `/tmp/pdk-uploads/` | 文件上传目录 |

前端配置位于 `frontend/.env`（如不存在请创建）：

```env
VITE_AMAP_KEY=你的高德地图JS_API_Key
VITE_API_BASE=/api
```

---

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `123456` |
| 艺人（示例） | `artist1` | `123456` |

---

## 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 艺人注册 |
| POST | `/api/auth/login` | 登录（返回 Token） |
| POST | `/api/report/submit` | 提交报备 |
| GET | `/api/report/my` | 我的报备列表 |
| GET | `/api/admin/approval/list` | 报备审批列表 |
| POST | `/api/admin/approval/{id}/action` | 执行审批（通过/驳回） |
| GET | `/api/certificate/verify/{certCode}` | 证件核验（公开） |
| GET | `/api/artist/notices` | 我的通知列表 |
| POST | `/api/admin/credit/operate` | 信用分手动操作 |
| GET | `/api/admin/statistics/overview` | 数据大屏统计 |

完整接口文档见启动后的 Knife4j：`http://localhost:8080/doc.html`

---

## 注意事项

1. **高德地图 Key**：需自行申请，前端 JS API Key 和后端 Web 服务 Key 分别填写；未配置时地图显示为占位提示，不影响其他功能
2. **二维码核验地址**：开发环境下二维码内嵌 `localhost:5173`，手机扫码需将 `PDK_BASE_URL` 改为局域网 IP，同时 `vite.config.js` 已配置 `host: true` 支持局域网访问
3. **文件上传**：上传的资质文件和证件图片保存在 `PDK_UPLOAD_PATH` 目录，通过 `/files/**` 静态资源路由访问
