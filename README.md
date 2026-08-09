# sbm-declaration-services

Yangın Sigorta Vergisi (YSV) beyanname verilerini Allianz Oracle veritabanından okuyup
**ESB üzerinden SBM'ye (Sigorta Bilgi ve Gözetim Merkezi)** ileten Spring Boot 3.3 / Java 21
REST servisi.

| Öğe | Değer |
|---|---|
| Base package | `tr.com.allianz.ysv.services` |
| Main class | `DeclarationServiceApplication` |
| Java | 21 |
| Spring Boot | 3.3.5 |
| Build | Maven (wrapper dahil) |
| HTTP client | Spring `RestClient` (Apache HttpClient 5 üzerinde) |
| DB | Oracle (`ojdbc11`) + Spring Data JPA, tüm profillerde |
| Context path | `/sbm-declaration-services` |

---

## 1. İş akışı

```
OPUS → (manuel SQL script) → CUSTOMER.ALZ_SBM_DECL_PROCESS
                                      │
                          [gönder / güncelle / iptal / sorgu API]
                                      │
              1) alz-token-management'tan TOKEN al (her çağrıda YENİ, cache YOK)
                                      │
              2) ESB (tek URL) üzerinden SBM REST API'ye istek
                                      │
              3) request + response → CUSTOMER.ALZ_SBM_DECL_LOG (yasal kanıt)
                                      │
              4) ALZ_SBM_DECL_PROCESS.STATUS güncelle
```

Uygulama Excel **okumaz**. Veri veritabanına manuel script ile girer (`db/sample_insert.sql`),
uygulama yalnızca tablodan okur.

### Durum makinesi

```
NEW ──(gönder)──▶ PROCESSING ──(SBM 200 + result:true)──▶ SENT ──(sorgu onaylar)──▶ COMPLETED
                       │
                       └──(hata / result:false)──▶ ERROR  (tekrar denenebilir)
```

- **Gönder** (`POST`) yalnızca `NEW` / `ERROR` kayıtları işler.
- **Güncelle** ve **iptal** (`PUT`) yalnızca `SENT` / `COMPLETED` kayıtları işler; başarılı
  güncelleme kaydı yeniden `SENT` yapar (SBM tarafı tekrar doğrulanana kadar).
- **Sorgu** (`GET`) SBM onayı verirse `SENT` kayıtları `COMPLETED` olur.
- Aynı grubun paralel işlenmesi `@Lock(PESSIMISTIC_WRITE)` ile engellenir; her grup kendi
  transaction'ında işlenir, böylece son grubun hatası SBM'nin kabul ettiği grupları geri
  almaz.

### Gruplama

SBM bir beyannameyi **İl - İlçe - Yıl - Ay** ile tanır ve aynı kombinasyon için ikinci bir
kayıt gönderilirse RISK-HAVUZU-00004 döner. Bu yüzden gruplama anahtarı tam olarak bu dört
alandır:

```
GROUP BY (DECLARATION_YEAR, DECLARATION_MONTH, CITY_CODE, ilceKodu)
   → tek request
   → gruptaki kayıtlar ysvTutarList elemanı olur (menkul tipi başına bir eleman)
```

- `ilceKodu` grup anahtarında **SBM'nin göreceği** değerdir: büyükşehirde `null`, ilçe kodu
  yoksa (`null` veya `0`) yine `null`. Böylece bir ilin geçersiz satırları tek grupta
  toplanır ve topluca tek bir hata alır.
- **`ysvDosyaNo` grup anahtarında yer almaz.** Dosya numarasını sigorta şirketi serbestçe
  belirlediği için anahtara konsaydı tek bir yasal beyanname birden fazla isteğe bölünürdü.
  Dosya numarası grubun kayıtlarından okunur; grupta birden fazla farklı dosya numarası
  varsa ilki gönderilir ve uygulama log'una **WARNING** yazılır.
- Aynı grupta aynı `menkulTipi` iki kez varsa **istek atılmadan önce** RISK-HAVUZU-00005 ile
  hata verilir.

### Büyükşehir kuralı

- İl büyükşehirse → `ilceKodu` **gönderilmez** (DB'de `0` veya dolu olsa bile).
- Büyükşehir değilse → `ilceKodu` **zorunlu**; yoksa veya `0` ise istek atılmaz, kayıt
  `ERROR` olur (RISK-HAVUZU-00008).

---

## 2. Allianz VDI Kurulumu

Proje yalnızca Allianz VDI ortamında (Windows + IntelliJ + iç Nexus + Oracle + ESB) derlenir
ve çalışır. `tr.com.allianz:ysv-services-rest-client` bağımlılığı yalnızca iç Nexus'ta
yayınlıdır, dışarıdan çözülemez.

### 2.1 Repoyu klonla

```cmd
cd C:\dev\projects
git clone https://github.com/m-ogur34/sbm-declaration-services.git
cd sbm-declaration-services
```

### 2.2 IntelliJ IDEA — Java 21 SDK

1. `File > Project Structure > SDKs` → **Java 21** (Temurin/Oracle JDK 21) ekli olmalı.
2. `File > Project Structure > Project` → *SDK: 21*, *Language level: 21*.
3. `File > Settings > Build, Execution, Deployment > Build Tools > Maven > Runner`
   → *JRE: Project SDK (21)*, *VM Options:* `-Dfile.encoding=UTF-8`.
4. `File > Settings > Editor > File Encodings` → *Global*, *Project* ve *Properties Files*
   için **UTF-8**, "Transparent native-to-ascii conversion" işaretli.
5. `File > Settings > Build, Execution, Deployment > Compiler > Annotation Processors`
   → **Enable annotation processing** (Lombok + MapStruct için zorunlu).
6. Lombok plugin kurulu olmalı.

### 2.3 Nexus `settings.xml` doğrulaması

`%USERPROFILE%\.m2\settings.xml` dosyasının iç Nexus'a yönlendiğini doğrula:

```cmd
type %USERPROFILE%\.m2\settings.xml
```

İçinde şunlar bulunmalı:

- `<mirror>` → Allianz Nexus group repository URL'i (`*` veya `external:*` için)
- `<server>` → Nexus kullanıcı adı/şifresi (ya da token)
- `ysv-services-rest-client` artifact'ının yayınlandığı repository'nin group'a dahil olması

Doğrulama:

```cmd
mvnw.cmd -B dependency:get -Dartifact=tr.com.allianz:ysv-services-rest-client:9e937d1b74890c608e842f1a009e11db43ae57b4
```

Bu komut hatasız biterse Nexus erişimi tamamdır.

> Maven Wrapper varsayılan olarak dağıtımı `repo.maven.apache.org` adresinden indirir.
> VDI'da dışarı çıkış kapalıysa `.mvn/wrapper/maven-wrapper.properties` içindeki
> `distributionUrl` / `wrapperUrl` değerlerini Nexus proxy URL'i ile değiştir veya
> `MVNW_REPOURL` ortam değişkenini Nexus'a ayarla.

### 2.4 Derleme ve test

```cmd
mvnw.cmd clean verify
```

Bu komut derler, testleri çalıştırır, JaCoCo raporunu üretir ve kapsam eşiğini kontrol eder.
Rapor: `target/site/jacoco/index.html`.

### 2.5 Lokal çalıştırma (dev profili)

`dev` profili de Oracle kullanır (H2 yoktur). Bağlantı bilgileri ortam değişkenlerinden
okunur:

```cmd
set SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//opusuat-scan.allianz-tr.local:1521/OPSSCUAT
set SPRING_DATASOURCE_USERNAME=<kullanici>
set SPRING_DATASOURCE_PASSWORD=<sifre>
mvnw.cmd spring-boot:run
```

- Swagger UI: <http://localhost:8080/sbm-declaration-services/swagger-ui.html>
- Health: <http://localhost:8080/sbm-declaration-services/actuator/health>

### 2.6 Veritabanı

```sql
-- 1) Şema (verilen script, değiştirilmedi)
@db/setup_db.sql
-- 2) Excel'den türetilmiş 50 satırlık örnek veri (25 grup)
@db/sample_insert.sql
```

---

## 3. Test kapsamı (coverage)

`jacoco-maven-plugin` `verify` fazında raporu üretir ve eşiği kontrol eder.
`haltOnFailure=true` olduğu için eşik altındaki build **kırılır**.

| Seviye | Sayaç | Minimum |
|---|---|---|
| BUNDLE | LINE | **%90** |
| BUNDLE | BRANCH | **%85** |

Kapsam dışı bırakılanlar:

- `DeclarationServiceApplication`
- `dto/**`
- `entity/**`
- `config/*Properties` (property sınıfları ve iç sınıfları)
- `exception/ErrorResponse`
- MapStruct'ın ürettiği `*MapperImpl`

Lombok'un ürettiği getter/setter/builder/equals/hashCode kodu `lombok.config` içindeki
`lombok.addLombokGeneratedAnnotation = true` sayesinde JaCoCo tarafından sayılmaz.

Testler DB gerektirmez: repository'ler Mockito ile, REST çağrıları
`MockRestServiceServer` ile taklit edilir. `@DataJpaTest` kullanılmaz.

> Not: `prepare-agent` hedefi `initialize` fazına bağlıdır. `test` fazına bağlansaydı Maven,
> aynı fazdaki surefire'ı yaşam döngüsü sırasına göre **önce** çalıştırabilir ve ajan
> yüklenmeden test koştuğu için kapsam 0 çıkardı. `report` ve `check` `verify` fazındadır.

---

## 4. REST API

Base path: `/api/v1/declarations`
Tetikleyen kullanıcı `X-User-Name` header'ından okunur (yoksa `SYSTEM`).

| Metot | Path | Açıklama |
|---|---|---|
| `POST` | `/send` | Filtreye uyan `NEW`/`ERROR` kayıtları SBM'ye POST eder |
| `PUT` | `/update` | Filtreye uyan `SENT`/`COMPLETED` kayıtları SBM'de günceller |
| `POST` | `/cancel` | Tutarları 0'layarak PUT eder (SBM'de silme yoktur) |
| `GET` | `/query/{ysvDosyaNo}` | SBM'den sorgular, onaylanırsa `COMPLETED` yapar |
| `GET` | `/processes` | Sayfalı liste (status, yıl, ay, il filtreli) |

İstek gövdesi (send / update / cancel):

```json
{ "year": 2026, "month": 1, "cityCode": null, "processIds": [] }
```

`processIds` doluysa diğer filtreler dikkate alınmaz.

Toplu işlem sonucu:

```json
{ "totalGroups": 120, "successCount": 118, "failCount": 2,
  "failures": [ { "ysvDosyaNo": "...", "errorCode": "...", "message": "..." } ] }
```

Hata gövdesi (tüm hatalar için tek tip):

```json
{ "timestamp": "...", "path": "...", "code": "...", "message": "...", "details": [] }
```

Örnek istekler: [`docs/api-examples.http`](docs/api-examples.http)

---

## 5. Profiller ve konfigürasyon

| Profil | Konum | Not |
|---|---|---|
| `dev` | `src/main/resources/application-dev.yml` | Lokal geliştirme, Oracle |
| `sc-test` | `helm/charts/configs/application-sc-test.yml` | |
| `sc-uat` | `helm/charts/configs/application-sc-uat.yml` | |
| `prep` | `helm/charts/configs/application-prep.yml` | |
| `prod` | `helm/charts/configs/application-prod.yml` | `live.yaml` ve `dr.yaml` kullanır |

Ortak Spring ayarları `helm/charts/common-configs/application.yml` içindedir.

Veritabanı kullanıcı adı/şifre/URL **koda veya yml'e yazılmaz**: Vault template'i
`SPRING_DATASOURCE_URL / USERNAME / PASSWORD` ortam değişkenlerini export eder, Spring
bunları otomatik okur (`helm/values/<ortam>.yaml`).

```
SC-UAT : jdbc:oracle:thin:@//opusuat-scan.allianz-tr.local:1521/OPSSCUAT
SC-PREP: jdbc:oracle:thin:@//opusprep-scan.allianz-tr.local:1521/opusprep
SC-PROD: jdbc:oracle:thin:@//opusprod-scan.allianz-tr.local:1453/OPUSAUX
Çıkış IP (SBM whitelist): 195.87.49.10
```

### ESB ve token

Uygulama SBM adreslerine **doğrudan gitmez**; tüm istekler tek ESB URL'ine gider, ortam
yönlendirmesini ESB yapar.

```yaml
esb:
  base-url: http://esb.allianz.com.tr:12000
  ysv:
    beyanname-path: /api/rest/vergi-beyan-rs/v10/ysv-beyanname
    sorgu-path: /api/rest/vergi-beyan-rs/v10/ysv-beyanname/sorgu
    sorgu-method: GET
token-management:
  base-url: https://int-sc-test-auth.allianz.com.tr
  path: /alz-token-management/api/v1/tokens/sbm-token-generate
sbm:
  company-code: "045"
  retry:
    max-attempts: 2          # yalnızca SEC-00002 ve 5xx için
```

Her istekten önce yeni token alınır (**cache yoktur**). SBM'ye giden header'lar token
yanıtından üretilir:

| Header | Kaynak |
|---|---|
| `Authorization` | `Bearer <accessToken>` |
| `Requester-ID-Type` | `clientCredentials.clientIdentityType` |
| `Requester-ID-No` | `clientCredentials.clientIdNumber` |

`accessToken` ve `clientIdNumber` log'a **asla maskesiz** yazılmaz (ilk 10 karakter + `***`).
`Authorization` header'ı `ALZ_SBM_DECL_LOG` payload'ına yazılmaz.

---

## 6. SBM hata kodları

| Kod | Anlamı | Aksiyon |
|---|---|---|
| `RISK-HAVUZU-00002` | Başka şirket adına işlem yapılamaz | `sigortaSirketKodu` = 045 kontrolü |
| `RISK-HAVUZU-00003` | İlgili ay veri girişine kapalı | Retry etme, `ERROR` |
| `RISK-HAVUZU-00004` | Mükerrer beyanname (il-ilçe-yıl-ay) | Retry etme |
| `RISK-HAVUZU-00005` | Mükerrer menkul tipi | Gruplama hatası, gönderim öncesi yakalanır |
| `RISK-HAVUZU-00006` | İl bulunamadı | Veri hatası |
| `RISK-HAVUZU-00007` | Büyükşehirde ilçe gönderilemez | `ilceKodu` gönderilmez |
| `RISK-HAVUZU-00008` | Büyükşehir değilse ilçe gönderilmelidir | `ilceKodu` zorunlu |
| `RISK-HAVUZU-00009` | İlçe bulunamadı | Veri hatası |
| `SEC-00001` | Kimlik doğrulama bilgileri gönderilmelidir | Token boş |
| `SEC-00002` | Token geçersiz/süresi dolmuş | **1 kez** yeni token alıp retry |
| `SEC-00003` | Erişim izni yok | Retry etme |
| `SEC-00004` | IP doğrulanamadı | Retry etme, whitelist sorunu |
| `SEC-00005..08` | Header hataları | Retry etme |
| `CORE-00000` | Beklenmeyen hata | `Transaction-Id` ile SBM'ye destek talebi |
| `CORE-00001` | HTTP metodu desteklenmiyor | Endpoint/metot kontrolü |
| `CORE-00005/00006` | Format hatası | Veri hatası |
| `CORE-00009` | Kaynak bulunamadı | Endpoint yanlış |
| `CORE-01000` | Zorunlu alan boş | Veri hatası |
| `CORE-01001` | Kayıt bulunamadı | Sorguda normal |
| `CORE-01004/01008` | Değer aralığı / uzunluk hatası | Veri hatası |

`reasons[]` listesi birleştirilip `ALZ_SBM_DECL_PROCESS.ERROR_DETAILS` alanına yazılır
(2000 karakteri aşarsa kırpılır) ve kayıt `ERROR` olur. Yanıt header'ındaki
**`Transaction-Id`** her çağrıda loglanır — SBM destek talebinde bu isteniyor.

**Veri silme:** SBM'de silme yoktur. `POST /cancel` tüm tutar alanlarını `0` yaparak `PUT`
gönderir.

---

## 7. Proje yapısı

```
src/main/java/tr/com/allianz/ysv/services/
├── DeclarationServiceApplication.java
├── config/         RestClientConfig, EsbProperties, TokenManagementProperties,
│                   SbmProperties, OpenApiConfig
├── controller/     DeclarationController
├── dto/            request/, response/, internal/
├── entity/         DeclarationProcess, DeclarationLog, Municipality
├── enums/          ProcessStatus, MovableType, OperationType, LogLevel, SbmErrorCode
├── exception/      SbmIntegrationException, TokenException, GlobalExceptionHandler,
│                   ErrorResponse
├── mapper/         SbmMapper, ProcessMapper
├── repository/     DeclarationProcessRepository, DeclarationLogRepository,
│                   MunicipalityRepository
├── service/        DeclarationService, DeclarationGroupProcessor, SbmClientService,
│                   TokenManagementService, DeclarationLogService
└── util/           BuyuksehirUtil, DateUtil, JsonUtil, MaskUtil

db/       setup_db.sql (değiştirilmedi), sample_insert.sql (50 satır)
docs/     api-examples.http
helm/     charts/ (Chart.yaml, values.yaml, templates/, configs/, common-configs/)
          values/ (sc-test, sc-uat, prep, live, dr)
```

---

## 8. Açık Konular

1. **Sorgu metodu `GET` mi `POST` mu?** SBM dökümanı `ysv-beyanname/sorgu` için "GET"
   diyor ama gövdeli bir request örneği veriyor. `esb.ysv.sorgu-method` property'si ile
   `GET`/`POST` arasında değiştirilebilir bırakıldı; `SbmClientService` iki varyantı da
   destekliyor. SBM/ESB ekibinden teyit alınmalı.
2. **ESB path'leri.** ESB'nin SBM path'lerini birebir mi proxy'lediği, yoksa kendi path'ini
   mi beklediği teyit edilmedi. Path'ler property'den yönetiliyor
   (`esb.ysv.beyanname-path`, `esb.ysv.sorgu-path`), hard-code edilmedi.
3. **`gecmisAyIadeTutari`.** Alan SBM'nin dokümante ettiği alan listesinde yok. DB'de değer
   varsa gönderiliyor, yoksa `@JsonInclude(NON_NULL)` ile payload'dan çıkarılıyor. SBM
   tarafında karşılığı olup olmadığı teyit edilmeli.
4. **Token `functionName` değeri.** Örnek istekte `"test"` geçiyor. Uygulama operasyona göre
   `ysv-beyanname-gonder` / `-guncelle` / `-sorgu` gönderiyor; alz-token-management ekibinden
   beklenen değer teyit edilmeli.
5. **PROD token servisi base URL'i.** Elimizde yalnızca TEST adresi
   (`https://int-sc-test-auth.allianz.com.tr`) var. `application-prep.yml` ve
   `application-prod.yml` şimdilik bu adresi taşıyor; ilk üretim koşusundan önce
   değiştirilmeli.
6. **Büyükşehir il listesi OPUS verisiyle doğrulanmalı.** Analizle verilen 30 il kodu listesi
   `BuyuksehirUtil` içinde birebir kullanıldı, ancak OPUS extract'i (548 satır / 274 grup)
   bu listeyle iki noktada çelişiyor:
   - **Büyükşehir listesinde olduğu halde ilçe kodu dolu 14 satır** — hepsi il 22 (Edirne).
     Liste doğruysa SBM'ye ilçe gönderilmemeli; veri doğruysa Edirne listeden çıkmalı.
     Aksi halde `RISK-HAVUZU-00007` beklenir.
   - **Büyükşehir listesinde olmadığı halde ilçe kodu 0 olan 2 satır** — il 47 (Mardin).
     Mardin gerçekte büyükşehir olduğu için listede eksik görünüyor; düzeltilmezse bu
     satırlar `RISK-HAVUZU-00008` ile `ERROR` statüsüne düşer.

   `db/sample_insert.sql` içine bu iki il bilinçli olarak alınmadı. Liste iş birimiyle
   netleştirilmeli.
7. **Büyükşehirlerde ilçe bazlı satırlar il bazında toplanmalı mı?** SBM büyükşehirler için
   il bazında tek beyanname bekliyor. OPUS'ta aynı büyükşehir için birden fazla ilçe satırı
   geldiğinde iki seçenek var:
   - satırlar il bazında **toplanır** (tutarlar menkul tipi bazında sum'lanır), ya da
   - her ilçe ayrı gönderilir ve SBM `RISK-HAVUZU-00004` (mükerrer beyanname) döner.

   Mevcut kod **hiçbir tutarı kendiliğinden toplamıyor**: satırlar il bazında tek gruba
   düşüyor, aynı menkul tipi birden fazla kez geldiği için gönderim öncesi
   `RISK-HAVUZU-00005` ile hata veriliyor ve kayıtlar `ERROR` statüsüne düşüyor. Yani hata
   sessizce yanlış veri göndermek yerine görünür oluyor. Toplama kuralının uygulanıp
   uygulanmayacağı (ve toplanacaksa `sonOdemeTarihi` / `ysvDosyaNo` için hangi satırın esas
   alınacağı) iş birimiyle netleştirilmeli.
