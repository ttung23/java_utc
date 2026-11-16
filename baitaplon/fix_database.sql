WHENEVER SQLERROR CONTINUE;

PROMPT [1/4] Dang offline toan bo cac datafile bai tap cu bi thieu (17, 18, 19)...
ALTER DATABASE DATAFILE 17 OFFLINE DROP;
ALTER DATABASE DATAFILE 18 OFFLINE DROP;
ALTER DATABASE DATAFILE 19 OFFLINE DROP;

PROMPT [2/4] Dang MO DATABASE ORACLE (ALTER DATABASE OPEN)...
ALTER DATABASE OPEN;
ALTER PLUGGABLE DATABASE ALL OPEN;

PROMPT [3/4] Kiem tra trang thai sau khi mo:
SELECT instance_name, status FROM v$instance;
SELECT name, open_mode FROM v$database;
SELECT name, open_mode FROM v$pdbs;

PROMPT [4/4] Dang tao user slms_user va nap toan bo 6 bang...
ALTER SESSION SET "_ORACLE_SCRIPT"=true;
DROP USER slms_user CASCADE;
CREATE USER slms_user IDENTIFIED BY slms_password DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP;
GRANT CONNECT, RESOURCE, DBA TO slms_user;
GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE TO slms_user;
GRANT UNLIMITED TABLESPACE TO slms_user;

ALTER SESSION SET CURRENT_SCHEMA = slms_user;
@@database/schema.sql

PROMPT ====================================================================
PROMPT CHUC MUNG! ORACLE DATABASE DA MO VA NAP 6 BANG THANH CONG 100%!
PROMPT ====================================================================

EXIT;
