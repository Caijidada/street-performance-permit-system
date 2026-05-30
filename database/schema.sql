-- =========================================
-- 街头演出点位报备系统 数据库建表脚本
-- 数据库：MySQL 8.0
-- 字符集：utf8mb4
-- =========================================

CREATE DATABASE IF NOT EXISTS pdk_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pdk_db;

-- -------------------------------------------
-- 用户表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username        VARCHAR(50)     NOT NULL COMMENT '登录用户名',
    password        VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(50)     NOT NULL COMMENT '真实姓名',
    id_card         VARCHAR(255)    NOT NULL COMMENT '身份证号（AES加密存储）',
    phone           VARCHAR(11)     NOT NULL COMMENT '手机号',
    avatar          VARCHAR(255)    DEFAULT NULL COMMENT '头像URL',
    role            TINYINT         NOT NULL DEFAULT 1 COMMENT '角色：1-艺人 2-管理员 3-超管',
    credit_score    INT             NOT NULL DEFAULT 100 COMMENT '信用分（初始100）',
    qualification   VARCHAR(255)    DEFAULT NULL COMMENT '演艺资质证明文件URL',
    qualify_status  TINYINT         NOT NULL DEFAULT 0 COMMENT '资质审核：0-待审 1-通过 2-拒绝',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '账号状态：0-禁用 1-正常',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -------------------------------------------
-- 演出点位表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS t_venue (
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
    images          TEXT            DEFAULT NULL COMMENT '点位图片URL列表（JSON数组）',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '点位状态：0-关闭 1-开放',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_district (district),
    KEY idx_status (status),
    KEY idx_location (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出点位表';

-- -------------------------------------------
-- 报备订单表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS t_report_order (
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
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-待一审 1-待终审 2-通过 3-驳回 4-撤销',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报备订单表';

-- -------------------------------------------
-- 审批记录表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS t_approval_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '报备订单ID',
    approver_id     BIGINT          NOT NULL COMMENT '审批人ID',
    action          TINYINT         NOT NULL COMMENT '动作：1-一审通过 2-一审驳回 3-终审通过 4-终审驳回',
    comment         TEXT            DEFAULT NULL COMMENT '审批意见',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_approver_id (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

-- -------------------------------------------
-- 电子准演证表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS t_certificate (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    cert_code       VARCHAR(64)     NOT NULL COMMENT '证件唯一码（二维码内容）',
    order_id        BIGINT          NOT NULL COMMENT '关联报备订单ID',
    user_id         BIGINT          NOT NULL COMMENT '艺人ID',
    venue_id        BIGINT          NOT NULL COMMENT '点位ID',
    perform_date    DATE            NOT NULL COMMENT '演出日期',
    time_slot_start TIME            NOT NULL COMMENT '演出开始时间',
    time_slot_end   TIME            NOT NULL COMMENT '演出结束时间',
    cert_url        VARCHAR(255)    DEFAULT NULL COMMENT '证件图片URL',
    valid_status    TINYINT         NOT NULL DEFAULT 1 COMMENT '有效状态：0-已失效 1-有效',
    issue_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cert_code (cert_code),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_user_id (user_id),
    KEY idx_perform_date (perform_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子准演证表';

-- -------------------------------------------
-- 信用日志表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS t_credit_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT          NOT NULL COMMENT '艺人用户ID',
    delta           INT             NOT NULL COMMENT '变化值（正数加分，负数扣分）',
    score_after     INT             NOT NULL COMMENT '变化后信用分',
    event_type      TINYINT         NOT NULL COMMENT '事件：1-按时演出 2-迟到 3-超时 4-扰民 5-爽约 6-系统奖励',
    order_id        BIGINT          DEFAULT NULL COMMENT '关联报备订单ID',
    operator_id     BIGINT          DEFAULT NULL COMMENT '操作人ID（管理员）',
    remark          VARCHAR(255)    DEFAULT NULL COMMENT '备注说明',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_order_id (order_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用日志表';

-- -------------------------------------------
-- 消息通知表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS t_notice (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT          NOT NULL COMMENT '接收用户ID',
    title       VARCHAR(100)    NOT NULL COMMENT '标题',
    content     VARCHAR(500)    NOT NULL COMMENT '内容',
    type        TINYINT         NOT NULL DEFAULT 1 COMMENT '类型：1-审批通知 2-信用通知 3-系统通知',
    is_read     TINYINT         NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表';
