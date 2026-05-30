-- =========================================
-- 模拟数据脚本（已校正字段名与状态值）
-- t_report_order 状态: 0=待一审 1=待终审 2=通过 3=驳回 4=撤销
-- t_approval_record action: 1=一审通过 2=一审驳回 3=终审通过 4=终审驳回
-- =========================================
USE pdk_db;

-- =========================================
-- 模拟艺人账号（密码：artist123456）
-- =========================================
INSERT INTO t_user (username, password, real_name, id_card, phone, role, credit_score, qualify_status, status) VALUES
('artist_zhang', '$2b$10$/EdBE1dF28rwgPoa0ouM8OAk0gDt1/uLHAZCT7bAmKzKzcO/bKhjC', '张伟', 'ENC_001', '13711111001', 1, 98, 1, 1),
('artist_li',    '$2b$10$/EdBE1dF28rwgPoa0ouM8OAk0gDt1/uLHAZCT7bAmKzKzcO/bKhjC', '李娜', 'ENC_002', '13711111002', 1, 92, 1, 1),
('artist_wang',  '$2b$10$/EdBE1dF28rwgPoa0ouM8OAk0gDt1/uLHAZCT7bAmKzKzcO/bKhjC', '王芳', 'ENC_003', '13711111003', 1, 85, 1, 1),
('artist_liu',   '$2b$10$/EdBE1dF28rwgPoa0ouM8OAk0gDt1/uLHAZCT7bAmKzKzcO/bKhjC', '刘洋', 'ENC_004', '13711111004', 1, 76, 1, 1),
('artist_chen',  '$2b$10$/EdBE1dF28rwgPoa0ouM8OAk0gDt1/uLHAZCT7bAmKzKzcO/bKhjC', '陈晨', 'ENC_005', '13711111005', 1, 70, 1, 1),
('artist_zhao',  '$2b$10$/EdBE1dF28rwgPoa0ouM8OAk0gDt1/uLHAZCT7bAmKzKzcO/bKhjC', '赵磊', 'ENC_006', '13711111006', 1, 62, 0, 1),
('artist_sun',   '$2b$10$/EdBE1dF28rwgPoa0ouM8OAk0gDt1/uLHAZCT7bAmKzKzcO/bKhjC', '孙丽', 'ENC_007', '13711111007', 1, 55, 1, 1),
('artist_zhou',  '$2b$10$/EdBE1dF28rwgPoa0ouM8OAk0gDt1/uLHAZCT7bAmKzKzcO/bKhjC', '周杰', 'ENC_008', '13711111008', 1, 88, 1, 1);

-- =========================================
-- 模拟报备订单（字段名已校正）
-- =========================================
INSERT INTO t_report_order
  (order_no, user_id, venue_id, perform_date, time_slot_start, time_slot_end,
   perform_type, perform_content, expected_audience, status, create_time)
SELECT
  CONCAT('ORD', DATE_FORMAT(NOW(), '%Y%m%d'), LPAD(d.seq, 4, '0')),
  u.id,
  d.venue_id,
  d.perform_date,
  d.ts, d.te,
  d.ptype, d.content, d.audience, d.status,
  DATE_SUB(NOW(), INTERVAL d.ago DAY)
FROM (
  SELECT 1  seq, '张伟' uname, 1 venue_id, DATE_SUB(CURDATE(),INTERVAL 30 DAY) perform_date, '14:00:00' ts,'16:00:00' te,'歌唱'  ptype,'民谣吉他弹唱专场'     content, 50  audience, 2 status, 30 ago UNION ALL
  SELECT 2,  '张伟', 2, DATE_SUB(CURDATE(),INTERVAL 25 DAY),'15:00:00','17:00:00','乐器','古典吉他独奏',        40, 2, 25 UNION ALL
  SELECT 3,  '张伟', 3, DATE_SUB(CURDATE(),INTERVAL 20 DAY),'19:00:00','21:00:00','歌唱','流行歌曲演唱会',      60, 2, 20 UNION ALL
  SELECT 4,  '张伟', 1, DATE_SUB(CURDATE(),INTERVAL 15 DAY),'10:00:00','12:00:00','歌唱','民谣专场第二季',      55, 2, 15 UNION ALL
  SELECT 5,  '张伟', 5, DATE_SUB(CURDATE(),INTERVAL 10 DAY),'14:00:00','16:00:00','乐器','轻音乐演奏',          30, 2, 10 UNION ALL
  SELECT 6,  '李娜', 1, DATE_SUB(CURDATE(),INTERVAL 28 DAY),'10:00:00','12:00:00','舞蹈','街舞表演秀',          80, 2, 28 UNION ALL
  SELECT 7,  '李娜', 2, DATE_SUB(CURDATE(),INTERVAL 22 DAY),'16:00:00','18:00:00','舞蹈','爵士舞专场',          70, 2, 22 UNION ALL
  SELECT 8,  '李娜', 3, DATE_SUB(CURDATE(),INTERVAL 18 DAY),'19:00:00','21:00:00','歌唱','流行歌曲演唱',        60, 2, 18 UNION ALL
  SELECT 9,  '李娜', 4, DATE_SUB(CURDATE(),INTERVAL 12 DAY),'10:00:00','12:00:00','舞蹈','现代舞汇报演出',      50, 2, 12 UNION ALL
  SELECT 10, '王芳', 2, DATE_SUB(CURDATE(),INTERVAL 26 DAY),'16:00:00','18:00:00','舞蹈','现代舞表演',          70, 2, 26 UNION ALL
  SELECT 11, '王芳', 4, DATE_SUB(CURDATE(),INTERVAL 21 DAY),'10:00:00','12:00:00','乐器','二胡独奏',            30, 2, 21 UNION ALL
  SELECT 12, '王芳', 3, DATE_SUB(CURDATE(),INTERVAL 16 DAY),'10:00:00','12:00:00','乐器','笛子独奏',            40, 2, 16 UNION ALL
  SELECT 13, '刘洋', 1, DATE_SUB(CURDATE(),INTERVAL 24 DAY),'13:00:00','15:00:00','魔术','近景魔术表演',        90, 2, 24 UNION ALL
  SELECT 14, '刘洋', 3, DATE_SUB(CURDATE(),INTERVAL 19 DAY),'18:00:00','20:00:00','歌唱','粤语歌专场',          55, 2, 19 UNION ALL
  SELECT 15, '刘洋', 4, DATE_SUB(CURDATE(),INTERVAL 14 DAY),'10:00:00','12:00:00','魔术','儿童魔术秀',          50, 3, 14 UNION ALL
  SELECT 16, '陈晨', 5, DATE_SUB(CURDATE(),INTERVAL 23 DAY),'14:00:00','16:00:00','乐器','钢琴轻音乐',          35, 2, 23 UNION ALL
  SELECT 17, '陈晨', 2, DATE_SUB(CURDATE(),INTERVAL 17 DAY),'11:00:00','13:00:00','杂技','杂技绝活表演',        65, 2, 17 UNION ALL
  SELECT 18, '周杰', 1, DATE_SUB(CURDATE(),INTERVAL 27 DAY),'15:00:00','17:00:00','歌唱','说唱现场秀',         100, 2, 27 UNION ALL
  SELECT 19, '周杰', 3, DATE_SUB(CURDATE(),INTERVAL 22 DAY),'19:00:00','21:00:00','乐器','电子琴表演',          80, 2, 22 UNION ALL
  SELECT 20, '周杰', 2, DATE_SUB(CURDATE(),INTERVAL 16 DAY),'16:00:00','18:00:00','歌唱','嘻哈专场',            80, 2, 16 UNION ALL
  SELECT 21, '周杰', 1, DATE_SUB(CURDATE(),INTERVAL 8 DAY), '15:00:00','17:00:00','歌唱','周末说唱',            90, 2,  8 UNION ALL
  SELECT 22, '张伟', 1, DATE_ADD(CURDATE(),INTERVAL 2 DAY), '14:00:00','16:00:00','歌唱','周末民谣场',          60, 1,  1 UNION ALL
  SELECT 23, '李娜', 2, DATE_ADD(CURDATE(),INTERVAL 3 DAY), '16:00:00','18:00:00','舞蹈','爵士舞专场二',        70, 1,  1 UNION ALL
  SELECT 24, '王芳', 3, DATE_ADD(CURDATE(),INTERVAL 4 DAY), '10:00:00','12:00:00','乐器','笛子独奏第二场',      40, 0,  0 UNION ALL
  SELECT 25, '孙丽', 1, DATE_ADD(CURDATE(),INTERVAL 1 DAY), '13:00:00','15:00:00','歌唱','古风演唱会',          45, 0,  0 UNION ALL
  SELECT 26, '赵磊', 2, DATE_ADD(CURDATE(),INTERVAL 5 DAY), '15:00:00','17:00:00','舞蹈','街舞battle',          60, 0,  0
) d
JOIN t_user u ON u.real_name = d.uname;

-- =========================================
-- 审批记录（一审通过）
-- =========================================
INSERT INTO t_approval_record (order_id, approver_id, action, comment, create_time)
SELECT o.id, a.id, 1, '资料完整，符合要求，一审通过', DATE_ADD(o.create_time, INTERVAL 2 HOUR)
FROM t_report_order o
JOIN t_user a ON a.username = 'admin'
WHERE o.status IN (1, 2);

-- 审批记录（终审通过）
INSERT INTO t_approval_record (order_id, approver_id, action, comment, create_time)
SELECT o.id, a.id, 3, '演出内容合规，终审通过，准予演出', DATE_ADD(o.create_time, INTERVAL 6 HOUR)
FROM t_report_order o
JOIN t_user a ON a.username = 'superadmin'
WHERE o.status = 2;

-- 审批记录（一审驳回）
INSERT INTO t_approval_record (order_id, approver_id, action, comment, create_time)
SELECT o.id, a.id, 2, '演出类型与该场地不符，驳回申请', DATE_ADD(o.create_time, INTERVAL 3 HOUR)
FROM t_report_order o
JOIN t_user a ON a.username = 'admin'
WHERE o.status = 3;

-- =========================================
-- 电子准演证（字段名已校正）
-- =========================================
INSERT INTO t_certificate (cert_code, order_id, user_id, venue_id, perform_date,
  time_slot_start, time_slot_end, cert_url, valid_status, issue_time)
SELECT
  CONCAT('CERT', DATE_FORMAT(o.perform_date,'%Y%m%d'), LPAD(o.id, 5, '0')),
  o.id,
  o.user_id,
  o.venue_id,
  o.perform_date,
  o.time_slot_start,
  o.time_slot_end,
  CONCAT('/files/cert/CERT', DATE_FORMAT(o.perform_date,'%Y%m%d'), LPAD(o.id,5,'0'), '.png'),
  1,
  DATE_ADD(o.create_time, INTERVAL 6 HOUR)
FROM t_report_order o
WHERE o.status = 2;

-- =========================================
-- 信用日志（字段名已校正）
-- event_type: 1=按时演出 5=爽约 6=系统奖励
-- =========================================
INSERT INTO t_credit_log (user_id, delta, score_after, event_type, remark, create_time)
SELECT u.id,  2,  98, 1, '演出完成，系统自动加分', DATE_SUB(NOW(), INTERVAL 25 DAY) FROM t_user u WHERE u.username='artist_zhang' UNION ALL
SELECT u.id,  2,  90, 1, '演出完成，系统自动加分', DATE_SUB(NOW(), INTERVAL 20 DAY) FROM t_user u WHERE u.username='artist_zhang' UNION ALL
SELECT u.id, -5,  92, 5, '演出爽约扣分',           DATE_SUB(NOW(), INTERVAL 15 DAY) FROM t_user u WHERE u.username='artist_li'    UNION ALL
SELECT u.id,  2,  85, 1, '演出完成，系统自动加分', DATE_SUB(NOW(), INTERVAL 20 DAY) FROM t_user u WHERE u.username='artist_wang'  UNION ALL
SELECT u.id,-10,  76, 4, '收到扰民投诉，扣分处理', DATE_SUB(NOW(), INTERVAL 10 DAY) FROM t_user u WHERE u.username='artist_liu'   UNION ALL
SELECT u.id,  2,  88, 1, '演出完成，系统自动加分', DATE_SUB(NOW(), INTERVAL 8 DAY)  FROM t_user u WHERE u.username='artist_zhou'  UNION ALL
SELECT u.id,  2,  90, 1, '演出完成，系统自动加分', DATE_SUB(NOW(), INTERVAL 5 DAY)  FROM t_user u WHERE u.username='artist_zhou'  UNION ALL
SELECT u.id, 10,  98, 6, '管理员奖励：优秀演出者', DATE_SUB(NOW(), INTERVAL 3 DAY)  FROM t_user u WHERE u.username='artist_zhang' UNION ALL
SELECT u.id, -5,  55, 5, '演出爽约扣分',           DATE_SUB(NOW(), INTERVAL 2 DAY)  FROM t_user u WHERE u.username='artist_sun';
