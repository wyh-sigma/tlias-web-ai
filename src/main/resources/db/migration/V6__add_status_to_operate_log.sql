-- 操作日志表增加 status 字段，区分操作成功/失败
-- 1: 成功, 0: 失败
ALTER TABLE `operate_log`
    ADD COLUMN `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '操作状态, 1:成功, 0:失败' AFTER `return_value`;
