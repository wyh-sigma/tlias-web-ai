-- 批量演示数据：把数据量提升到可做分页、报表与压测的规模
-- 目标：emp 约 100、clazz 50、student 5000~10000（本脚本新增 8000）
-- 使用确定性取模生成，字段唯一值均避开 V3/V4 已有数据

-- 递归 CTE 默认上限 1000，这里需要生成 8000 行
SET SESSION cte_max_recursion_depth = 20000;

-- 1) 员工：再补 94 个，配合已有 6 个共 100 个
--    用户名 emp_demo_1..94、手机号 13800000001..94，均不会与 admin(13800000000)、V4 员工冲突
INSERT IGNORE INTO `emp`
  (`username`, `password`, `role`, `name`, `gender`, `phone`, `job`, `salary`, `entry_date`, `dept_id`, `create_time`, `update_time`)
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 94
)
SELECT
  CONCAT('emp_demo_', n),
  '$2a$10$ZeCKCm.YfE0piqseQ4I/3.xA7XT7SSQ31AlrfjydjTqkrL5EBqGl.',
  'USER',
  CONCAT('演示员工', n),
  (n % 2) + 1,
  CONCAT('138', LPAD(n, 8, '0')),
  (n % 5) + 1,
  6000 + (n % 40) * 100,
  DATE_SUB(CURDATE(), INTERVAL (n * 7 % 900) DAY),
  (n % 6) + 1,
  NOW(), NOW()
FROM seq;

-- 2) 班级：再补 46 个，配合已有 4 个共 50 个
--    结课时间按 5 天步长分布，保证既有 30 天内预警班，也有已结课/长期班
INSERT IGNORE INTO `clazz`
  (`name`, `room`, `begin_date`, `end_date`, `master_id`, `subject`, `create_time`, `update_time`)
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 46
),
masters AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn, COUNT(*) OVER () AS cnt
  FROM emp WHERE job = 1
)
SELECT
  CONCAT('演示班级', LPAD(n, 3, '0')),
  CONCAT('D', LPAD(n, 3, '0')),
  DATE_SUB(CURDATE(), INTERVAL (30 + n) DAY),
  DATE_ADD(CURDATE(), INTERVAL (((n % 12) * 5) - 20) DAY),
  m.id,
  (n % 6) + 1,
  NOW(), NOW()
FROM seq
JOIN masters m ON m.rn = ((seq.n - 1) % m.cnt) + 1;

-- 3) 学生：新增 8000 个，配合已有 12 个共 8012 个
--    学号 SD00000001..、手机号 13600000001..、身份证 1101012000 + 8 位序号，均唯一
--    违纪数据按取模分布：约 1/20 严重违纪，用于违纪排行与高危清单
INSERT IGNORE INTO `student`
  (`name`, `no`, `gender`, `phone`, `id_card`, `is_college`, `address`, `degree`,
   `graduation_date`, `clazz_id`, `violation_count`, `violation_score`, `create_time`, `update_time`)
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 8000
),
clazzes AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn, COUNT(*) OVER () AS cnt
  FROM clazz
)
SELECT
  CONCAT('学生', n),
  CONCAT('SD', LPAD(n, 8, '0')),
  (n % 2) + 1,
  CONCAT('136', LPAD(n, 8, '0')),
  CONCAT('1101012000', LPAD(n, 8, '0')),
  n % 2,
  CONCAT('演示地址', n, '号'),
  (n % 6) + 1,
  '2025-06-30',
  c.id,
  CASE WHEN n % 20 = 0 THEN 4 WHEN n % 7 = 0 THEN 2 WHEN n % 3 = 0 THEN 1 ELSE 0 END,
  CASE WHEN n % 20 = 0 THEN 12 WHEN n % 7 = 0 THEN 5 WHEN n % 3 = 0 THEN 2 ELSE 0 END,
  NOW(), NOW()
FROM seq
JOIN clazzes c ON c.rn = ((seq.n - 1) % c.cnt) + 1;
