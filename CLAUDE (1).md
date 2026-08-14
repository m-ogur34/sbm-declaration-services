# CLAUDE.md — sbm-declaration-services

Bu dosya, projede çalışacak agent için bağlam ve kesinleşmiş kararları içerir.
Kod yazmadan önce bu dosyanın tamamını oku. Buradaki kararlar tartışmaya açık
değildir; bir çelişki görürsen kod değiştirmeden önce sor.

---

## 1. Proje nedir

Yangın Sigorta Vergisi (YSV) beyanname verilerinin SBM'ye (Sigorta Bilgi ve
Gözetim Merkezi) REST üzerinden gönderilmesi, güncellenmesi ve sorgulanması.
Allianz Sigorta içi proje. Şirket kodu: **045**.

Üç repodan oluşan bir teslimatın **1. adımı** bu repo:

| Repo | İçerik | Durum |
|---|---|---|
| `sbm-declaration-services` | SBM'ye veri gönderen servis katmanı | **bu repo — aktif** |
| `sbm-declaration-ui-backend` | UI için backend | sonraki adım |
| `sbm-declaration-ui` | Angular 17 arayüz | sonraki adım |

### Uçtan uca akış

1. OPUS sisteminden alınan YSV verileri, **manuel script** ile
   `CUSTOMER.ALZ_SBM_DECL_PROCESS` tablosuna basılır. (Uygulama bu insert'i yapmaz.)
2. Bu repodaki **gönder / güncelle / sorgu** API'leri tetiklenir.
3. Her çağrıda `alz-token-management` servisinden **taze token** alınır.
4. İstek **ESB (OSB 12c)** üzerinden SBM'ye çıkar.

---

## 2. Teknoloji

- Spring Boot 3.x / **Java 21**
- HTTP istemcisi: **Spring `RestClient`** (RestTemplate/WebClient kullanma)
- Oracle DB
- Java paket kökü: `tr.com.allianz.ysv.services`
  alt paketler: `client`, `config`, `controller`, `dto`, `mapper`, `service`
- Build: Maven (`mvnw`)

---

## 3. Token — `alz-token-management`

YSV'nin kendi token servisi **yoktur ve olmayacaktır**. Merkezi servis kullanılır.

- TEST endpoint:
  `POST https://int-sc-test-auth.allianz.com.tr/alz-token-management/api/v1/tokens/sbm-token-generate`
  (path `sbm-token-generate`, `sbm-generate-token` **değil**)
- Request alanları: `clientName` (= `ysv`), `transactionId` (her istekte yeni UUID),
  `functionName`, `userName`, `companyCode` (= `045`)
- Response: `accessToken` + `clientCredentials`
  - `clientIdentityType` → SBM header `Requester-ID-Type`
  - `clientIdNumber` → SBM header `Requester-ID-No`
  - Bu iki header **hardcode edilmez**, token cevabından gelir.
- **Cache YOK.** Her gönder/güncelle/sorgu çağrısında yeni token alınır.
  Cache zaten `alz-token-management` tarafında.
- İlgili sınıflar: `TokenManagementService`, `TokenManagementProperties`, `TokenManagementDto`

### Kritik konfigürasyon kuralı

`alz-token-management` request parametrelerinin **tamamı ortam bazlıdır**:
`base-url`, `path`, `clientName`, `userName`, `companyCode`.
Hiçbiri ortak/paylaşılan default olarak yazılmaz — hepsi
`helm/chart/configs/application-<ortam>.yml` içinde ayrı ayrı tanımlanır.

### Kaldırılmış olması gereken eski yapı

`SbmTokenService` (userCode/password ile doğrudan SBM authenticate), `SbmTokenDto`,
`sbm.auth.*` konfigürasyonu ve 25 dakikalık cache — **obsolete**. Kodda kalıntı
varsa temizle.

---

## 4. ESB

- Tek URL: `http://esb.allianz.com.tr:12000`
- ESB ortam bazlı yönlendirmeyi **kendisi** yapar; uygulama ortama göre farklı
  SBM adresi seçmez.
- **pom.xml'e ESB için hiçbir dependency eklenmez.** Uygulama ESB URL'ine
  doğrudan HTTP isteği atar. (`tr.com.allianz:ysv-services-rest-client` diye bir
  bağımlılık **yok**; eski notlarda geçtiyse yanlıştır.)
- ESB proxy path deseni (SOAP projesinden referans):
  `YsvServices/ProxyService/...`

---

## 5. SBM sözleşmesi — alan tipleri (EN KRİTİK BÖLÜM)

SBM dökümanının **alan tipi tablosu** esastır. Dökümandaki güncellenmiş örnek
JSON'da her değer tırnak içinde string olarak gösterilmiş — **bu örnek yanlıştır,
uyma.** Tipler SOAP WSDL stub'ları ve sorgu response örneğiyle de doğrulanmıştır.

| Alan | JSON tipi |
|---|---|
| `ay`, `ilKodu`, `ilceKodu`, `yil`, `vergiOrani` | **number** |
| `alinanPrimTutari`, `iptalPrimTutari`, `odenecekVergi`, `vergiPrimTutari`, `gecmisAyIadeTutari` | **decimal (number)** |
| `sigortaSirketKodu`, `ysvDosyaNo`, `menkulTipi` | **string** |
| `sonOdemeTarihi` | **string** (`yyyy-MM-dd`) |

Diğer kurallar:

- `menkulTipi`: kaynak Excel'de `1`/`2`. SBM'ye **`"MENKUL"` / `"GAYRIMENKUL"`**
  string olarak gönderilir. (1 = MENKUL, 2 = GAYRIMENKUL)
- `gecmisAyIadeTutari`: her `ysvTutarList` **elemanının içinde** yer alır, root'ta değil.
- `ilceKodu`: kaynak veride hiç boş gelmez. **`0` = beyanname büyükşehir (il)
  seviyesinde ödeniyor** demektir → bu durumda alan payload'a **hiç konmaz**
  (`0` gönderilmez). `@JsonInclude(NON_NULL)` bunu sağlar.
- POST (gönder) ile PUT (güncelle) ayrımı `@JsonInclude(NON_NULL)` ile yapılır.
- Silme işlemi yoktur; silme gerekirse tutarlar **0** olarak güncellenir.
- Hata cevapları: **HTTP 422**, gövdede `error.reasons[]`.

### Büyükşehir mantığı

`SbmMapper` içinde. İş birimi kaynak Excel'i büyükşehir/ilçe kırılımına göre
zaten düzenleyerek veriyor. Bu yüzden **`BuyuksehirUtil` içindeki 30 ilin
hardcode listesi ve bloklayıcı validasyon gereksizdir** — veri olduğu gibi
gönderilir. İlgili SBM hataları bilgi amaçlı: `RISK-HAVUZU-00007`
(büyükşehirde ilçe gönderilemez), `RISK-HAVUZU-00008` (büyükşehir değilse ilçe
gönderilmelidir).

### Gruplama

`DeclarationGroupKey` = (**yıl, ay, ilKodu, ilceKodu**).
`ysvDosyaNo` anahtarın parçası **değildir**; grubun satırlarından okunur.

---

## 6. Konfigürasyon ve Helm

Referans proje: **`accounting-services`**. Yapısı birebir örnek alınacak.

```
helm/
  chart/
    common-configs/application.yml      # ortak: logging, jpa, actuator, server
    configs/
      application-sc-test.yml
      application-sc-uat.yml
      application-prep.yml
      application-prod.yml
    templates/
      _helpers.tpl
      common-configmap.yaml
      configmap.yaml
      secret.yaml
    Chart.yaml
    values.yaml
  values/
    sc-test.yaml
    sc-uat.yaml
    prep.yaml
    live.yaml
    dr.yaml
src/main/resources/
  application.yml          # spring.profiles.active ile ortam yönlendirmesi
  application-dev.yml      # vault config import
```

- `Chart.yaml`: `springboot-deployment` chart'ına bağımlı,
  repo `oci://harbor.allianz-tr.local/middleware`, versiyon `1.x.x`,
  condition `springboot-deployment.enabled`.
- `chart/values.yaml`: `global.repoName`, `global.bundleName`,
  `global.overrides` (içinde `esb.server`), podLogger image
  (`harbor.allianz-tr.local/allianz-release/pod-logger:v2`), `app.contextPath`,
  `app.image.repository.group: internal-release`, actuator liveness/readiness probe'ları.
- `configmap.yaml` / `common-configmap.yaml`: `.Files.Glob` ile ilgili klasörü
  tarayıp ConfigMap'e basar.
- `_helpers.tpl`: `allianz.bundlename` tanımı.

### DB bağlantısı — otomatik değil, iki parçalı

1. `helm/values/<ortam>.yaml` içinde `springboot-deployment.secretManager.vault`
   bloğu Vault'tan okuyup env değişkeni export eder:
   ```yaml
   secretManager:
     vault:
       secret: kv/data/UAT
       template: |
         {{ with secret "kv/data/UAT/data-source/<datasource-adi>" -}}
         export SPRING_DATASOURCE_USERNAME="{{ .Data.data.username }}"
         export SPRING_DATASOURCE_PASSWORD="{{ .Data.data.password }}"
         export SPRING_DATASOURCE_URL="{{ .Data.data.url }}"
         {{- end }}
   ```
2. `src/main/resources/application-dev.yml` içinde
   `spring.config.import: vault://` + `spring.cloud.vault.*`
   (host, port 443, scheme https, authentication: token, kv backend,
   `default-context: DEV/apps/<uygulama-adi>`).

Ayrıca `values/<ortam>.yaml` içinde ortam bazlı: `springboot.profile`, `sysType`,
JVM args, cpu/memory limits, `autoscale.minReplicas/maxReplicas`.

---

## 7. Veritabanı

Script: `YSV-db-scripti.sql` (v2.1, sıfırdan CREATE, DROP içermez).
Şema: `CUSTOMER`, public synonym'lerle.

Tablolar:
- `ALZ_SBM_MUNICIPALITY` — belediye referans (CITY_CODE, DISTRICT_CODE, KEP_EMAIL, EMAIL, ...)
- `ALZ_SBM_DECL_PROCESS` — beyanname süreç tablosu
- `ALZ_SBM_DECL_LOG` — log tablosu

`ALZ_SBM_DECL_PROCESS` ↔ SBM alan eşlemesi:

| Kolon | SBM alanı |
|---|---|
| `DECLARATION_MONTH` | `ay` |
| `CITY_CODE` | `ilKodu` |
| `DISTRICT_CODE` | `ilceKodu` |
| `COMPANY_CODE` | `sigortaSirketKodu` |
| `PAYMENT_DATE` | `sonOdemeTarihi` |
| `DECLARATION_YEAR` | `yil` |
| `SBM_FILE_NO` | `ysvDosyaNo` |
| `RECEIVED_PREMIUM_AMOUNT` | `alinanPrimTutari` |
| `CANCELLED_PREMIUM_AMOUNT` | `iptalPrimTutari` |
| `MOVABLE_TYPE` (VARCHAR2) | `menkulTipi` |
| `PREV_MONTH_REFUND_AMOUNT` | `gecmisAyIadeTutari` |

Not: `MOVABLE_TYPE_ID NUMBER` → `MOVABLE_TYPE VARCHAR2` migration'ı yapılmıştır.

---

## 8. Loglama

Her gönder / güncelle / sorgu işlemi loglanır. `transactionId` log'larda izlenebilir
olmalı. SBM'den beklenmeyen hata gelirse response header'daki `Transaction-Id`
loglanmalı (SBM destek talebi için gerekiyor).

---

## 9. Kalite kapısı

- `./mvnw clean verify`
- JaCoCo: **LINE ≥ %90, BRANCH ≥ %85, `haltOnFailure=true`**
- Proje henüz **hiç derlenmedi**. İlk derleme Allianz VDI'da (Windows + IntelliJ +
  iç Nexus) yapılacak. Derleme hatalarını normal karşıla.

---

## 10. Bilinen açık konular

- Retry-token URL path'i ve `userName` değeri token ekibiyle (Hüseyin Dağ /
  Ömer Faruk Ceylan) teyit edilecek.
- SBM REST şifresi (`koc` kullanıcısı) ve TEST/PRE/PROD için IP whitelist talebi beklemede.
- Teknik tasarım dökümanı yeni token mimarisine göre güncellenecek.
- Veri anomalisi (iş birimine sorulacak): 14 satır büyükşehir olmasına rağmen ilçe
  kodlu (il 22 Edirne), 2 satır büyükşehir olmadığı halde ilçe kodu 0 (il 47 Mardin).

---

## 11. Tuzaklar — bunları yapma

- ❌ Token cache'i ekleme.
- ❌ `Requester-ID-Type` / `Requester-ID-No` header'larını hardcode etme.
- ❌ SBM dökümanındaki tırnaklı örnek JSON'a göre tüm alanları string yapma.
- ❌ `ilceKodu`'yu büyükşehir için `0` gönderme — alanı tamamen çıkar.
- ❌ ESB için pom'a dependency ekleme.
- ❌ Ortama göre farklı SBM URL'i seçme — ESB tek URL, yönlendirmeyi kendi yapar.
- ❌ `menkulTipi`'ni sayısal gönderme.
- ❌ RestTemplate / WebClient kullanma.
