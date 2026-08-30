---------------------------------------------------------------------
-- SBM YSV BEYANNAME — LOKAL GELİŞTİRME ŞEMASI
-- local/local_setup.sql
-- Açıklama: Geliştiricinin kendi lokal Oracle'ında (XE / Free / Docker)
--   projeyi çalıştırabilmesi için sade DDL.
--   FARKLAR (prod db/setup_db.sql'e göre):
--     - CUSTOMER. şema öneki YOK  -> objeler bağlanılan kullanıcının şemasında
--     - PUBLIC SYNONYM YOK        -> DBA yetkisi gerektirmez
--     - GRANT YOK                 -> lokalde rol tanımlı değil
--     - COMMENT YOK               -> kısa tutuldu
--   Tablo / kolon / constraint / index yapısı prod ile birebir AYNIDIR.
--
-- KULLANIM:
--   1) Lokal Oracle'da bir kullanıcı aç:
--        CREATE USER ysv IDENTIFIED BY ysv;
--        GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO ysv;
--   2) Bu script'i o kullanıcı ile çalıştır.
--   3) local/sample_data_scenarios.sql ile test verisini yükle.
--   4) application-local.yml profili bu şemaya bağlanır (bkz. CALISMA-PRENSIBI.md §12).
---------------------------------------------------------------------

-- =============================================
-- 1. SEQUENCES
-- =============================================
CREATE SEQUENCE ALZ_SBM_MUNICIPALITY_SEQ  START WITH 1 INCREMENT BY 1 NOCYCLE CACHE 20 NOORDER;
CREATE SEQUENCE ALZ_SBM_DECL_PROCESS_SEQ  START WITH 1 INCREMENT BY 1 NOCYCLE CACHE 20 NOORDER;
CREATE SEQUENCE ALZ_SBM_DECL_LOG_SEQ      START WITH 1 INCREMENT BY 1 NOCYCLE CACHE 20 NOORDER;

-- =============================================
-- 2. TABLES
-- =============================================
CREATE TABLE ALZ_SBM_MUNICIPALITY (
    ID                  NUMBER(10)     DEFAULT ALZ_SBM_MUNICIPALITY_SEQ.NEXTVAL,
    CITY_CODE           NUMBER(3)      NOT NULL,
    DISTRICT_CODE       NUMBER(10)     NOT NULL,
    MUNICIPALITY_NAME   VARCHAR2(255)  NOT NULL,
    KEP_EMAIL           VARCHAR2(320),
    EMAIL               VARCHAR2(320),
    PHONE               VARCHAR2(30),
    POSTAL_ADDRESS      VARCHAR2(1000),
    NOTIFICATION_TYPE   VARCHAR2(20)   DEFAULT 'BOTH',
    CONTACT_PERSON      VARCHAR2(255),
    IS_ACTIVE           CHAR(1)        DEFAULT 'Y' NOT NULL
);

CREATE TABLE ALZ_SBM_DECL_PROCESS (
    ID                          NUMBER(10)      DEFAULT ALZ_SBM_DECL_PROCESS_SEQ.NEXTVAL,
    DECLARATION_MONTH           NUMBER(2),       -- SBM: ay
    CITY_CODE                   NUMBER(3),       -- SBM: ilKodu
    DISTRICT_CODE               NUMBER(10),      -- SBM: ilceKodu
    COMPANY_CODE                VARCHAR2(3),     -- SBM: sigortaSirketKodu
    PAYMENT_DATE                DATE,            -- SBM: sonOdemeTarihi
    DECLARATION_YEAR            NUMBER(4),       -- SBM: yil
    SBM_FILE_NO                 VARCHAR2(100),   -- SBM: ysvDosyaNo
    RECEIVED_PREMIUM_AMOUNT     NUMBER(15,2),    -- SBM: alinanPrimTutari
    CANCELLED_PREMIUM_AMOUNT    NUMBER(15,2),    -- SBM: iptalPrimTutari
    PREV_MONTH_REFUND_AMOUNT    NUMBER(15,2),    -- SBM: gecmisAyIadeTutari
    MOVABLE_TYPE                VARCHAR2(20),    -- SBM: menkulTipi ("MENKUL"/"GAYRIMENKUL")
    TAX_AMOUNT                  NUMBER(15,2),    -- SBM: odenecekVergi
    TAX_RATIO                   NUMBER(3),       -- SBM: vergiOrani
    TAX_PREMIUM_AMOUNT          NUMBER(15,2),    -- SBM: vergiPrimTutari
    STATUS                      VARCHAR2(20),    -- NEW/PROCESSING/SENT/ERROR/COMPLETED
    DATE_CREATED                DATE            DEFAULT SYSDATE,
    CREATED_BY_USER             VARCHAR2(100),
    DATE_UPDATED                DATE,
    UPDATED_BY_USER             VARCHAR2(100),
    DATE_SENT                   DATE,
    SENT_BY_USER                VARCHAR2(100),
    ERROR_DETAILS               VARCHAR2(2000),
    SOURCE_FILE_NAME            VARCHAR2(500)
);

CREATE TABLE ALZ_SBM_DECL_LOG (
    ID                  NUMBER(10)      DEFAULT ALZ_SBM_DECL_LOG_SEQ.NEXTVAL,
    PROCESS_ID          NUMBER(10),
    OPERATION_TYPE      VARCHAR2(30),
    LOG_LEVEL           VARCHAR2(50),
    LOG_MESSAGE         CLOB,
    REQUEST_PAYLOAD     CLOB,
    RESPONSE_PAYLOAD    CLOB,
    DATE_CREATED        DATE            DEFAULT SYSDATE
);

-- =============================================
-- 3. CONSTRAINTS
-- =============================================
ALTER TABLE ALZ_SBM_MUNICIPALITY  ADD CONSTRAINT PK_ALZ_SBM_MUNICIPALITY  PRIMARY KEY (ID);
ALTER TABLE ALZ_SBM_DECL_PROCESS  ADD CONSTRAINT PK_ALZ_SBM_DECL_PROCESS  PRIMARY KEY (ID);
ALTER TABLE ALZ_SBM_DECL_LOG      ADD CONSTRAINT PK_ALZ_SBM_DECL_LOG      PRIMARY KEY (ID);
ALTER TABLE ALZ_SBM_DECL_LOG ADD CONSTRAINT FK_SBM_LOG_PROCESS FOREIGN KEY (PROCESS_ID) REFERENCES ALZ_SBM_DECL_PROCESS(ID);
ALTER TABLE ALZ_SBM_MUNICIPALITY ADD CONSTRAINT UQ_MUNICIPALITY_CITY_DIST UNIQUE (CITY_CODE, DISTRICT_CODE);
ALTER TABLE ALZ_SBM_MUNICIPALITY ADD CONSTRAINT CHK_MUNICIPALITY_ACTIVE CHECK (IS_ACTIVE IN ('Y', 'N'));
ALTER TABLE ALZ_SBM_MUNICIPALITY ADD CONSTRAINT CHK_MUNICIPALITY_NOTIF_TYPE CHECK (NOTIFICATION_TYPE IN ('KEP', 'POSTAL', 'BOTH', 'NONE'));
ALTER TABLE ALZ_SBM_DECL_PROCESS ADD CONSTRAINT CHK_DECL_MOVABLE_TYPE CHECK (MOVABLE_TYPE IN ('MENKUL', 'GAYRIMENKUL'));
ALTER TABLE ALZ_SBM_DECL_PROCESS ADD CONSTRAINT CHK_DECL_STATUS CHECK (STATUS IN ('NEW', 'PROCESSING', 'SENT', 'ERROR', 'COMPLETED'));
ALTER TABLE ALZ_SBM_DECL_PROCESS ADD CONSTRAINT CHK_DECL_MONTH CHECK (DECLARATION_MONTH BETWEEN 1 AND 12);
ALTER TABLE ALZ_SBM_DECL_PROCESS ADD CONSTRAINT CHK_DECL_YEAR CHECK (DECLARATION_YEAR BETWEEN 2000 AND 2099);

-- =============================================
-- 4. INDEXES
-- =============================================
CREATE INDEX IDX_DECL_PROC_STATUS       ON ALZ_SBM_DECL_PROCESS(STATUS);
CREATE INDEX IDX_DECL_PROC_FILE_NO      ON ALZ_SBM_DECL_PROCESS(SBM_FILE_NO);
CREATE INDEX IDX_DECL_PROC_CITY_DIST    ON ALZ_SBM_DECL_PROCESS(CITY_CODE, DISTRICT_CODE);
CREATE INDEX IDX_DECL_PROC_DATE_CREATED ON ALZ_SBM_DECL_PROCESS(DATE_CREATED);
CREATE INDEX IDX_DECL_PROC_YEAR_MONTH   ON ALZ_SBM_DECL_PROCESS(DECLARATION_YEAR, DECLARATION_MONTH);
CREATE INDEX IDX_DECL_LOG_PROCESS_ID    ON ALZ_SBM_DECL_LOG(PROCESS_ID);
CREATE INDEX IDX_DECL_LOG_DATE          ON ALZ_SBM_DECL_LOG(DATE_CREATED);
CREATE INDEX IDX_MUNICIPALITY_CITY      ON ALZ_SBM_MUNICIPALITY(CITY_CODE);

---------------------------------------------------------------------
-- Doğrulama:
--   SELECT table_name FROM user_tables WHERE table_name LIKE 'ALZ_SBM%';
---------------------------------------------------------------------
