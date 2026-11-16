CONNECT / as sysdba
SELECT instance_name, status, database_status FROM v$instance;
SELECT name, open_mode FROM v$database;
SELECT name, open_mode FROM v$pdbs;
ALTER DATABASE OPEN;
ALTER PLUGGABLE DATABASE ALL OPEN;
ALTER SESSION SET "_ORACLE_SCRIPT"=true;
SELECT username FROM all_users WHERE username = 'SLMS_USER';
EXIT;
