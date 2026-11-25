-- BCrypt 密码哈希长度为 60，原 varchar(32) 会截断导致登录失败
ALTER TABLE `emp`
  MODIFY COLUMN `password` varchar(100) DEFAULT NULL COMMENT '密码';

-- 基于角色的访问控制
ALTER TABLE `emp`
  ADD COLUMN `role` varchar(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER' AFTER `password`;
