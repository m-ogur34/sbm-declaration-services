---------------------------------------------------------------------
-- SBM YSV BEYANNAME OTOMASYON SİSTEMİ
-- alter_decl_log_triggered_by_user.sql
-- Versiyon: 1.0 | Tarih: 2026-09-15
-- Açıklama: ALZ_SBM_DECL_LOG tablosuna TRIGGERED_BY_USER kolonu ekler.
--   Process tablosundaki UPDATED_BY_USER / SENT_BY_USER sadece son işlemi
--   tutar; çağrı bazlı "kim tetikledi" bilgisi log tablosunda saklanır.
--
-- KULLANIM:
--   setup_db.sql'in ESKİ sürümüyle kurulmuş şemalarda (ör. VDI test DB)
--   bir kez çalıştırılır. Yeni kurulumlarda gerekmez: kolon artık
--   setup_db.sql içinde tanımlı.
---------------------------------------------------------------------
ALTER TABLE CUSTOMER.ALZ_SBM_DECL_LOG ADD (TRIGGERED_BY_USER VARCHAR2(100));

COMMENT ON COLUMN CUSTOMER.ALZ_SBM_DECL_LOG.TRIGGERED_BY_USER IS 'Islemi tetikleyen kullanici (X-User-Name); UI baglanana kadar SYSTEM. Kimlik dogrulama degildir';

-- Geri alma:
-- ALTER TABLE CUSTOMER.ALZ_SBM_DECL_LOG DROP COLUMN TRIGGERED_BY_USER;
