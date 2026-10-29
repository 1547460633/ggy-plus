-- IoT 远程点灯 MVP。仅新增对象；请先在目标库备份后由发布流程执行，禁止直接在开发库执行。
CREATE TABLE IF NOT EXISTS iot_device (
  device_id bigint NOT NULL COMMENT '设备ID',
  device_key varchar(64) NOT NULL COMMENT '设备标识',
  device_secret_hash varchar(100) NOT NULL COMMENT 'BCrypt 密钥摘要',
  device_name varchar(100) NOT NULL COMMENT '设备名称',
  product_type varchar(32) NOT NULL DEFAULT 'ESP32_LIGHT' COMMENT '产品类型',
  enabled char(1) NOT NULL DEFAULT '0' COMMENT '启用状态（0正常 1停用）',
  online_status char(1) NOT NULL DEFAULT '0' COMMENT '在线状态（0离线 1在线）',
  light_state char(1) NOT NULL DEFAULT '0' COMMENT '灯状态（0关 1开 U未知）',
  last_seen_time datetime DEFAULT NULL COMMENT '最后心跳时间',
  last_report_time datetime DEFAULT NULL COMMENT '最后状态上报时间',
  last_offline_reason varchar(255) DEFAULT NULL COMMENT '最近离线原因',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  create_dept bigint DEFAULT NULL, create_by bigint DEFAULT NULL, create_time datetime DEFAULT NULL,
  update_by bigint DEFAULT NULL, update_time datetime DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (device_id), UNIQUE KEY uk_iot_device_key (device_key),
  KEY idx_iot_device_online_seen (online_status, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT 设备';

CREATE TABLE IF NOT EXISTS iot_device_command (
  command_id bigint NOT NULL COMMENT '指令ID', device_id bigint NOT NULL COMMENT '设备ID',
  request_id varchar(64) NOT NULL COMMENT '请求关联ID', command_type varchar(32) NOT NULL COMMENT '指令类型',
  desired_light_state char(1) NOT NULL COMMENT '期望灯状态（0关 1开）', command_status varchar(16) NOT NULL COMMENT '状态',
  failure_reason varchar(255) DEFAULT NULL COMMENT '失败原因', published_time datetime DEFAULT NULL COMMENT '发送时间',
  ack_time datetime DEFAULT NULL COMMENT '回执时间', finished_time datetime DEFAULT NULL COMMENT '完成时间',
  timeout_time datetime DEFAULT NULL COMMENT '超时时间', create_dept bigint DEFAULT NULL, create_by bigint DEFAULT NULL,
  create_time datetime DEFAULT NULL, update_by bigint DEFAULT NULL, update_time datetime DEFAULT NULL,
  PRIMARY KEY (command_id), UNIQUE KEY uk_iot_command_device_request (device_id, request_id),
  KEY idx_iot_command_device_status (device_id, command_status), KEY idx_iot_command_timeout (command_status, timeout_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT 设备指令';

CREATE TABLE IF NOT EXISTS iot_device_report (
  report_id bigint NOT NULL COMMENT '上报ID', device_id bigint NOT NULL COMMENT '设备ID',
  request_id varchar(64) DEFAULT NULL COMMENT '关联指令请求ID', message_id varchar(64) NOT NULL COMMENT '设备消息幂等ID', report_type varchar(32) NOT NULL COMMENT '上报类型',
  event_time datetime NOT NULL COMMENT '设备事件时间', received_time datetime NOT NULL COMMENT '平台接收时间',
  light_state char(1) DEFAULT NULL COMMENT '灯状态（0关 1开 U未知）', payload text NOT NULL COMMENT '原始报文',
  PRIMARY KEY (report_id), UNIQUE KEY uk_iot_report_device_message (device_id, message_id), KEY idx_iot_report_device_time (device_id, received_time),
  KEY idx_iot_report_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT 设备上报记录';

-- 菜单 ID 保持在 IoT 专属区间。管理员角色（role_id=1）仅补充新增菜单关联，不修改任何既有授权。
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_time, remark)
VALUES
  (1700, '物联网设备', 0, 8, 'iot', NULL, '', 1, 0, 'M', '0', '0', '', 'dashboard', NOW(), 'IoT 设备管理目录'),
  (1701, '设备管理', 1700, 1, 'device', 'iot/device/index', '', 1, 0, 'C', '0', '0', 'iot:device:list', 'cpu', NOW(), 'IoT 设备管理'),
  (1702, '设备查询', 1701, 1, '#', '', '', 1, 0, 'F', '0', '0', 'iot:device:query', '#', NOW(), ''),
  (1703, '设备新增', 1701, 2, '#', '', '', 1, 0, 'F', '0', '0', 'iot:device:add', '#', NOW(), ''),
  (1704, '设备修改', 1701, 3, '#', '', '', 1, 0, 'F', '0', '0', 'iot:device:edit', '#', NOW(), ''),
  (1705, '凭证轮换', 1701, 4, '#', '', '', 1, 0, 'F', '0', '0', 'iot:device:credential', '#', NOW(), ''),
  (1706, '远程控制', 1701, 5, '#', '', '', 1, 0, 'F', '0', '0', 'iot:device:control', '#', NOW(), ''),
  (1707, '指令记录', 1700, 2, 'command', 'iot/command/index', '', 1, 0, 'C', '0', '0', 'iot:command:list', 'list', NOW(), 'IoT 指令记录'),
  (1708, '指令查询', 1707, 1, '#', '', '', 1, 0, 'F', '0', '0', 'iot:command:query', '#', NOW(), '')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), perms=VALUES(perms), update_time=NOW();

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 1700 AND 1708;

-- 回滚：先删除 sys_role_menu 中 menu_id 1700-1708 的关联，再删除 sys_menu 1700-1708，最后依次 DROP TABLE iot_device_report、iot_device_command、iot_device。
