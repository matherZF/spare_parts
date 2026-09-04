-- ============================================================
-- WMS 仓储管理系统 - MySQL 建库建表脚本
-- 数据库: wms
-- 字符集: utf8mb4
-- 说明:
--   1. 本脚本用于手动初始化数据库结构（DDL）。
--   2. 应用启动时 JPA (ddl-auto=update) 会自动校验/更新表结构。
--   3. 初始种子数据（用户、商品、库位、批次、库存等）由
--      DataInitializer 在应用首次启动时自动写入，无需在此脚本中插入。
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `wms`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `wms`;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_user`;
CREATE TABLE `wms_user` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`     VARCHAR(64)  NOT NULL COMMENT '登录账号',
    `password`     VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `display_name` VARCHAR(64)  DEFAULT NULL COMMENT '显示名称',
    `role`         VARCHAR(16)  NOT NULL COMMENT '角色：ADMIN / OPERATOR',
    `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`   DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 商品表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_product`;
CREATE TABLE `wms_product` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sku`        VARCHAR(64)  NOT NULL COMMENT 'SKU编码',
    `name`       VARCHAR(128) NOT NULL COMMENT '商品名称',
    `spec`       VARCHAR(255) DEFAULT NULL COMMENT '规格',
    `unit`       VARCHAR(32)  DEFAULT NULL COMMENT '单位',
    `created_at` DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_sku` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ------------------------------------------------------------
-- 3. 库位表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_location`;
CREATE TABLE `wms_location` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`       VARCHAR(64)  NOT NULL COMMENT '库位编码',
    `area`       VARCHAR(64)  DEFAULT NULL COMMENT '所属区域',
    `type`       VARCHAR(64)  DEFAULT NULL COMMENT '库位类型',
    `device_no`  VARCHAR(64)  DEFAULT NULL COMMENT '绑定灯光设备编号',
    `remark`     VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_location_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库位表';

-- ------------------------------------------------------------
-- 4. 批次表
--    批次由 item_key、生产日期、保质期、生产厂商等信息共同构成
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_batch`;
CREATE TABLE `wms_batch` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `item_key`         VARCHAR(64)  NOT NULL COMMENT '批次号（唯一）',
    `product_id`       BIGINT       NOT NULL COMMENT '关联商品ID',
    `sku`              VARCHAR(64)  DEFAULT NULL COMMENT 'SKU编码（冗余）',
    `product_name`     VARCHAR(128) DEFAULT NULL COMMENT '商品名称（冗余）',
    `production_date`  DATE         DEFAULT NULL COMMENT '生产日期',
    `shelf_life_days`  INT          DEFAULT NULL COMMENT '保质期（天）',
    `manufacturer`     VARCHAR(128) DEFAULT NULL COMMENT '生产厂商',
    `created_at`       DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_item_key` (`item_key`),
    KEY `idx_batch_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批次表';

-- ------------------------------------------------------------
-- 5. 入库单表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_putaway_order`;
CREATE TABLE `wms_putaway_order` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`     VARCHAR(64)  NOT NULL COMMENT '入库单号',
    `product_id`   BIGINT       NOT NULL COMMENT '关联商品ID',
    `batch_id`     BIGINT       DEFAULT NULL COMMENT '关联批次ID',
    `plan_qty`     INT          NOT NULL COMMENT '计划入库数量',
    `put_qty`      INT          NOT NULL DEFAULT 0 COMMENT '已上架数量',
    `status`       VARCHAR(16)  NOT NULL COMMENT '状态：PENDING / DONE',
    `created_at`   DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_putaway_order_no` (`order_no`),
    KEY `idx_putaway_product` (`product_id`),
    KEY `idx_putaway_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库单表';

-- ------------------------------------------------------------
-- 6. 入库明细表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_putaway_item`;
CREATE TABLE `wms_putaway_item` (
    `id`                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `putaway_order_id`  BIGINT   NOT NULL COMMENT '入库单ID',
    `location_id`       BIGINT   NOT NULL COMMENT '库位ID',
    `qty`               INT      NOT NULL COMMENT '上架数量',
    `created_at`        DATETIME DEFAULT NULL COMMENT '上架时间',
    PRIMARY KEY (`id`),
    KEY `idx_putaway_item_order` (`putaway_order_id`),
    KEY `idx_putaway_item_location` (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库明细表';

-- ------------------------------------------------------------
-- 7. 出库单表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_outbound_order`;
CREATE TABLE `wms_outbound_order` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`     VARCHAR(64)  NOT NULL COMMENT '出库单号',
    `status`       VARCHAR(16)  NOT NULL COMMENT '状态：PENDING / PICKING / DONE',
    `created_at`   DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_outbound_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库单表';

-- ------------------------------------------------------------
-- 8. 出库明细表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_outbound_item`;
CREATE TABLE `wms_outbound_item` (
    `id`                 BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `outbound_order_id`  BIGINT   NOT NULL COMMENT '出库单ID',
    `product_id`         BIGINT   NOT NULL COMMENT '商品ID',
    `location_id`        BIGINT   DEFAULT NULL COMMENT '拣货库位ID',
    `batch_id`           BIGINT   DEFAULT NULL COMMENT '拣货批次ID',
    `requested_qty`      INT      NOT NULL COMMENT '需求数量',
    `picked_qty`         INT      NOT NULL DEFAULT 0 COMMENT '已拣数量',
    `created_at`         DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_outbound_item_order` (`outbound_order_id`),
    KEY `idx_outbound_item_product` (`product_id`),
    KEY `idx_outbound_item_location` (`location_id`),
    KEY `idx_outbound_item_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库明细表';

-- ------------------------------------------------------------
-- 9. 库存表
--    唯一键 (product_id, location_id, batch_id)：同一商品在同一库位的同一批次只有一条库存记录
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_inventory`;
CREATE TABLE `wms_inventory` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id`   BIGINT   NOT NULL COMMENT '商品ID',
    `location_id`  BIGINT   NOT NULL COMMENT '库位ID',
    `batch_id`     BIGINT   DEFAULT NULL COMMENT '批次ID',
    `qty`          INT      NOT NULL DEFAULT 0 COMMENT '库存数量',
    `updated_at`   DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inventory_prod_loc_batch` (`product_id`, `location_id`, `batch_id`),
    KEY `idx_inventory_location` (`location_id`),
    KEY `idx_inventory_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

-- ------------------------------------------------------------
-- 10. 库存变动日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wms_inventory_log`;
CREATE TABLE `wms_inventory_log` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id`       BIGINT       NOT NULL COMMENT '商品ID',
    `product_name`     VARCHAR(128) DEFAULT NULL COMMENT '商品名称（冗余）',
    `sku`              VARCHAR(64)  DEFAULT NULL COMMENT 'SKU编码（冗余）',
    `location_id`      BIGINT       DEFAULT NULL COMMENT '库位ID',
    `location_code`    VARCHAR(64)  DEFAULT NULL COMMENT '库位编码（冗余）',
    `change_type`      VARCHAR(16)  NOT NULL COMMENT '变动类型：INBOUND / OUTBOUND',
    `change_qty`       INT          NOT NULL COMMENT '变动数量',
    `before_qty`       INT          NOT NULL COMMENT '变动前库存',
    `after_qty`        INT          NOT NULL COMMENT '变动后库存',
    `ref_type`         VARCHAR(32)  DEFAULT NULL COMMENT '关联单据类型：PUTAWAY / OUTBOUND',
    `ref_no`           VARCHAR(64)  DEFAULT NULL COMMENT '关联单号',
    `operator`         VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
    `remark`           VARCHAR(255) DEFAULT NULL COMMENT '备注',
    -- 批次信息
    `item_key`         VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
    `production_date`  DATE         DEFAULT NULL COMMENT '生产日期',
    `shelf_life_days`  INT          DEFAULT NULL COMMENT '保质期（天）',
    `manufacturer`     VARCHAR(128) DEFAULT NULL COMMENT '生产厂商',
    `created_at`       DATETIME     DEFAULT NULL COMMENT '记录时间',
    PRIMARY KEY (`id`),
    KEY `idx_log_product` (`product_id`),
    KEY `idx_log_location` (`location_id`),
    KEY `idx_log_change_type` (`change_type`),
    KEY `idx_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存变动日志表';

-- ------------------------------------------------------------
-- 外键约束
-- ------------------------------------------------------------
ALTER TABLE `wms_putaway_order`
    ADD CONSTRAINT `fk_putaway_order_product` FOREIGN KEY (`product_id`) REFERENCES `wms_product` (`id`),
    ADD CONSTRAINT `fk_putaway_order_batch`   FOREIGN KEY (`batch_id`)   REFERENCES `wms_batch` (`id`);

ALTER TABLE `wms_putaway_item`
    ADD CONSTRAINT `fk_putaway_item_order`    FOREIGN KEY (`putaway_order_id`) REFERENCES `wms_putaway_order` (`id`) ON DELETE CASCADE,
    ADD CONSTRAINT `fk_putaway_item_location` FOREIGN KEY (`location_id`)      REFERENCES `wms_location` (`id`);

ALTER TABLE `wms_outbound_item`
    ADD CONSTRAINT `fk_outbound_item_order`    FOREIGN KEY (`outbound_order_id`) REFERENCES `wms_outbound_order` (`id`) ON DELETE CASCADE,
    ADD CONSTRAINT `fk_outbound_item_product`  FOREIGN KEY (`product_id`)        REFERENCES `wms_product` (`id`),
    ADD CONSTRAINT `fk_outbound_item_location` FOREIGN KEY (`location_id`)       REFERENCES `wms_location` (`id`),
    ADD CONSTRAINT `fk_outbound_item_batch`    FOREIGN KEY (`batch_id`)          REFERENCES `wms_batch` (`id`);

ALTER TABLE `wms_inventory`
    ADD CONSTRAINT `fk_inventory_product`  FOREIGN KEY (`product_id`)  REFERENCES `wms_product` (`id`),
    ADD CONSTRAINT `fk_inventory_location` FOREIGN KEY (`location_id`) REFERENCES `wms_location` (`id`),
    ADD CONSTRAINT `fk_inventory_batch`    FOREIGN KEY (`batch_id`)    REFERENCES `wms_batch` (`id`);
