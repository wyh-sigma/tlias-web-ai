-- 基础数据：新环境初始化后可直接登录；已有数据时不会覆盖
INSERT IGNORE INTO `dept` (`id`, `name`, `create_time`, `update_time`) VALUES
  (1, '教研部', NOW(), NOW()),
  (2, '学工部', NOW(), NOW()),
  (3, '咨询部', NOW(), NOW()),
  (4, '就业部', NOW(), NOW()),
  (5, '人事部', NOW(), NOW()),
  (6, '行政部', NOW(), NOW());

-- admin / 123456，BCrypt 哈希
INSERT IGNORE INTO `emp`
  (`username`, `password`, `role`, `name`, `gender`, `phone`, `job`, `salary`, `entry_date`, `dept_id`, `create_time`, `update_time`)
VALUES
  ('admin', '$2a$10$ZeCKCm.YfE0piqseQ4I/3.xA7XT7SSQ31AlrfjydjTqkrL5EBqGl.', 'ADMIN',
   '管理员', 1, '13800000000', 4, 10000, CURRENT_DATE, 1, NOW(), NOW());
