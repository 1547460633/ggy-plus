-- 微信小程序登录绑定关系唯一约束。
-- 执行前请确认 sys_social.auth_id 中不存在重复值；本文件未自动执行。
ALTER TABLE sys_social
    ADD UNIQUE KEY uk_sys_social_auth_id (auth_id);
