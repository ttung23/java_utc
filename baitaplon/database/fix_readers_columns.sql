-- ====================================================================
-- SLMS: Script bo sung cac cot email, phone, password_hash vao READERS
-- va tao bang STAFF_USERS trong Oracle Database
-- ====================================================================

SET SERVEROUTPUT ON;

PROMPT Dang bo sung cot cho bang READERS...

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE READERS ADD (email VARCHAR2(150))';
    DBMS_OUTPUT.PUT_LINE('>>> Da them cot email vao READERS');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN
            DBMS_OUTPUT.PUT_LINE('>>> Cot email da ton tai trong READERS');
        ELSE
            DBMS_OUTPUT.PUT_LINE('>>> Luu y email: ' || SQLERRM);
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE READERS ADD (phone VARCHAR2(30))';
    DBMS_OUTPUT.PUT_LINE('>>> Da them cot phone vao READERS');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN
            DBMS_OUTPUT.PUT_LINE('>>> Cot phone da ton tai trong READERS');
        ELSE
            DBMS_OUTPUT.PUT_LINE('>>> Luu y phone: ' || SQLERRM);
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE READERS ADD (password_hash VARCHAR2(100))';
    DBMS_OUTPUT.PUT_LINE('>>> Da them cot password_hash vao READERS');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN
            DBMS_OUTPUT.PUT_LINE('>>> Cot password_hash da ton tai trong READERS');
        ELSE
            DBMS_OUTPUT.PUT_LINE('>>> Luu y password_hash: ' || SQLERRM);
        END IF;
END;
/

PROMPT Dang kiem tra va tao bang STAFF_USERS...

BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE STAFF_USERS (
        staff_id VARCHAR2(20) NOT NULL,
        username VARCHAR2(50) NOT NULL UNIQUE,
        full_name VARCHAR2(100) NOT NULL,
        password_hash VARCHAR2(100) NOT NULL,
        role VARCHAR2(20) NOT NULL,
        status VARCHAR2(20) DEFAULT ''ACTIVE'',
        CONSTRAINT pk_staff_users PRIMARY KEY (staff_id)
    )';
    DBMS_OUTPUT.PUT_LINE('>>> Da tao bang STAFF_USERS thanh cong');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('>>> Bang STAFF_USERS da ton tai');
        ELSE
            DBMS_OUTPUT.PUT_LINE('>>> Luu y STAFF_USERS: ' || SQLERRM);
        END IF;
END;
/

COMMIT;

PROMPT ====================================================================
PROMPT >>> HOAN TAT BO SUNG COT VA BANG CHO DATABASE SLMS!
PROMPT ====================================================================

