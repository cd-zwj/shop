ALTER TABLE coupon_template
  ADD COLUMN IF NOT EXISTS required_member_level INT NULL COMMENT '最低可用会员等级',
  ADD COLUMN IF NOT EXISTS required_member_tag_ids VARCHAR(255) NULL COMMENT '必须具备的会员标签ID列表',
  ADD COLUMN IF NOT EXISTS excluded_member_tag_ids VARCHAR(255) NULL COMMENT '不可用会员标签ID列表';
