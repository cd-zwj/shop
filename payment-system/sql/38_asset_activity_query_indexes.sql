-- Unified asset activity feed pagination indexes.
ALTER TABLE `merchant_wallet_log`
  ADD INDEX `idx_merchant_activity_page` (`platform_user_id`, `create_time`);

ALTER TABLE `member_points_log`
  ADD INDEX `idx_points_activity_page` (`platform_user_id`, `create_time`);

ALTER TABLE `member_growth_log`
  ADD INDEX `idx_growth_activity_page` (`platform_user_id`, `create_time`);

ALTER TABLE `user_coupon`
  ADD INDEX `idx_user_coupon_activity_join` (`platform_user_id`);

ALTER TABLE `coupon_expire_record`
  ADD INDEX `idx_coupon_expire_activity_page` (`platform_user_id`, `expire_time`);

ALTER TABLE `coupon_lock_record`
  ADD INDEX `idx_coupon_lock_activity_page` (`platform_user_id`, `lock_time`);

ALTER TABLE `coupon_release_record`
  ADD INDEX `idx_coupon_release_activity_page` (`platform_user_id`, `release_time`);

ALTER TABLE `coupon_write_off_record`
  ADD INDEX `idx_coupon_write_off_activity_page` (`platform_user_id`, `write_off_time`);
