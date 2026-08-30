# TEST-PLAN — YSV → SBM entegrasyonu

> VDI'da adım adım işaretlenecek çalışma listesi. Referans: `CALISMA-PRENSIBI.md`,
> `HELM-VE-KONFIG.md`. Durum: `[ ]` yapılacak · `[x]` tamam · `[!]` sorun var (not düş).
>
> **Token kuralı (Claude ile):** hata → özet metin (başarısız test FQN + ilk assertion,
> ya da `dosya:satır` + mesaj). Tam `mvn` logu / stack trace yapıştırma. Ekran görüntüsü
> yerine metin.

---

## 0. Ön koşullar

- [ ] VDI'da repo güncel: `git checkout feature/SBMD-13 && git pull`
- [ ] Java 21 + Maven (wrapper yok, düz `mvn`)
- [ ] İç Nexus erişimi var

---

## 1. Derleme (VDI, iç Nexus)

- [ ] `mvn clean verify`
  - [ ] Bağımlılıklar iniyor — özellikle `org.apache.poi:poi-ooxml:5.3.0`
        (+ `commons-compress`, `xmlbeans`, `commons-collections4`).
        İnmezse: iç Nexus'ta mevcut POI sürümünü söyle → `pom.xml`'de `poi.version` güncellenir.
  - [ ] 266 test yeşil
  - [ ] JaCoCo `check` geçti (INSTRUCTION ≥ %90, BRANCH ≥ %90)
- Not: Mac'te (Maven Central) `BUILD SUCCESS` alındı — VDI'da tek risk Nexus'ta POI.

**Sonuç:** ___

---

## 2. Veritabanı

### 2a. Lokal Oracle (geliştirici makinesi)
- [ ] Kullanıcı: `CREATE USER ysv IDENTIFIED BY ysv; GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO ysv;`
- [ ] `db/local/local_setup.sql`
- [ ] `db/sample_data_scenarios.sql` (S1–S10)
- [ ] Doğrulama: `SELECT SBM_FILE_NO, STATUS, MOVABLE_TYPE FROM ALZ_SBM_DECL_PROCESS WHERE SBM_FILE_NO LIKE 'YSV-T-%';`

### 2b. SC-TEST / SC-UAT (DBA ile)
- [ ] `db/setup_db.sql` çalıştırıldı
- [ ] Geri alma elde: `db/rollback_db.sql` (tüm ortamlara deploy edilebilir, toleranslı)
- [ ] Vault path'i doğru: `kv/data/<ENV>/data-source/opusAgencyDataSource` → `username/password/url`

**Sonuç:** ___

---

## 3. Uygulama ayağa kaldırma

### 3a. Lokal profil
- [ ] `mvn spring-boot:run -Dspring-boot.run.profiles=local`
- [ ] Log: `HikariPool-1 - Start completed`
- [ ] Swagger: `http://localhost:8080/sbm-declaration-services/swagger-ui.html`

### 3b. k8s (SC-TEST) — `HELM-VE-KONFIG.md` §5 kontrol listesi
- [ ] `helm dependency update ./helm/chart`
- [ ] `helm upgrade --install sbm-declaration-services ./helm/chart -f helm/values/sc-test.yaml -n <ns>`
- [ ] `kubectl exec <pod> -- env | grep SPRING_PROFILES_ACTIVE` → `sc-test`
- [ ] `kubectl exec <pod> -- env | grep SPRING_DATASOURCE_` → URL/USERNAME/PASSWORD dolu
- [ ] `kubectl describe pod` → ConfigMap mount + `SPRING_CONFIG_*`
- [ ] `API_GUARD_API_KEY` secret'ı tanımlı mı (boşsa anahtar kontrolü kapalı, uyarı loglanır)
- [ ] `/actuator/health/liveness` + `/readiness` 200

**Sonuç:** ___

---

## 4. 1. AŞAMA — Excel yükleme

- [ ] `POST /api/v1/declarations/upload` (multipart, `file` = `YSV-OPUS.xlsx`, header `X-User-Name`)
  - [ ] Yanıt: `totalRows`, `inserted`, `failed`, `errors[]` beklendiği gibi
  - [ ] DB'de satırlar `STATUS=NEW`, `COMPANY_CODE='045'`, `SOURCE_FILE_NAME` dolu
  - [ ] `sonOdemeTarihi` doğru (Excel seri `46042` → `2026-01-20`)
  - [ ] `menkulTipi` `1`/`2` → `MENKUL`/`GAYRIMENKUL`
- [ ] Hatalı satır senaryosu: bozuk bir satırlı dosya → o satır `errors[]`'te, diğerleri yazıldı
- [ ] Mükerrer: aynı dosyayı 2. kez yükle → tüm satırlar `ALZ-EXCEL-DUPLICATE`
- [ ] İki farklı (yıl, ay) içeren dosya → HTTP 400 (`birden fazla dönem`)
- [ ] `.csv` / `.xls` yükleme → HTTP 400

**Sonuç:** ___

---

## 5. 2. AŞAMA — Token

- [ ] Bir `send`/`query` tetiklendiğinde token servisine istek gidiyor (log: `Requesting SBM token`)
- [ ] Her çağrıda yeni `transactionId` (cache yok)
- [ ] İstek gövdesi: `clientName/transactionId/functionName/userName/companyCode`
- [ ] `functionName` = ortam config'i (SC-TEST'te `test`)
- [ ] Cevap: `accessToken` + `clientCredentials.clientIdentityType`/`clientIdNumber`
- [ ] Log'da token/kimlik no maskeli

**Sonuç:** ___

---

## 6. 3. + 4. AŞAMA — SBM gönder/güncelle/sorgu (ESB üzerinden)

### 6a. Gönder (POST)
- [ ] `POST /api/v1/declarations/send` (filtre ile NEW satırlar)
- [ ] Header'lar ESB'ye gidiyor: `Authorization: Bearer ...`, `Requester-ID-Type`, `Requester-ID-No`
- [ ] **Tipli JSON** gönderiliyor (sayısal alanlar tırnaksız) → SBM **422 vermiyor**
      → verirse: hangi alan? `CALISMA-PRENSIBI.md` §5.2 fallback (o alan string'e çevrilir)
- [ ] Başarı: HTTP 2xx + `{"result":true,"data":{"ysvDosyaNo":...},"status":201}` → satırlar `SENT`
- [ ] Büyükşehir satırında payload'da `ilceKodu` **yok**
- [ ] Kompleks grup (S3): `ysvTutarList` 2 eleman (MENKUL + GAYRIMENKUL), biri negatif `gecmisAyIadeTutari`
- [ ] Mükerrer menkul tipi (S7): yerel `RISK-HAVUZU-00005`, SBM'ye gitmeden `ERROR`

### 6b. Güncelle (PUT) / İptal
- [ ] `PUT /api/v1/declarations/update` (SENT satırlar) → gövdede `ay/yil/ilKodu/ilceKodu` **yok**
- [ ] `POST /api/v1/declarations/cancel` → tüm tutarlar `0` ile PUT

### 6c. Sorgu (GET)
- [ ] `GET /api/v1/declarations/query/{ysvDosyaNo}`
- [ ] ESB'ye **GET + query string** gidiyor (`?ysvDosyaNo=..&sigortaSirketKodu=045`), gövde yok
- [ ] Cevap zarfı `{"result":true,"data":{...beyanname...},"status":200}` → `data` altındaki alanlar dolu
- [ ] Başarılıysa ilgili satırlar `SENT` → `COMPLETED`

### 6d. Hata / retry
- [ ] SBM 422 → `error.reasons[]` `ERROR_DETAILS`'e yazıldı, retry **yok**
- [ ] `SEC-00002` / 5xx → 1 kez retry (yeni token ile), sonra `ERROR`
- [ ] Beklenmeyen hata → response header `Transaction-Id` loglandı

**Sonuç:** ___

---

## 7. PEN test savunma katmanı

- [ ] `X-Api-Key` header'ı olmadan `/api/v1/**` → HTTP 401 (secret tanımlıysa)
- [ ] Doğru `X-Api-Key` → geçiyor
- [ ] `/actuator/health`, `/swagger-ui`, `/v3/api-docs` → anahtarsız erişilebiliyor (muaf)
- [ ] Kısa sürede çok istek → HTTP 429 + `Retry-After`
- [ ] Actuator: `env`, `beans`, `heapdump` kapalı (sadece health/info/metrics/prometheus)

**Sonuç:** ___

---

## 8. Entegrasyon smoke (VDI ağı)

- [ ] `curl -sS -o /dev/null -w "%{http_code}\n" -X POST https://int-sc-test-auth.allianz.com.tr/alz-token-management/api/v1/tokens/sbm-token-generate -H 'content-type: application/json' -d '{...}'`
- [ ] `curl -sv http://esb.allianz.com.tr:12000` → bağlanıyor mu (status kodu)
- [ ] DNS yoksa `ESB_SERVER` ile IP override (`helm/values/<ortam>.yaml`)

**Sonuç:** ___

---

## Açık maddeler (CALISMA-PRENSIBI.md §11)

- [ ] Tipli gövde 422 kontrolü (§6a)
- [ ] ESB proxy path'i kesinleştirme (ESB ekibi / OSB konsolu)
- [ ] Token `functionName` — token ekibi (Hüseyin Dağ / Ömer Faruk Ceylan)
- [ ] SBM REST şifresi (`koc`) + TEST/PRE/PROD IP whitelist
- [ ] Veri anomalisi (iş birimi): büyükşehir + ilçe kodlu 14 satır; büyükşehir değil + ilçe 0 olan 2 satır
- [ ] İş biriminden Excel `menkulTipi`'ni metin (`MENKUL`/`GAYRIMENKUL`) isteme
