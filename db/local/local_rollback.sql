---------------------------------------------------------------------
-- SBM YSV BEYANNAME — LOKAL GELİŞTİRME ŞEMASI
-- local/local_rollback.sql
-- Açıklama: local_setup.sql'in oluşturduğu objeleri geri alır (kendi şemanda).
--   Toleranslı: obje yoksa hata vermez.
-- KULLANIM: local_setup.sql'i çalıştıran kullanıcı ile çalıştır.
---------------------------------------------------------------------

SET SERVEROUTPUT ON

DECLARE
    PROCEDURE exec_ignore(p_sql IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;
        DBMS_OUTPUT.PUT_LINE('OK   : ' || p_sql);
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE IN (-942, -2289, -4043) THEN
                DBMS_OUTPUT.PUT_LINE('SKIP : ' || p_sql || '  (' || SQLCODE || ')');
            ELSE
                RAISE;
            END IF;
    END;
BEGIN
    exec_ignore('DROP TABLE ALZ_SBM_DECL_LOG CASCADE CONSTRAINTS PURGE');
    exec_ignore('DROP TABLE ALZ_SBM_DECL_PROCESS CASCADE CONSTRAINTS PURGE');
    exec_ignore('DROP TABLE ALZ_SBM_MUNICIPALITY CASCADE CONSTRAINTS PURGE');
    exec_ignore('DROP SEQUENCE ALZ_SBM_DECL_LOG_SEQ');
    exec_ignore('DROP SEQUENCE ALZ_SBM_DECL_PROCESS_SEQ');
    exec_ignore('DROP SEQUENCE ALZ_SBM_MUNICIPALITY_SEQ');
    DBMS_OUTPUT.PUT_LINE('local_rollback.sql tamamlandi.');
END;
/
