# 街头演出点位报备系统 — 系统架构设计文档

> 作者：架构设计
> 版本：v1.0
> 日期：2026-04-02
> 技术栈：Vue 3 + Spring Boot 3.0 + MySQL 8.0 + Redis

---

## 目录

1. [系统概述](#1-系统概述)
2. [整体架构](#2-整体架构)
3. [前端架构设计](#3-前端架构设计)
4. [后端架构设计](#4-后端架构设计)
5. [数据库设计](#5-数据库设计)
6. [核心算法设计](#6-核心算法设计)
7. [接口设计规范](#7-接口设计规范)
8. [安全设计](#8-安全设计)
9. [部署架构](#9-部署架构)
10. [目录结构](#10-目录结构)

---

## 1. 系统概述

### 1.1 项目背景

解决城市公共空间街头演出存在的无序竞争、噪音扰民及管理部门监管困难等痛点，构建数字化街头演艺管理平台。

### 1.2 用户角色

| 角色 | 说明 | 核心操作 |
|------|------|----------|
| 街头艺人 | 演出申请方 | 注册认证、地图选点、提交报备、查看审批进度、持证演出 |
| 文旅/城管管理员 | 审批管理方 | 点位管理、报备审批、扫码核验、信用扣分、数据查看 |
| 超级管理员 | 系统运营方 | 用户管理、权限配置、系统设置、全局数据统计 |

### 1.3 核心功能模块

- **用户认证模块**：实名注册、演艺资质上传、JWT 鉴权、角色权限控制
- **地图选点报备模块**：可视化点位展示、时段选择、冲突预检、报备提交
- **在线审批流模块**：一审/终审流程、驳回申诉、电子准演证生成（含二维码）
- **信用评价模块**：扫码核验、信用加减分、违规记录追溯、优先排期权
- **数据统计大屏**：演出热力图、点位预约率、艺人活跃度、区县分布分析

---

## 2. 整体架构

### 2.1 架构模式

采用**前后端分离 B/S 架构**，前端 SPA 通过 RESTful API 与后端通信。

```
┌─────────────────────────────────────────────────────────┐
│                        客户端层                          │
│   ┌──────────────────┐      ┌──────────────────────┐    │
│   │  艺人端 (Web SPA) │      │  管理端 (Web SPA)    │    │
│   │  Vue 3 + Vite    │      │  Vue 3 + Element Plus│    │
│   └────────┬─────────┘      └──────────┬───────────┘    │
└────────────┼──────────────────────────┼────────────────-┘
             │          HTTPS           │
┌────────────▼──────────────────────────▼────────────────-┐
│                       接入层 (Nginx)                      │
│              反向代理 / 静态资源 / SSL 卸载               │
└────────────────────────┬───────────────────────────────-┘
                         │
┌────────────────────────▼───────────────────────────────-┐
│                    应用层 (Spring Boot 3.0)               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │用户认证  │ │报备审批  │ │点位管理  │ │数据统计    │  │
│  │模块      │ │模块      │ │模块      │ │模块        │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │信用评价  │ │证件生成  │ │冲突检测  │ │文件上传    │  │
│  │模块      │ │模块      │ │模块      │ │模块        │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘  │
└────────────────────────┬───────────────────────────────-┘
                         │
┌──────────────┬──────────▼──────────┬─────────────────--┐
│   MySQL 8.0  │      Redis 7.x      │   本地/OSS 存储    │
│   主数据存储  │  缓存 + 分布式锁    │   文件/图片/证件   │
└──────────────┴─────────────────────┴─────────────────--┘
```

### 2.2 技术选型总览

| 层次 | 技术 | 说明 |
|------|------|------|
| 前端框架 | Vue 3 + Composition API | 现代化响应式前端 |
| 前端构建 | Vite | 快速编译打包 |
| UI 组件库 | Element Plus | 表单/表格/审批流界面 |
| 地图服务 | 高德地图 Web API | 点位可视化、围栏绘制 |
| 数据可视化 | ECharts | 热力图、柱状图、饼图 |
| 后端框架 | Spring Boot 3.0 | RESTful API 服务 |
| ORM 框架 | MyBatis Plus | 简化数据库 CRUD |
| 主数据库 | MySQL 8.0 | 核心业务数据存储 |
| 缓存/锁 | Redis 7.x | 热点缓存 + 分布式锁防超卖 |
| 鉴权方案 | JWT (Sa-Token) | 无状态 Token 鉴权 |
| 文件存储 | 本地存储 / 阿里云 OSS | 资质文件、电子证件 |
| 接口文档 | Knife4j (Swagger 3) | API 文档自动生成 |

---

## 3. 前端架构设计

### 3.1 目录结构

```
frontend/
├── public/
├── src/
│   ├── api/                  # 接口请求层（按模块拆分）
│   │   ├── auth.js
│   │   ├── venue.js
│   │   ├── report.js
│   │   ├── approval.js
│   │   └── statistics.js
│   ├── assets/               # 静态资源
│   ├── components/           # 公共组件
│   │   ├── MapPicker.vue     # 地图选点组件
│   │   ├── QrCode.vue        # 二维码组件
│   │   └── CreditBadge.vue   # 信用分徽章
│   ├── composables/          # Composition API 复用逻辑
│   │   ├── useMap.js         # 地图操作逻辑
│   │   ├── useConflict.js    # 冲突检测逻辑
│   │   └── useAuth.js        # 鉴权逻辑
│   ├── router/               # Vue Router 路由配置
│   │   └── index.js
│   ├── stores/               # Pinia 状态管理
│   │   ├── user.js
│   │   ├── venue.js
│   │   └── approval.js
│   ├── views/
│   │   ├── artist/           # 艺人端页面
│   │   │   ├── Login.vue
│   │   │   ├── Register.vue
│   │   │   ├── MapReport.vue     # 地图选点报备
│   │   │   ├── MyReports.vue     # 我的报备列表
│   │   │   └── MyCertificate.vue # 我的准演证
│   │   └── admin/            # 管理端页面
│   │       ├── Dashboard.vue     # 数据大屏
│   │       ├── VenueManage.vue   # 点位管理
│   │       ├── ApprovalList.vue  # 审批列表
│   │       ├── UserManage.vue    # 艺人管理
│   │       └── CreditManage.vue  # 信用管理
│   ├── utils/
│   │   ├── request.js        # Axios 封装（统一拦截器）
│   │   └── constants.js      # 全局常量
│   └── main.js
├── index.html
├── vite.config.js
└── package.json
```

### 3.2 路由设计

```
/                         → 重定向至登录页
/login                    → 登录页
/register                 → 艺人注册页

/artist/                  → 艺人端（需登录）
  map-report              → 地图选点报备
  my-reports              → 我的报备记录
  certificate/:id         → 电子准演证详情

/admin/                   → 管理端（需管理员权限）
  dashboard               → 数据统计大屏
  venues                  → 点位管理
  approvals               → 报备审批列表
  approval/:id            → 审批详情
  users                   → 艺人管理
  credit                  → 信用管理
```

### 3.3 地图核心交互流程

```
艺人进入地图页
    │
    ▼
加载全市白名单点位（Marker 打点）
    │
    ▼
点击点位 → 弹出信息窗体（显示：允许音量/时段/当日预约状态）
    │
    ▼
选择演出时段 → 前端调用冲突预检接口
    │
    ├── 有冲突 → 高亮冲突区域（红色围栏）+ 提示
    │
    └── 无冲突 → 填写报备表单 → 提交
```

---

## 4. 后端架构设计

### 4.1 目录结构

```
backend/
└── src/main/java/com/pdk/
    ├── PdkApplication.java       # 启动类
    ├── common/
    │   ├── result/               # 统一响应体 Result<T>
    │   ├── exception/            # 全局异常处理
    │   ├── enums/                # 业务枚举（审批状态、违规类型等）
    │   └── constant/             # 全局常量
    ├── config/
    │   ├── RedisConfig.java
    │   ├── MybatisPlusConfig.java
    │   ├── SaTokenConfig.java    # JWT 鉴权配置
    │   └── SwaggerConfig.java
    ├── module/
    │   ├── auth/                 # 用户认证模块
    │   │   ├── controller/
    │   │   ├── service/
    │   │   ├── mapper/
    │   │   └── entity/
    │   ├── venue/                # 点位管理模块
    │   ├── report/               # 报备申请模块
    │   ├── approval/             # 审批流模块
    │   ├── credit/               # 信用评价模块
    │   ├── certificate/          # 电子证件模块
    │   └── statistics/           # 数据统计模块
    └── util/
        ├── ConflictDetector.java # 时空冲突检测工具
        ├── QrCodeUtil.java       # 二维码生成工具
        └── RedisLockUtil.java    # Redis 分布式锁工具
```

### 4.2 核心模块职责

| 模块 | Controller | Service 核心方法 |
|------|-----------|-----------------|
| auth | 登录/注册/资质上传 | login, register, uploadQualification |
| venue | 点位 CRUD、地图数据 | listMapVenues, getVenueDetail, addVenue |
| report | 提交报备、我的报备 | submitReport, preCheckConflict, listMyReports |
| approval | 审批列表、审批操作 | approve, reject, listPendingApprovals |
| credit | 信用操作、扫码核验 | verifyByCertCode, addCreditLog, getArtistCredit |
| certificate | 证件生成/下载 | generateCertificate, downloadCertificate |
| statistics | 大屏数据接口 | getHeatmapData, getVenueStats, getArtistRanking |

### 4.3 Redis 使用策略

| 场景 | Key 设计 | 策略 |
|------|----------|------|
| 热门点位基础数据缓存 | `venue:detail:{venueId}` | TTL 30 分钟，写入时更新 |
| 并发抢约分布式锁 | `lock:report:{venueId}:{date}:{timeSlot}` | SETNX，TTL 10s，防超卖 |
| 用户 Token 黑名单 | `token:blacklist:{token}` | TTL = Token 剩余有效期 |
| 验证码缓存 | `captcha:{uuid}` | TTL 5 分钟 |

---

## 5. 数据库设计

> 数据库：MySQL 8.0，字符集：utf8mb4，排序规则：utf8mb4_unicode_ci

### 5.1 ER 关系图（文字描述）

```
用户(t_user) ─── 一对多 ──→ 报备订单(t_report_order)
点位(t_venue) ─── 一对多 ──→ 报备订单(t_report_order)
报备订单(t_report_order) ─── 一对一 ──→ 电子准演证(t_certificate)
报备订单(t_report_order) ─── 一对多 ──→ 审批记录(t_approval_record)
用户(t_user) ─── 一对多 ──→ 信用日志(t_credit_log)
```

### 5.2 表结构详细设计

#### 5.2.1 用户表 `t_user`

```sql
CREATE TABLE t_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username        VARCHAR(50)     NOT NULL COMMENT '登录用户名',
    password        VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(50)     NOT NULL COMMENT '真实姓名',
    id_card         VARCHAR(18)     NOT NULL COMMENT '身份证号（加密存储）',
    phone           VARCHAR(11)     NOT NULL COMMENT '手机号',
    avatar          VARCHAR(255)    DEFAULT NULL COMMENT '头像URL',
    role            TINYINT         NOT NULL DEFAULT 1 COMMENT '角色：1-艺人 2-管理员 3-超管',
    credit_score    INT             NOT NULL DEFAULT 100 COMMENT '信用分（初始100）',
    qualification   VARCHAR(255)    DEFAULT NULL COMMENT '演艺资质证明文件URL',
    qualify_status  TINYINT         NOT NULL DEFAULT 0 COMMENT '资质审核状态：0-待审 1-通过 2-拒绝',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '账号状态：0-禁用 1-正常',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_id_card (id_card)
) ENGINE=InnoDB COMMENT='用户表';
```

#### 5.2.2 演出点位表 `t_venue`

```sql
CREATE TABLE t_venue (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name            VARCHAR(100)    NOT NULL COMMENT '点位名称',
    description     TEXT            DEFAULT NULL COMMENT '点位描述',
    province        VARCHAR(50)     DEFAULT NULL COMMENT '省',
    city            VARCHAR(50)     DEFAULT NULL COMMENT '市',
    district        VARCHAR(50)     DEFAULT NULL COMMENT '区/县',
    address         VARCHAR(255)    NOT NULL COMMENT '详细地址',
    longitude       DECIMAL(11,8)   NOT NULL COMMENT '经度',
    latitude        DECIMAL(10,8)   NOT NULL COMMENT '纬度',
    max_decibel     INT             NOT NULL DEFAULT 70 COMMENT '允许最大分贝(dB)',
    max_audience    INT             NOT NULL DEFAULT 50 COMMENT '最大允许观众人数',
    allow_types     VARCHAR(255)    DEFAULT NULL COMMENT '允许演出类型（逗号分隔）',
    open_time_start TIME            NOT NULL COMMENT '每日开放开始时间',
    open_time_end   TIME            NOT NULL COMMENT '每日开放结束时间',
    open_days       VARCHAR(20)     DEFAULT '1,2,3,4,5,6,7' COMMENT '开放星期（1-7，逗号分隔）',
    images          TEXT            DEFAULT NULL COMMENT '点位图片URL列表（JSON）',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '点位状态：0-关闭 1-开放',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_district (district),
    KEY idx_location (longitude, latitude)
) ENGINE=InnoDB COMMENT='演出点位表';
```

#### 5.2.3 报备订单表 `t_report_order`

```sql
CREATE TABLE t_report_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no        VARCHAR(32)     NOT NULL COMMENT '报备单号（唯一）',
    user_id         BIGINT          NOT NULL COMMENT '艺人用户ID',
    venue_id        BIGINT          NOT NULL COMMENT '演出点位ID',
    perform_date    DATE            NOT NULL COMMENT '演出日期',
    time_slot_start TIME            NOT NULL COMMENT '演出开始时间',
    time_slot_end   TIME            NOT NULL COMMENT '演出结束时间',
    perform_type    VARCHAR(50)     NOT NULL COMMENT '演出类型（歌唱/乐器/舞蹈等）',
    perform_content TEXT            DEFAULT NULL COMMENT '演出曲目/内容简介',
    expected_audience INT           DEFAULT NULL COMMENT '预计观众人数',
    equipment       VARCHAR(255)    DEFAULT NULL COMMENT '演出器材说明',
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '审批状态：0-待一审 1-待终审 2-通过 3-驳回 4-撤销',
    reject_reason   TEXT            DEFAULT NULL COMMENT '驳回原因',
    cert_id         BIGINT          DEFAULT NULL COMMENT '关联电子准演证ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_venue_id (venue_id),
    KEY idx_perform_date (perform_date),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='报备订单表';
```

#### 5.2.4 审批记录表 `t_approval_record`

```sql
CREATE TABLE t_approval_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '报备订单ID',
    approver_id     BIGINT          NOT NULL COMMENT '审批人ID',
    action          TINYINT         NOT NULL COMMENT '审批动作：1-一审通过 2-一审驳回 3-终审通过 4-终审驳回',
    comment         TEXT            DEFAULT NULL COMMENT '审批意见',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_approver_id (approver_id)
) ENGINE=InnoDB COMMENT='审批记录表';
```

#### 5.2.5 电子准演证表 `t_certificate`

```sql
CREATE TABLE t_certificate (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    cert_code       VARCHAR(64)     NOT NULL COMMENT '证件唯一码（用于二维码）',
    order_id        BIGINT          NOT NULL COMMENT '关联报备订单ID',
    user_id         BIGINT          NOT NULL COMMENT '艺人ID',
    venue_id        BIGINT          NOT NULL COMMENT '点位ID',
    perform_date    DATE            NOT NULL COMMENT '演出日期',
    time_slot_start TIME            NOT NULL COMMENT '演出开始时间',
    time_slot_end   TIME            NOT NULL COMMENT '演出结束时间',
    cert_url        VARCHAR(255)    DEFAULT NULL COMMENT '证件文件PDF/图片URL',
    valid_status    TINYINT         NOT NULL DEFAULT 1 COMMENT '有效状态：0-已失效 1-有效',
    issue_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cert_code (cert_code),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_user_id (user_id),
    KEY idx_perform_date (perform_date)
) ENGINE=InnoDB COMMENT='电子准演证表';
```

#### 5.2.6 信用日志表 `t_credit_log`

```sql
CREATE TABLE t_credit_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT          NOT NULL COMMENT '艺人用户ID',
    delta           INT             NOT NULL COMMENT '变化值（正数加分，负数扣分）',
    score_after     INT             NOT NULL COMMENT '变化后信用分',
    event_type      TINYINT         NOT NULL COMMENT '事件类型：1-按时演出 2-迟到 3-超时 4-扰民 5-爽约 6-系统奖励',
    order_id        BIGINT          DEFAULT NULL COMMENT '关联报备订单ID',
    operator_id     BIGINT          DEFAULT NULL COMMENT '操作人ID（管理员）',
    remark          VARCHAR(255)    DEFAULT NULL COMMENT '备注说明',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_order_id (order_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='信用日志表';
```

### 5.3 信用分规则

| 事件 | 分值变化 | 说明 |
|------|----------|------|
| 按时完成演出 | +2 | 系统自动加分 |
| 迟到 15 分钟以内 | -5 | 管理员核验记录 |
| 迟到超 15 分钟 | -10 | 管理员核验记录 |
| 超时演出 | -8 | 管理员核验记录 |
| 扰民投诉成立 | -15 | 管理员处理后记录 |
| 爽约（无故不演） | -20 | 管理员核验记录 |
| 系统奖励（月度优秀） | +10 | 超管手动操作 |

> 信用分低于 60 分：禁止预约热门点位；低于 40 分：账号暂停使用

---

## 6. 核心算法设计

### 6.1 时空冲突检测算法

**触发时机**：艺人提交报备前，后端进行冲突预检（接口：`POST /api/report/pre-check`）

**冲突判定条件**（满足其一即判定冲突）：

1. **同点位时间重叠**：同一 `venue_id`，演出时间段与已通过的报备时间段存在重叠
2. **邻近点位时间重叠**：距离目标点位 **100 米范围内**的其他点位，在相同时间段内已有报备通过

```
冲突检测伪代码：

function preCheckConflict(venueId, date, startTime, endTime):

    # 1. 同点位冲突检测
    conflicts = query t_report_order WHERE:
        venue_id = venueId
        AND perform_date = date
        AND status IN (1, 2)  -- 待终审或已通过
        AND NOT (time_slot_end <= startTime OR time_slot_start >= endTime)

    IF conflicts NOT EMPTY → 返回冲突信息

    # 2. 邻近点位冲突检测（Haversine 距离公式）
    nearbyVenues = query t_venue WHERE:
        distance(longitude, latitude, targetLng, targetLat) <= 100m
        AND id != venueId

    nearbyConflicts = query t_report_order WHERE:
        venue_id IN nearbyVenues
        AND perform_date = date
        AND status IN (1, 2)
        AND NOT (time_slot_end <= startTime OR time_slot_start >= endTime)

    IF nearbyConflicts NOT EMPTY → 返回邻近冲突信息

    RETURN 无冲突
```

**距离计算（Haversine 公式 SQL 实现）**：

```sql
SELECT id, name,
  (6371000 * ACOS(
    COS(RADIANS(:lat)) * COS(RADIANS(latitude)) *
    COS(RADIANS(longitude) - RADIANS(:lng)) +
    SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
  )) AS distance
FROM t_venue
HAVING distance <= 100
ORDER BY distance;
```

### 6.2 并发抢约防超卖（Redis 分布式锁）

```
艺人提交报备
    │
    ▼
尝试获取 Redis 锁
Key: lock:report:{venueId}:{date}:{timeSlot}
SETNX，TTL=10s
    │
    ├── 获取失败 → 返回"当前点位该时段热度较高，请稍后重试"
    │
    └── 获取成功
            │
            ▼
        再次执行冲突检测（Double Check）
            │
            ├── 有冲突 → 释放锁，返回冲突信息
            │
            └── 无冲突 → 写入数据库 → 释放锁 → 返回成功
```

---

## 7. 接口设计规范

### 7.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1743523200000
}
```

### 7.2 核心接口列表

#### 认证模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 艺人注册 |
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/logout` | 退出登录 |
| POST | `/api/auth/qualification` | 上传演艺资质 |

#### 点位模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/venue/map-list` | 获取地图点位列表 |
| GET | `/api/venue/{id}` | 获取点位详情 |
| GET | `/api/venue/{id}/schedule` | 获取点位预约日历 |
| POST | `/api/admin/venue` | 新增点位（管理员） |
| PUT | `/api/admin/venue/{id}` | 修改点位（管理员） |
| DELETE | `/api/admin/venue/{id}` | 删除点位（管理员） |

#### 报备模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/report/pre-check` | 时空冲突预检 |
| POST | `/api/report/submit` | 提交报备申请 |
| GET | `/api/report/my-list` | 我的报备列表 |
| GET | `/api/report/{id}` | 报备详情 |
| DELETE | `/api/report/{id}` | 撤销报备 |

#### 审批模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/approval/list` | 待审列表 |
| POST | `/api/admin/approval/{orderId}/approve` | 审批通过 |
| POST | `/api/admin/approval/{orderId}/reject` | 审批驳回 |

#### 证件模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/certificate/{certCode}` | 扫码核验（公开接口） |
| GET | `/api/certificate/{certCode}/download` | 下载证件 |

#### 信用模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/artist/{userId}/credit` | 查看艺人信用详情 |
| POST | `/api/admin/credit/deduct` | 信用扣分 |
| GET | `/api/admin/credit/logs` | 信用日志列表 |

#### 统计模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/stats/heatmap` | 演出热力图数据 |
| GET | `/api/admin/stats/venue-rate` | 点位预约率统计 |
| GET | `/api/admin/stats/artist-rank` | 艺人活跃度排行 |
| GET | `/api/admin/stats/overview` | 大屏概览数据 |

---

## 8. 安全设计

### 8.1 认证与授权

- 使用 **Sa-Token** 实现 JWT 无状态鉴权，Token 有效期 7 天
- 接口通过注解 `@SaCheckRole` 实现角色鉴权
- 敏感接口（扣分、审批）记录操作日志

### 8.2 数据安全

- 身份证号使用 **AES-256** 加密存储
- 密码使用 **BCrypt** 哈希存储（不可逆）
- 电子准演证二维码内容为签名后的 `certCode`，防伪造

### 8.3 接口安全

- 所有写操作接口需要 Token 鉴权
- 文件上传限制类型（jpg/png/pdf）和大小（最大 10MB）
- 防止 SQL 注入：MyBatis Plus 参数化查询
- XSS 防护：前端 v-html 禁用，后端输入过滤

---

## 9. 部署架构

```
开发环境（本地）
├── 前端：Vite dev server  localhost:5173
├── 后端：Spring Boot      localhost:8080
├── MySQL：                localhost:3306
└── Redis：                localhost:6379

生产环境（单机部署）
├── Nginx（:80/:443）→ 反向代理前端静态资源 + 后端 API
├── Spring Boot JAR（:8080）
├── MySQL 8.0（:3306）
└── Redis（:6379）
```

---

## 10. 目录结构

```
pdk/
├── frontend/              # Vue 3 前端项目
├── backend/               # Spring Boot 后端项目
├── database/              # 数据库脚本
│   ├── schema.sql         # 建表 DDL
│   ├── init_data.sql      # 初始化数据（点位、管理员账号）
│   └── indexes.sql        # 索引优化脚本
├── word/                  # 需求文档
└── ARCHITECTURE.md        # 本文档
```

---

*本文档依据毕业设计任务书及开题报告整理，架构设计以实现核心功能为优先，兼顾可扩展性与实现难度。*
