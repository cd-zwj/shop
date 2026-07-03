-- Align built-in platform test accounts with project docs.
-- Password for admin/merchant/user/user2: admin123

SET @admin123_hash = '$2a$10$RM41BiXeQbfAcRJbjh2g2O6Z9lHJEO/Or6b40U8p.BXHmbSwfs5Ky';
SET @old_unknown_hash = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJw2';

INSERT INTO platform_user (user_no, username, phone, email, password_hash, status, deleted)
VALUES
    ('PU202606070001', 'admin', '13800000001', 'admin@test.com', @admin123_hash, 1, 0),
    ('PU202606070002', 'merchant', '13800000002', 'merchant@test.com', @admin123_hash, 1, 0),
    ('PU202606070003', 'user', '13800000003', 'user@test.com', @admin123_hash, 1, 0),
    ('PU202606070004', 'user2', '13800000004', 'user2@test.com', @admin123_hash, 1, 0)
ON DUPLICATE KEY UPDATE
    phone = VALUES(phone),
    email = VALUES(email),
    password_hash = CASE
        WHEN password_hash = @old_unknown_hash THEN @admin123_hash
        ELSE password_hash
    END,
    status = 1,
    deleted = 0;
