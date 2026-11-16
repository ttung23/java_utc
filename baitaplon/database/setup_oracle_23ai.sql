-- ====================================================================
-- SCRIPT THIẾT LẬP ORACLE 23ai CHO HỆ THỐNG QUẢN LÝ THƯ VIỆN SLMS
-- Chạy script này bằng quyền SYSDBA:
-- sqlplus / as sysdba @setup_oracle_23ai.sql
-- ====================================================================

SET ECHO ON;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR CONTINUE;

-- 0. Kiểm tra trạng thái và mở Database
-- Offline datafile bài tập cũ bị thiếu nếu có (TBS02_1.DBF)
ALTER DATABASE DATAFILE 'C:\APP\SUMAYTINH0912350956\PRODUCT\26AI\ORADATA\FREE\TBS02_1.DBF' OFFLINE DROP;
ALTER DATABASE DATAFILE 17 OFFLINE DROP;
ALTER DATABASE OPEN;
ALTER PLUGGABLE DATABASE ALL OPEN;

PROMPT >>> Da kiem tra va thuc hien lenh MO Database!

-- 1. Cho phép tạo user thông thường trên Container Database (CDB)
ALTER SESSION SET "_ORACLE_SCRIPT" = TRUE;

-- 2. Xóa user cũ nếu đã tồn tại
DROP USER slms_user CASCADE;

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

PROMPT >>> Da tao user slms_user thanh cong!
PROMPT >>> Dang tien hanh tao cac bang va nap du lieu mau vao schema slms_user...

-- 5. Chuyển context sang schema slms_user để tạo bảng
ALTER SESSION SET CURRENT_SCHEMA = slms_user;

-- 6. Thực thi script schema.sql (dùng @@ để tìm cùng thư mục với script này)
@@schema.sql

PROMPT ====================================================================
PROMPT >>> CHUC MUNG! HE THONG DA NAP TOAN BO 6 BANG VAO ORACLE 23ai THAT!
PROMPT >>> Username: slms_user
PROMPT >>> Password: slms_password
PROMPT >>> Service Name / SID: FREE
PROMPT ====================================================================
