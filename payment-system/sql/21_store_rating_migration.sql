-- ========================================
-- 门店评分字段幂等迁移
-- 兼容已执行过 18_store_membership.sql 的存量环境
-- ========================================

USE `payment_db`;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_store_rating_if_missing $$
CREATE PROCEDURE add_store_rating_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'store'
      AND column_name = 'rating'
  ) THEN
    ALTER TABLE `store`
      ADD COLUMN `rating` DECIMAL(3,2) DEFAULT NULL COMMENT '门店评分，范围0.00-5.00'
      AFTER `latitude`;
  END IF;
END $$

CALL add_store_rating_if_missing() $$
DROP PROCEDURE IF EXISTS add_store_rating_if_missing $$

DELIMITER ;
