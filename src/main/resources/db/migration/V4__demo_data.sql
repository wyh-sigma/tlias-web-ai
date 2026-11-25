-- 演示数据：用于报表、结课预警、违纪排行等页面展示与压测
-- 使用 INSERT IGNORE，重复执行不会产生重复数据

-- 1) 补充员工：班主任（job=1）与讲师（job=2），密码均为 123456
INSERT IGNORE INTO `emp`
  (`username`, `password`, `role`, `name`, `gender`, `phone`, `job`, `salary`, `entry_date`, `dept_id`, `create_time`, `update_time`)
VALUES
  ('teacher_wang', '$2a$10$ZeCKCm.YfE0piqseQ4I/3.xA7XT7SSQ31AlrfjydjTqkrL5EBqGl.', 'USER', '王敏', 2, '13900000001', 1, 8200, '2022-03-01', 2, NOW(), NOW()),
  ('teacher_li',   '$2a$10$ZeCKCm.YfE0piqseQ4I/3.xA7XT7SSQ31AlrfjydjTqkrL5EBqGl.', 'USER', '李强', 1, '13900000002', 1, 8000, '2022-07-15', 2, NOW(), NOW()),
  ('teacher_zhao', '$2a$10$ZeCKCm.YfE0piqseQ4I/3.xA7XT7SSQ31AlrfjydjTqkrL5EBqGl.', 'USER', '赵雪', 2, '13900000003', 1, 8100, '2023-01-10', 2, NOW(), NOW()),
  ('lecturer_chen','$2a$10$ZeCKCm.YfE0piqseQ4I/3.xA7XT7SSQ31AlrfjydjTqkrL5EBqGl.', 'USER', '陈鹏', 1, '13900000004', 2, 9500, '2021-09-01', 1, NOW(), NOW()),
  ('lecturer_liu', '$2a$10$ZeCKCm.YfE0piqseQ4I/3.xA7XT7SSQ31AlrfjydjTqkrL5EBqGl.', 'USER', '刘洋', 1, '13900000005', 2, 9300, '2022-11-20', 1, NOW(), NOW());

-- 2) 班级：包含 30 天内结课、已结课、长期班级
INSERT IGNORE INTO `clazz` (`name`, `room`, `begin_date`, `end_date`, `master_id`, `subject`, `create_time`, `update_time`)
VALUES
  ('Java就业69期', 'A101', DATE_SUB(CURDATE(), INTERVAL 170 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY),
   (SELECT id FROM (SELECT id FROM emp WHERE username = 'teacher_wang') t), 1, NOW(), NOW()),
  ('Java就业70期', 'A102', DATE_SUB(CURDATE(), INTERVAL 150 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY),
   (SELECT id FROM (SELECT id FROM emp WHERE username = 'teacher_li') t), 1, NOW(), NOW()),
  ('前端就业01期', 'B201', DATE_SUB(CURDATE(), INTERVAL 200 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY),
   (SELECT id FROM (SELECT id FROM emp WHERE username = 'teacher_zhao') t), 2, NOW(), NOW()),
  ('大数据02期', 'C301', DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 160 DAY),
   (SELECT id FROM (SELECT id FROM emp WHERE username = 'teacher_wang') t), 3, NOW(), NOW());

-- 3) 学生：分布到不同班级，覆盖违纪次数/扣分
INSERT IGNORE INTO `student`
  (`name`, `no`, `gender`, `phone`, `id_card`, `is_college`, `address`, `degree`, `graduation_date`, `clazz_id`, `violation_count`, `violation_score`, `create_time`, `update_time`)
VALUES
  ('张三', 'S202600001', 1, '13700000001', '110101200001010011', 1, '北京市海淀区', 4, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = 'Java就业69期') t), 3, 8, NOW(), NOW()),
  ('李四', 'S202600002', 2, '13700000002', '110101200001010022', 1, '北京市朝阳区', 4, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = 'Java就业69期') t), 2, 5, NOW(), NOW()),
  ('王五', 'S202600003', 1, '13700000003', '110101200001010033', 0, '河北省石家庄市', 3, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = 'Java就业69期') t), 1, 2, NOW(), NOW()),
  ('赵六', 'S202600004', 2, '13700000004', '110101200001010044', 1, '上海市浦东新区', 5, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = 'Java就业69期') t), 0, 0, NOW(), NOW()),
  ('孙七', 'S202600005', 1, '13700000005', '110101200001010055', 0, '山东省济南市', 4, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = 'Java就业70期') t), 4, 12, NOW(), NOW()),
  ('周八', 'S202600006', 2, '13700000006', '110101200001010066', 1, '江苏省南京市', 4, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = 'Java就业70期') t), 1, 3, NOW(), NOW()),
  ('吴九', 'S202600007', 1, '13700000007', '110101200001010077', 1, '浙江省杭州市', 3, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = 'Java就业70期') t), 0, 0, NOW(), NOW()),
  ('郑十', 'S202600008', 2, '13700000008', '110101200001010088', 0, '四川省成都市', 4, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = '前端就业01期') t), 2, 6, NOW(), NOW()),
  ('冯十一', 'S202600009', 1, '13700000009', '110101200001010099', 1, '广东省广州市', 4, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = '前端就业01期') t), 1, 2, NOW(), NOW()),
  ('陈十二', 'S202600010', 2, '13700000010', '110101200001010100', 1, '湖北省武汉市', 5, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = '前端就业01期') t), 0, 0, NOW(), NOW()),
  ('褚十三', 'S202600011', 1, '13700000011', '110101200001010111', 0, '陕西省西安市', 3, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = '大数据02期') t), 3, 7, NOW(), NOW()),
  ('卫十四', 'S202600012', 2, '13700000012', '110101200001010122', 1, '福建省厦门市', 4, '2025-06-30',
   (SELECT id FROM (SELECT id FROM clazz WHERE name = '大数据02期') t), 0, 0, NOW(), NOW());
