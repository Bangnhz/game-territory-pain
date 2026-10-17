-- ========================================================
-- Territorial Paint - Script dọn dẹp dữ liệu mẫu / testuser
-- ========================================================
USE territorial_paint;

-- 1. Xóa lịch sử đấu liên quan đến các tài khoản test/mẫu
DELETE FROM MATCH_HISTORY 
WHERE user_id IN (
    SELECT id FROM (
        SELECT id FROM USERS 
        WHERE LOWER(username) LIKE 'testuser%' 
           OR LOWER(username) LIKE 'testusser%' 
           OR LOWER(username) LIKE 'test\\_%'
           OR LOWER(username) = 'test'
    ) AS t1
) OR opponent_id IN (
    SELECT id FROM (
        SELECT id FROM USERS 
        WHERE LOWER(username) LIKE 'testuser%' 
           OR LOWER(username) LIKE 'testusser%' 
           OR LOWER(username) LIKE 'test\\_%'
           OR LOWER(username) = 'test'
    ) AS t2
);

-- 2. Xóa các trận đấu liên quan đến các tài khoản test/mẫu
DELETE FROM MATCHES 
WHERE winner_user_id IN (
    SELECT id FROM (
        SELECT id FROM USERS 
        WHERE LOWER(username) LIKE 'testuser%' 
           OR LOWER(username) LIKE 'testusser%' 
           OR LOWER(username) LIKE 'test\\_%'
           OR LOWER(username) = 'test'
    ) AS t1
) OR forfeited_user_id IN (
    SELECT id FROM (
        SELECT id FROM USERS 
        WHERE LOWER(username) LIKE 'testuser%' 
           OR LOWER(username) LIKE 'testusser%' 
           OR LOWER(username) LIKE 'test\\_%'
           OR LOWER(username) = 'test'
    ) AS t2
);

-- 3. Xóa thống kê (USER_STATS) của các tài khoản test/mẫu
DELETE FROM USER_STATS 
WHERE user_id IN (
    SELECT id FROM (
        SELECT id FROM USERS 
        WHERE LOWER(username) LIKE 'testuser%' 
           OR LOWER(username) LIKE 'testusser%' 
           OR LOWER(username) LIKE 'test\\_%'
           OR LOWER(username) = 'test'
    ) AS t
);

-- 4. Xóa các tài khoản test/mẫu khỏi bảng USERS
DELETE FROM USERS 
WHERE LOWER(username) LIKE 'testuser%' 
   OR LOWER(username) LIKE 'testusser%' 
   OR LOWER(username) LIKE 'test\\_%'
   OR LOWER(username) = 'test';

-- Kiểm tra lại danh sách người dùng còn lại
SELECT id, username, full_name, total_score, created_at FROM USERS;
