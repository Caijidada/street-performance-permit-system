-- =========================================
-- 索引优化脚本（建表脚本已包含基础索引，此处补充复合索引）
-- =========================================
USE pdk_db;

-- 报备订单：按用户+状态查询优化
ALTER TABLE t_report_order ADD INDEX idx_user_status (user_id, status);

-- 报备订单：时空冲突检测优化（点位+日期+状态）
ALTER TABLE t_report_order ADD INDEX idx_venue_date_status (venue_id, perform_date, status);

-- 信用日志：按用户+时间查询优化
ALTER TABLE t_credit_log ADD INDEX idx_user_time (user_id, create_time);

-- 证件：按日期查询（用于当日有效证件核查）
ALTER TABLE t_certificate ADD INDEX idx_date_status (perform_date, valid_status);
