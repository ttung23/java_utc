-- ====================================================================
-- SCRIPT THIẾT LẬP ORACLE 23ai CHO HỆ THỐNG QUẢN LÝ THƯ VIỆN SLMS
-- Chạy script này bằng quyền SYSDBA:
-- sqlplus / as sysdba @database/setup_oracle_23ai.sql
-- ====================================================================

-- 1. Cho phép tạo user thông thường trên Container Database (CDB)
ALTER SESSION SET "_ORACLE_SCRIPT" = TRUE;

-- 2. Xóa user cũ nếu đã tồn tại
BEGIN
    EXECUTE IMMEDIATE 'DROP USER slms_user CASCADE';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

-- 3. Tạo User slms_user với mật khẩu slms_password
CREATE USER slms_user IDENTIFIED BY slms_password
DEFAULT TABLESPACE USERS
TEMPORARY TABLESPACE TEMP;

-- 4. Cấp đầy đủ quyền thao tác cho slms_user
GRANT CONNECT, RESOURCE, DBA TO slms_user;
GRANT CREATE SESSION TO slms_user;
GRANT CREATE TABLE TO slms_user;
GRANT CREATE VIEW TO slms_user;
GRANT CREATE SEQUENCE TO slms_user;
GRANT UNLIMITED TABLESPACE TO slms_user;

PROMPT >>> Đã tạo user slms_user thành công!
PROMPT >>> Đang tiến hành tạo các bảng và nạp dữ liệu mẫu vào schema slms_user...

-- 5. Chuyển context sang schema slms_user để tạo bảng
ALTER SESSION SET CURRENT_SCHEMA = slms_user;

-- 6. Thực thi script schema.sql
@database/schema.sql

PROMPT ====================================================================
PROMPT >>> CHÚC MỪNG! HỆ THỐNG ĐÃ NẠP TOÀN BỘ 6 BẢNG VÀO ORACLE 23ai THẬT!
PROMPT >>> Username: slms_user
PROMPT >>> Password: slms_password
PROMPT >>> Service Name / SID: FREE
PROMPT ====================================================================
EXIT;

