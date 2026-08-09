# Claude Code Prompt — `sbm-declaration-services` (Adım 1)

> Bu dosyanın tamamını Claude Code'a tek mesaj olarak yapıştır.
> Öncesinde bu klasörün içine şu dosyaları koy: `DB_scripti.sql`,
> `SBM_duzenlenmis_SBM_gonderilecek_son_hali.xlsx`, SBM PDF'leri.

---

## ROL VE GÖREV

Sen kıdemli bir Java/Spring Boot geliştiricisisin. Allianz Sigorta için
**`sbm-declaration-services`** adında, üretime çıkacak kalitede bir Spring Boot 3.x /
Java 21 REST projesi yazacaksın. Proje, Yangın Sigorta Vergisi (YSV) beyanname
verilerini Allianz Oracle veritabanından okuyup **ESB üzerinden SBM'ye (Sigorta Bilgi ve
Gözetim Merkezi)** iletir.

Kod, yorumlar ve log mesajları **İngilizce**; kullanıcıya dönen hata mesajları
**Türkçe** olacak. Kodu doğrudan dosyalara yaz, sadece anlatma.

---

## 1. İŞ AKIŞI (KRİTİK — ÖNCE BUNU OKU)

```
OPUS → (manuel SQL script) → ALZ_SBM_DECL_PROCESS tablosu
                                      │
                          [gönder / güncelle / sorgu API]
                                      │
              1) alz-token-management'tan TOKEN al (her çağrıda YENİ)
                                      │
              2) ESB (tek URL) üzerinden SBM REST API'ye istek
                                      │
              3) request + response → ALZ_SBM_DECL_LOG (yasal kanıt)
                                      │
              4) ALZ_SBM_DECL_PROCESS.STATUS güncelle
```

- Excel'i uygulama **okumaz**. Veri DB'ye manuel script ile girer. Uygulama
  sadece tablodan okur. (Excel dosyasını yalnızca alan/veri tiplerini ve
  gruplama mantığını doğrulamak için referans al.)
- **Cache YOK.** Token cache'i `alz-token-management` tarafında. Her gönder,
  güncelle ve sorgu işleminde sıfırdan token alınacak.

---

## 2. TEKNOLOJİ VE PROJE KİMLİĞİ

| Öğe | Değer |
|---|---|
| Repo adı | `sbm-declaration-services` |
| Java | 21 |
| Spring Boot | 3.3.x |
| Base package | `tr.com.allianz.ysv.services` |
| Main class | `DeclarationServiceApplication` |
| Build | Maven |
| HTTP client | **Spring `RestClient`** (RestTemplate/WebClient kullanma) |
| DB | Oracle (`ojdbc11`), Spring Data JPA |
| Diğer | Lombok, MapStruct (veya elle mapper), Validation, Actuator, springdoc-openapi |
| Test | JUnit 5 + Mockito, en az %70 kapsam |

### Paket yapısı (birebir bu şekilde)

```
src/main/java/tr/com/allianz/ysv/services/
├── DeclarationServiceApplication.java
├── config/         RestClientConfig, EsbProperties, TokenManagementProperties, OpenApiConfig
├── controller/     DeclarationController
├── dto/            request/, response/, internal/
├── entity/         DeclarationProcess, DeclarationLog, Municipality
├── enums/          ProcessStatus, MovableType, OperationType, LogLevel
├── exception/      SbmIntegrationException, TokenException, GlobalExceptionHandler, ErrorResponse
├── mapper/         SbmMapper
├── repository/     DeclarationProcessRepository, DeclarationLogRepository, MunicipalityRepository
├── service/        DeclarationService, SbmClientService, TokenManagementService, DeclarationLogService
└── util/           BuyuksehirUtil, DateUtil, JsonUtil
```

---

## 3. VERİTABANI

`DB_scripti.sql` dosyasındaki şemayı **birebir** kullan. Tablolar:

- **`CUSTOMER.ALZ_SBM_DECL_PROCESS`** — beyanname süreç tablosu (ana tablo)
- **`CUSTOMER.ALZ_SBM_DECL_LOG`** — REST request/response log (yasal kanıt, FK: `PROCESS_ID`)
- **`CUSTOMER.ALZ_SBM_MUNICIPALITY`** — belediye referans tablosu

### Kolon → SBM alan eşleşmesi (`ALZ_SBM_DECL_PROCESS`)

| DB kolonu | SBM alanı | Not |
|---|---|---|
| `DECLARATION_MONTH` | `ay` | sadece POST |
| `DECLARATION_YEAR` | `yil` | sadece POST |
| `CITY_CODE` | `ilKodu` | sadece POST |
| `DISTRICT_CODE` | `ilceKodu` | sadece POST, büyükşehirde **null** |
| `COMPANY_CODE` | `sigortaSirketKodu` | POST + PUT, **"045"** |
| `PAYMENT_DATE` | `sonOdemeTarihi` | `yyyy-MM-dd` |
| `SBM_FILE_NO` | `ysvDosyaNo` | max 36, POST + PUT |
| `RECEIVED_PREMIUM_AMOUNT` | `alinanPrimTutari` | `ysvTutarList` içinde |
| `CANCELLED_PREMIUM_AMOUNT` | `iptalPrimTutari` | `ysvTutarList` içinde |
| `MOVABLE_TYPE` | `menkulTipi` | `"MENKUL"` / `"GAYRIMENKUL"` |
| `TAX_AMOUNT` | `odenecekVergi` | `ysvTutarList` içinde |
| `TAX_RATIO` | `vergiOrani` | `ysvTutarList` içinde |
| `TAX_PREMIUM_AMOUNT` | `vergiPrimTutari` | `ysvTutarList` içinde |
| `PREV_MONTH_REFUND_AMOUNT` | `gecmisAyIadeTutari` | DTO'ya ekle, `@JsonInclude(NON_NULL)` — SBM dökümanında listelenmiyor, null ise gönderilmez |
| `STATUS` | — | `NEW / PROCESSING / SENT / ERROR / COMPLETED` |

`ID` alanları `NUMBER(10) DEFAULT <SEQ>.NEXTVAL` — JPA'da
`@GeneratedValue(strategy = SEQUENCE, generator = ...)` + `@SequenceGenerator(allocationSize = 1)`
kullan (sequence cache 20 ama allocationSize **1** olmalı).

---

## 4. SBM ENTEGRASYONU

### 4.1 Endpoint'ler (SBM'nin gerçek adresleri — referans)

| Ortam | Adres |
|---|---|
| PROD | `https://rs.sbm.org.tr/api/rest/vergi-beyan-rs/v10/ysv-beyanname` |
| PREPROD | `https://prers.sbm.org.tr/api/rest/vergi-beyan-rs/v10/ysv-beyanname` |
| ŞİRKET TEST | `https://testrs.sbm.org.tr/api/rest/vergi-beyan-rs/v10/ysv-beyanname` |

**AMA uygulama bu adreslere doğrudan gitmeyecek.** Tüm istekler ESB'ye gider,
ESB ortam bazlı yönlendirmeyi kendisi yapar:

```
ESB base URL (tüm ortamlar için tek): http://esb.allianz.com.tr:12000
```

`pom.xml`'e eklenecek ESB client bağımlılığı:

```xml
<dependency>
    <groupId>tr.com.allianz</groupId>
    <artifactId>ysv-services-rest-client</artifactId>
    <version>9e937d1b74890c608e842f1a009e11db43ae57b4</version>
</dependency>
```

Path'ler property'den yönetilecek (hard-code etme):
- `esb.base-url: http://esb.allianz.com.tr:12000`
- `esb.ysv.beyanname-path: /api/rest/vergi-beyan-rs/v10/ysv-beyanname`
- `esb.ysv.sorgu-path: /api/rest/vergi-beyan-rs/v10/ysv-beyanname/sorgu`

### 4.2 Metotlar

| İşlem | HTTP | Açıklama |
|---|---|---|
| Gönder | `POST` | Yeni beyanname. `ay`, `yil`, `ilKodu`, `ilceKodu` **gönderilir** |
| Güncelle | `PUT` | `ay`, `yil`, `ilKodu`, `ilceKodu` **gönderilmez** (null → `@JsonInclude(NON_NULL)`) |
| Sorgu | `GET` | Body: `sigortaSirketKodu` + `ysvDosyaNo` |

> ⚠️ SBM dökümanı sorgu için "GET" diyor ama gövdeli bir request örneği veriyor.
> `SbmClientService`'te sorgu çağrısını **`esb.ysv.sorgu-method`** property'si ile
> `GET`/`POST` arasında değiştirilebilir yaz ve TODO yorumu bırak.

### 4.3 Örnek REQUEST (POST — tamamlanmış hali)

```json
{
  "ay": 1,
  "ilKodu": 1,
  "ilceKodu": null,
  "yil": 2026,
  "sigortaSirketKodu": "045",
  "sonOdemeTarihi": "2026-01-20",
  "ysvDosyaNo": "YSV202513491",
  "ysvTutarList": [
    {
      "alinanPrimTutari": 7453723.22,
      "iptalPrimTutari": 15090.61,
      "menkulTipi": "MENKUL",
      "odenecekVergi": 743863.26,
      "vergiOrani": 10,
      "vergiPrimTutari": 7438632.61
    },
    {
      "alinanPrimTutari": 0,
      "iptalPrimTutari": 0,
      "menkulTipi": "GAYRIMENKUL",
      "odenecekVergi": 0,
      "vergiOrani": 10,
      "vergiPrimTutari": 0
    }
  ]
}
```

**RESPONSE (başarılı):**
```json
{ "result": true, "status": 200, "ysvDosyaNo": "YSV202513491" }
```

**RESPONSE (hata — HTTP 422):**
```json
{
  "result": false,
  "status": 422,
  "error": {
    "timestamp": "2026-02-02T21:45:38.783",
    "reasons": [
      { "field": "ilceKodu", "code": "CORE-01004",
        "message": "ilceKodu alanının değeri 1 - 4 arasında olmalıdır.",
        "rejectedValue": "21" }
    ]
  }
}
```

`reasons[]` listesini birleştirip `ALZ_SBM_DECL_PROCESS.ERROR_DETAILS`'e (max 2000 char,
taşarsa kırp) yaz ve status'ü `ERROR` yap. Response header'daki **`Transaction-Id`**
değerini mutlaka logla — SBM destek talebinde bu isteniyor.

### 4.4 SBM hata kodları (enum + Türkçe açıklama olarak `SbmErrorCode` içine koy)

| Kod | Anlamı | Aksiyon |
|---|---|---|
| `RISK-HAVUZU-00002` | Başka şirket adına işlem yapılamaz | `sigortaSirketKodu` = 045 kontrolü |
| `RISK-HAVUZU-00003` | İlgili ay veri girişine kapalı | Retry etme, `ERROR` |
| `RISK-HAVUZU-00004` | Mükerrer beyanname (il-ilçe-yıl-ay) | Retry etme |
| `RISK-HAVUZU-00005` | Mükerrer menkul tipi | Gruplama hatası |
| `RISK-HAVUZU-00006` | İl bulunamadı | Veri hatası |
| `RISK-HAVUZU-00007` | Büyükşehirde ilçe gönderilemez | `ilceKodu` null olmalı |
| `RISK-HAVUZU-00008` | Büyükşehir değilse ilçe gönderilmelidir | `ilceKodu` zorunlu |
| `RISK-HAVUZU-00009` | İlçe bulunamadı | Veri hatası |
| `SEC-00001` | Kimlik doğrulama bilgileri gönderilmelidir | Token boş |
| `SEC-00002` | Token geçersiz/süresi dolmuş | **1 kez** yeni token alıp retry |
| `SEC-00003` | Erişim izni yok | Retry etme |
| `SEC-00004` | IP doğrulanamadı | Retry etme, whitelist sorunu |
| `SEC-00005..08` | Header hataları | Retry etme |
| `CORE-00000` | Beklenmeyen hata | Transaction-Id ile destek |
| `CORE-00005/00006` | Format hatası | Veri hatası |
| `CORE-00009` | Kaynak bulunamadı | Endpoint yanlış |
| `CORE-01000` | Zorunlu alan boş | Veri hatası |
| `CORE-01001` | Kayıt bulunamadı | Sorguda normal |
| `CORE-01004/01008` | Değer aralığı / uzunluk hatası | Veri hatası |

> **Veri silme:** SBM'de silme yok. Silme ihtiyacında `PUT` ile tüm tutar
> alanları `0` gönderilir. Bunu `DeclarationService.cancel(...)` metodu olarak yaz.

---

## 5. TOKEN — `alz-token-management`

**Her istekten önce yeni token alınır. Projede cache YOK.**

```
POST https://int-sc-test-auth.allianz.com.tr/alz-token-management/api/v1/tokens/sbm-token-generate
Content-Type: application/json
```

**Request:**
```json
{
  "clientName": "ysv",
  "transactionId": "5ee4333d-9833-47bf-ab66-90359f446123",
  "functionName": "test",
  "userName": "WDA2422_16178",
  "companyCode": "045"
}
```
- `transactionId` → her çağrıda `UUID.randomUUID().toString()`
- `clientName`, `userName`, `companyCode`, base URL → **configs klasöründeki ortam
  bazlı yml dosyalarından** okunacak (`token-management.*`), hard-code YASAK.
- `functionName` → çağrılan operasyona göre (`ysv-beyanname-gonder`,
  `ysv-beyanname-guncelle`, `ysv-beyanname-sorgu`).

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "clientCredentials": { "clientIdentityType": 1, "clientIdNumber": "86773997310" }
}
```

**SBM'ye giden header'lar bu response'tan üretilir:**

| Header | Kaynak |
|---|---|
| `Authorization` | `Bearer <accessToken>` |
| `Requester-ID-Type` | `clientCredentials.clientIdentityType` |
| `Requester-ID-No` | `clientCredentials.clientIdNumber` |
| `Content-Type` | `application/json` |

> Bu değerleri **hard-code etme** — token servisinden gelen değerler kullanılacak.
> `accessToken` ve `clientIdNumber` asla log'a **maskesiz** yazılmayacak
> (ilk 10 karakter + `***`).

---

## 6. İŞ KURALLARI (EN KRİTİK BÖLÜM)

### 6.1 Gruplama
SBM'ye **satır satır değil, gruplanmış** gönderilir:

```
GROUP BY (DECLARATION_YEAR, DECLARATION_MONTH, CITY_CODE, DISTRICT_CODE, SBM_FILE_NO)
   → tek request
   → her grup içindeki MENKUL/GAYRIMENKUL kayıtları ysvTutarList elemanı olur
```
`RISK-HAVUZU-00004` (il-ilçe-yıl-ay bazında tek kayıt) ve `RISK-HAVUZU-00005`
(menkul tipi bazında tek kayıt) bu yüzden var. Aynı grupta aynı `menkulTipi`
iki kez varsa **istek atmadan önce** hata ver.

### 6.2 Büyükşehir mantığı (`BuyuksehirUtil`)
30 büyükşehir il kodu:
```
1(Adana) 6(Ankara) 7(Antalya) 16(Bursa) 20(Denizli) 21(Diyarbakır) 25(Erzurum)
26(Eskişehir) 27(Gaziantep) 31(Hatay) 34(İstanbul) 35(İzmir) 38(Kayseri)
41(Kocaeli) 42(Konya) 44(Malatya) 45(Manisa) 46(Kahramanmaraş) 48(Muğla)
52(Ordu) 55(Samsun) 59(Tekirdağ) 61(Trabzon) 63(Şanlıurfa) 65(Van) 33(Mersin)
53(Rize)? → HAYIR, aşağıdaki listeyi kullan
```
**Kullanılacak kesin liste (30 il):**
`1, 6, 7, 9, 10, 16, 20, 21, 22, 25, 26, 27, 31, 33, 34, 35, 38, 41, 42, 44, 45, 46, 48, 52, 54, 55, 59, 61, 63, 65`

Kural:
- İl büyükşehirse → `ilceKodu` **null** gönderilir (DB'de `0` veya dolu olsa bile).
- Büyükşehir değilse → `ilceKodu` **zorunlu**, null/0 ise istek atma, `ERROR` yaz.
- Excel'de `ilceKodu = 0` olan satırlar büyükşehir satırlarıdır (doğrulandı).

### 6.3 menkulTipi dönüşümü
Excel/OPUS'ta sayısal, SBM'de string:
```
1 → "MENKUL"      2 → "GAYRIMENKUL"
```
DB'de zaten `VARCHAR2(20)` olarak `'MENKUL'/'GAYRIMENKUL'` tutuluyor (CHECK constraint var).
`MovableType` enum'ı `code` (1/2) ve `sbmValue` alanlarını taşısın.

### 6.4 sigortaSirketKodu
Excel'de `2320` görünüyor — bu **OPUS iç kodu**, SBM kodu değil.
SBM'ye gönderilecek değer **`"045"`** (Allianz Sigorta). Property'den oku:
`sbm.company-code: "045"`.

### 6.5 Tarih
`sonOdemeTarihi` → `LocalDate`, `yyyy-MM-dd` formatında serialize edilecek
(`@JsonFormat(pattern = "yyyy-MM-dd")`). Excel'deki `46042` serial değeri
`2026-01-20`'ye karşılık gelir — DB'ye script ile `DATE` olarak girilir.

### 6.6 Status makinesi
```
NEW ──(gönder tetiklendi)──▶ PROCESSING ──(SBM 200 + result:true)──▶ SENT ──▶ COMPLETED
                                  │
                                  └──(hata / result:false)──▶ ERROR  (tekrar denenebilir)
```
- Gönder API'si sadece `STATUS IN ('NEW','ERROR')` kayıtları işler.
- Güncelle API'si sadece `STATUS IN ('SENT','COMPLETED')` kayıtları işler.
- `DATE_SENT`, `SENT_BY_USER`, `DATE_UPDATED`, `UPDATED_BY_USER` alanlarını doldur.
- Aynı kaydın paralel işlenmesini engelle (`@Lock(PESSIMISTIC_WRITE)` veya
  `PROCESSING` status kontrolü).

### 6.7 Loglama (yasal zorunluluk)
Her SBM çağrısı için `ALZ_SBM_DECL_LOG`'a bir satır:
`PROCESS_ID`, `OPERATION_TYPE` (POST/PUT/GET), `LOG_LEVEL`, `LOG_MESSAGE`,
`REQUEST_PAYLOAD` (JSON), `RESPONSE_PAYLOAD` (JSON), `DATE_CREATED`.
- **Başarılı da olsa, hatalı da olsa yazılır.**
- Log yazımı `REQUIRES_NEW` transaction'da olsun ki ana transaction rollback
  olsa bile log kaybolmasın.
- `Authorization` header'ı payload'a yazılmayacak.

---

## 7. REST API (controller)

Base path: `/api/v1/declarations`

| Metot | Path | Açıklama |
|---|---|---|
| `POST` | `/send` | Body: `{ "year": 2026, "month": 1, "cityCode": null, "processIds": [] }` — filtreye uyan `NEW`/`ERROR` kayıtları SBM'ye POST eder |
| `PUT` | `/update` | Aynı filtre — SBM'ye PUT eder |
| `GET` | `/query/{ysvDosyaNo}` | SBM'den sorgular |
| `POST` | `/cancel` | Tutarları 0'layarak PUT eder |
| `GET` | `/processes` | Sayfalı liste (status, yıl, ay, il filtreli) — UI adımı için |

Toplu işlem sonucu dön:
```json
{ "totalGroups": 120, "successCount": 118, "failCount": 2,
  "failures": [ { "ysvDosyaNo": "...", "errorCode": "...", "message": "..." } ] }
```

`@RestControllerAdvice` ile `GlobalExceptionHandler` yaz; tüm hatalar tek tip
`ErrorResponse { timestamp, path, code, message, details[] }` döndürsün.

---

## 8. KONFİGÜRASYON

### `src/main/resources/application.yml`
```yaml
spring:
  application:
    name: sbm-declaration-services
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  jpa:
    hibernate.ddl-auto: none
    properties.hibernate.dialect: org.hibernate.dialect.OracleDialect
    open-in-view: false
server:
  port: 8080
  servlet.context-path: /sbm-declaration-services
management:
  endpoints.web.exposure.include: health,info,metrics,prometheus
  endpoint.health.probes.enabled: true
```

Profiller: `dev` (lokal), `sc-test`, `sc-uat`, `prep`, `prod`.
`application-dev.yml` `src/main/resources` altında (lokal geliştirme, H2 veya
test Oracle). Diğer 4 profil **helm/charts/configs** altında (aşağı bak).

### DB bağlantıları (values altında, ortam bazlı — Vault'tan)
```
SC-UAT : jdbc:oracle:thin:@//opusuat-scan.allianz-tr.local:1521/OPSSCUAT
SC-PREP: jdbc:oracle:thin:@//opusprep-scan.allianz-tr.local:1521/opusprep
SC-PROD: jdbc:oracle:thin:@//opusprod-scan.allianz-tr.local:1453/OPUSAUX
Çıkış IP (SBM whitelist): 195.87.49.10
```
Kullanıcı adı/şifre/URL **koda veya yml'e yazılmayacak** — Vault template'i
`SPRING_DATASOURCE_URL/USERNAME/PASSWORD` env değişkenlerini export ediyor,
Spring bunları otomatik okur.

---

## 9. HELM CHART YAPISI (birebir bu ağaç)

```
helm/
├── charts/
│   ├── common-configs/
│   │   └── application.yml            # tüm ortamlarda ortak Spring ayarları
│   ├── configs/
│   │   ├── application-sc-test.yml
│   │   ├── application-sc-uat.yml
│   │   ├── application-prep.yml
│   │   └── application-prod.yml
│   ├── templates/
│   │   ├── _helpers.tpl
│   │   ├── common-configmap.yaml
│   │   ├── configmap.yaml
│   │   └── secret.yaml
│   ├── .helmignore
│   ├── Chart.yaml
│   └── values.yaml
└── values/
    ├── dr.yaml
    ├── live.yaml
    ├── prep.yaml
    ├── sc-test.yaml
    └── sc-uat.yaml
```

**`helm/values/sc-uat.yaml` — bu formatı birebir kullan, diğer ortamları buna
göre türet:**

```yaml
springboot-deployment:
  app:
    env:
      java:
        args: -Xms3g -Xmx3g -XX:+FlightRecorder
      springboot:
        profile: sc-uat
        sysType: SC-UAT
    resources:
      limits:
        cpu: '2'
        memory: 3Gi
      requests:
        cpu: '0.5'
        memory: 3Gi
    secretManager:
      vault:
        secret: kv/data/UAT
        template: |
          {{ with secret "kv/data/UAT/data-source/opusAgencyDataSource" -}}
            export SPRING_DATASOURCE_USERNAME="{{ .Data.data.username }}"
            export SPRING_DATASOURCE_PASSWORD="{{ .Data.data.password }}"
            export SPRING_DATASOURCE_URL="{{ .Data.data.url }}"
          {{- end }}
    autoscale:
      minReplicas: 1
      maxReplicas: 1
```

Ortam eşlemesi: `sc-test → SC-TEST / kv/data/TEST`, `prep → PREP / kv/data/PREP`,
`live → PROD / kv/data/PROD`, `dr → PROD (DR bölgesi)`.
`prod` ortamında `minReplicas: 2`, `maxReplicas: 4`, `-Xms4g -Xmx4g` yap.

**`helm/charts/configs/application-sc-uat.yml`** içeriği (token ve ESB bilgileri
burada, ortam bazlı):

```yaml
esb:
  base-url: http://esb.allianz.com.tr:12000
  ysv:
    beyanname-path: /api/rest/vergi-beyan-rs/v10/ysv-beyanname
    sorgu-path: /api/rest/vergi-beyan-rs/v10/ysv-beyanname/sorgu
    sorgu-method: GET
  connect-timeout: 10s
  read-timeout: 60s

token-management:
  base-url: https://int-sc-test-auth.allianz.com.tr
  path: /alz-token-management/api/v1/tokens/sbm-token-generate
  client-name: ysv
  user-name: WDA2422_16178
  company-code: "045"
  connect-timeout: 5s
  read-timeout: 30s

sbm:
  company-code: "045"
  retry:
    max-attempts: 2          # sadece SEC-00002 ve 5xx için
```

---

## 10. TESTLER

- `SbmMapperTest` — büyükşehir/ilçe null mantığı, menkulTipi dönüşümü,
  POST/PUT alan farkı (`@JsonInclude(NON_NULL)` doğrulaması)
- `BuyuksehirUtilTest` — 30 il + negatif senaryolar
- `DeclarationServiceTest` — gruplama, status geçişleri, mükerrer menkul tipi
- `TokenManagementServiceTest` — MockRestServiceServer ile
- `SbmClientServiceTest` — 200/422/401 senaryoları, `Transaction-Id` loglama
- `DeclarationControllerTest` — `@WebMvcTest`
- `GlobalExceptionHandlerTest`

Hepsi **yeşil** olacak. `mvn clean verify` hatasız geçmeli.

---

## 11. TESLİM EDİLECEKLER

1. Çalışan Maven projesi (`mvn clean package` başarılı)
2. `helm/` ağacı yukarıdaki yapıda, 5 values dosyası + 4 config dosyası
3. `README.md` — kurulum, profiller, endpoint listesi, iş akışı diyagramı,
   SBM hata kodu tablosu
4. `docs/api-examples.http` — POST/PUT/GET örnek istekleri
5. `db/setup_db.sql` — verilen script (değiştirmeden)
6. `db/sample_insert.sql` — Excel'den türetilmiş **50 satırlık** örnek INSERT
   (büyükşehir ve ilçeli iller karışık, MENKUL/GAYRIMENKUL çiftli gruplar)
7. `.gitignore`, `Dockerfile`, `Jenkinsfile` (basit build/test/deploy iskeleti)

---

## 12. GIT VE GITHUB

Tüm dosyaları yazdıktan ve `mvn clean verify` yeşil geçtikten sonra:

```bash
git init -b main
git add .
git commit -m "feat(SBMD-13): SBM YSV beyanname gönderim servisi - ilk sürüm"

# GitHub CLI kurulu ve giriş yapılmışsa:
gh auth status || gh auth login
gh repo create m-ogur34/sbm-declaration-services --private --source=. --remote=origin --push

# gh yoksa, repoyu elle açtıktan sonra:
# git remote add origin https://github.com/m-ogur34/sbm-declaration-services.git
# git push -u origin main
```

Ayrıca `feature/SBMD-13` branch'i oluşturup oradan çalış, main'e PR aç.

---

## 13. YASAKLAR

- ❌ Şifre, token, kullanıcı adı hard-code etme
- ❌ SBM adreslerine doğrudan gitme (her zaman ESB üzerinden)
- ❌ Projede token cache'i yazma
- ❌ `RestTemplate` / `WebClient` kullanma → `RestClient`
- ❌ `ddl-auto: update` — şema script ile yönetiliyor
- ❌ Excel okuma kodu yazma (veri DB'ye manuel giriliyor)
- ❌ Log'a maskesiz token/kimlik numarası yazma

## 14. BELİRSİZ NOKTALAR — kod yaz, üstüne `// TODO(confirm):` bırak

1. Sorgu servisinin gerçekten `GET` mi `POST` mu olduğu (döküman çelişkili)
2. ESB'nin SBM path'lerini birebir mi proxy'lediği, yoksa kendi path'i mi olduğu
3. `gecmisAyIadeTutari` alanının SBM tarafında karşılığı olup olmadığı
4. Token servisinde `functionName` alanına hangi değerin beklendiği
5. PROD token servisi base URL'i (elimizde sadece TEST adresi var)

Bu 5 maddeyi `README.md` sonunda "Açık Konular" başlığı altında da listele.
