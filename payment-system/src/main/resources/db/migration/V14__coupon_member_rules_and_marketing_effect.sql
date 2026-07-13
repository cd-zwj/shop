SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE coupon_template ADD COLUMN required_member_level INT NULL COMMENT ''最低可用会员等级''',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_template'
      AND column_name = 'required_member_level'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE coupon_template ADD COLUMN required_member_tag_ids VARCHAR(255) NULL COMMENT ''必须具备的会员标签ID列表''',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_template'
      AND column_name = 'required_member_tag_ids'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE coupon_template ADD COLUMN excluded_member_tag_ids VARCHAR(255) NULL COMMENT ''不可用会员标签ID列表''',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_template'
      AND column_name = 'excluded_member_tag_ids'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
