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

- `ilceKodu` grup anahtarında **SBM'nin göreceği** değerdir: payload ile aynı
  normalizasyondan geçer, yani `0` ve `null` aynı anlama gelir ve tek grupta toplanır.
- **`ysvDosyaNo` grup anahtarında yer almaz.** Dosya numarasını sigorta şirketi serbestçe
  belirlediği için anahtara konsaydı tek bir yasal beyanname birden fazla isteğe bölünürdü.
  Dosya numarası grubun kayıtlarından okunur; grupta birden fazla farklı dosya numarası
  varsa ilki gönderilir ve uygulama log'una **WARNING** yazılır.
- Aynı grupta aynı `menkulTipi` iki kez varsa **istek atılmadan önce** RISK-HAVUZU-00005 ile
  hata verilir.

### İlçe kodu kuralı

Uygulamada büyükşehir listesi ve ilçe doğrulaması **yoktur**. Tek kural:

- `DISTRICT_CODE` **null veya 0** ise → `ilceKodu` alanı JSON gövdesine **hiç yazılmaz**
  (`null` + `@JsonInclude(NON_NULL)`). `"ilceKodu": 0` asla gönderilmez.
- Diğer tüm durumlarda DB'deki değer **olduğu gibi** gönderilir.

Kaynak Excel iş birimi tarafından büyükşehir ve bağlı ilçeler bazında düzenleniyor; hatalı
il/ilçe kombinasyonlarını SBM zaten `RISK-HAVUZU-00007` / `RISK-HAVUZU-00008` ile bildiriyor
ve bu cevap `ERROR_DETAILS`'e yazılıyor. Aynı kararı lokalde tekrar vermek, iş birimince
doğru kabul edilen kayıtları gereksiz yere bloklardı.

---

## 2. Allianz VDI Kurulumu

Proje Allianz VDI ortamında (Windows + IntelliJ + iç Nexus + Oracle + ESB) derlenir ve
çalışır. Bağımlılıklar iç Nexus üzerinden çözülür; projede Nexus'a özel bir artifact
bağımlılığı yoktur.

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

Doğrulama:

```cmd
mvnw.cmd -B dependency:resolve
```

Bu komut hatasız biterse Nexus erişimi tamamdır. Projede iç Nexus'a özel bir artifact
bağımlılığı yoktur; tüm bağımlılıklar Maven Central'ın Nexus proxy'sinden çözülür.

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
| `sc-test` | `helm/chart/configs/application-sc-test.yml` | |
| `sc-uat` | `helm/chart/configs/application-sc-uat.yml` | |
| `prep` | `helm/chart/configs/application-prep.yml` | |
| `prod` | `helm/chart/configs/application-prod.yml` | `live.yaml` ve `dr.yaml` kullanır |

**Ortak ayarlar** `helm/chart/common-configs/application.yml` içindedir: context-path,
logging seviyeleri, `spring.jpa`, Oracle driver, actuator endpoint'leri, ESB path'leri ve
timeout'ları, `sbm.company-code` ve retry ayarı.

`configs/application-<env>.yml` dosyalarında ortama özgü olanlar bulunur:
`spring.application.name`, ESB base URL'i, log seviyesi ve **`token-management` bloğunun
tamamı**. Token ayarlarının hiçbiri ortak config'de tutulmaz.

`src/main/resources` altında sadece `application.yml` ve `application-dev.yml` vardır.

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

**ESB entegrasyonu için Nexus'tan artifact çekilmiyor; ESB ortam URL'ine doğrudan
`RestClient` ile istek atılıyor (teyit edildi).** `pom.xml`'de ESB'ye ait hiçbir bağımlılık
yoktur.

```yaml
esb:
  base-url: ${ESB_SERVER:http://esb.allianz.com.tr:12000}
  ysv:
    beyanname-path: /api/rest/vergi-beyan-rs/v10/ysv-beyanname
    sorgu-path: /api/rest/vergi-beyan-rs/v10/ysv-beyanname/sorgu
    sorgu-method: GET
sbm:
  company-code: "045"
  retry:
    max-attempts: 2          # yalnızca SEC-00002 ve 5xx için
```

ESB adresi **koda gömülü değildir**. `helm/values/<ortam>.yaml` içindeki
`global.overrides.esb.server` değeri pod'a `ESB_SERVER` ortam değişkeni olarak geçer ve
yukarıdaki `${ESB_SERVER:...}` ifadesi bunu okur. `esb.allianz.com.tr` için her ortamda DNS
kaydı bulunmayabildiğinden (legacy SOAP client pom'unda bu not ve UAT için `10.70.52.149`
IP'si var) adres IP ile override edilebilir.

#### Token parametreleri ortam bazlıdır

`token-management` ayarlarının **tamamı** ortama özgüdür ve yalnızca
`helm/chart/configs/application-<profil>.yml` içinde bulunur — ortak config'de
`token-management` başlığı **yoktur**, kodda da hiçbir varsayılan değer tutulmaz.

```yaml
token-management:
  base-url: https://int-sc-test-auth.allianz.com.tr   # yalnızca SC-TEST'te bilinen değerler
  path: /alz-token-management/api/v1/tokens/sbm-token-generate
  client-name: ysv
  user-name: WDA2422_16178
  company-code: "045"
  connect-timeout: 5s
  read-timeout: 30s
```

`TokenManagementProperties` sınıfı `@Validated` ve zorunlu alanları `@NotBlank`. Bir profilde
bu parametreler doldurulmamışsa **uygulama başlangıçta hata verir ve ayağa kalkmaz**; eksiklik
ilk beyanname gönderiminde değil, deploy anında görünür. `sc-uat`, `prep` ve `prod`
dosyalarında anahtarlar bilerek boş bırakıldı (bkz. "Açık Konular" #5).

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
└── util/           DistrictCodeResolver, DateUtil, JsonUtil, MaskUtil

db/       setup_db.sql (değiştirilmedi), sample_insert.sql (50 satır)
docs/     api-examples.http
```

### Helm ağacı

```
helm/
├── chart/
│   ├── common-configs/application.yml
│   ├── configs/
│   │   ├── application-prep.yml
│   │   ├── application-prod.yml
│   │   ├── application-sc-test.yml
│   │   └── application-sc-uat.yml
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

`chart/Chart.yaml` Allianz `springboot-deployment` chart'ını
(`oci://harbor.allianz-tr.local/middleware`, `1.x.x`) bağımlılık olarak alır. ConfigMap
template'leri `.Files.Glob` ile `common-configs/` ve `configs/` klasörlerini okur; kaynak
adları `_helpers.tpl` içindeki `allianz.bundlename` helper'ından üretilir.

Vault yolları: `sc-test → kv/data/TEST`, `sc-uat → kv/data/UAT`, `prep → kv/data/PREP`,
`live` ve `dr → kv/data/PROD`. `live`/`dr` için `-Xms4g -Xmx4g`, memory limit `6Gi`,
`minReplicas: 1`, `maxReplicas: 5`.

> ⚠️ Klasör adının `chart` mı `charts` mı olduğu Jenkins pipeline ile teyit edilmeli.
> Bu repo `accounting-services` projesindeki gibi tekil `chart` kullanıyor; `Jenkinsfile`
> içindeki `helm upgrade` komutu da bu yolu kullanıyor.

---

## 8. Açık Konular

1. **Sorgu metodu `GET` mi `POST` mu?** SBM dökümanı `ysv-beyanname/sorgu` için "GET"
   diyor ama gövdeli bir request örneği veriyor. `esb.ysv.sorgu-method` property'si ile
   `GET`/`POST` arasında değiştirilebilir bırakıldı; `SbmClientService` iki varyantı da
   destekliyor. SBM/ESB ekibinden teyit alınmalı.
2. **`gecmisAyIadeTutari`.** Güncel SBM dökümanında alan `ysvTutarList`'in her elemanında
   yer alıyor; kod da onu tutar kalemine (`SbmAmountItem`) koyuyor, root'a değil. DB'de
   değer varsa gönderiliyor, yoksa `@JsonInclude(NON_NULL)` ile payload'dan çıkarılıyor.
   Gerçek bir gönderimle uçtan uca doğrulanmadı.
3. **Token `functionName` değeri.** Örnek istekte `"test"` geçiyor. Uygulama operasyona göre
   `ysv-beyanname-gonder` / `-guncelle` / `-sorgu` gönderiyor; alz-token-management ekibinden
   beklenen değer teyit edilmeli.
4. **alz-token-management parametreleri.** Parametrelerin **tamamı** (`base-url`, `path`,
   `client-name`, `user-name`, `company-code`) ortam bazlıdır ve
   `helm/chart/configs/application-<profil>.yml` içinde tutulur; ortak config'de
   `token-management` başlığı hiç yoktur. Yalnızca **SC-TEST** değerleri biliniyor;
   `sc-uat`, `prep` ve `prod` değerleri token ekibinden alınacak. O profillerde anahtarlar
   boş bırakıldı — değer uydurulmadı. `TokenManagementProperties` `@Validated` +
   `@NotBlank` olduğu için eksik ayarla uygulama **başlangıçta hata verir ve ayağa
   kalkmaz**; sorun ilk beyanname gönderiminde değil, deploy anında görünür.
5. **Alan tipleri: number mı string mi?** SBM dökümanının alan türü tablosu, PDF'deki istek
   örneği, sorgu yanıt örneği ve legacy SOAP WSDL stub'ları sayısal alanların JSON
   **number** olduğunu doğruluyor. Dökümanın güncellenmiş istek örneğinde değerler tırnaklı
   gösteriliyor; bu, tip tablosuyla çelişen bir örnek olarak değerlendirildi ve number
   tercih edildi. `CORE-00005` alınırsa çözüm: ilgili alanlara
   `@JsonFormat(shape = JsonFormat.Shape.STRING)` eklemek — DTO'lar tek noktada
   (`SbmDeclarationRequest`, `SbmAmountItem`) olduğu için tek satırlık bir değişiklik.
6. **İlçe kodu doğrulaması uygulamada yapılmıyor.** Kaynak Excel iş birimi tarafından
   düzenleniyor ve hatalı kayıtlar SBM'nin `RISK-HAVUZU-00007` / `RISK-HAVUZU-00008`
   hatalarıyla yakalanıyor. Uygulamada büyükşehir listesi tutulmuyor; tek kural
   `DISTRICT_CODE` null/0 ise alanın gönderilmemesi.
7. **ESB path namespace'i.** ESB kendi path namespace'ini kullanıyor olabilir: legacy SOAP'ta
   `YsvServices/ProxyService/YsvBeyanService` deseni kullanılmış. Bizim mevcut path'lerimiz
   SBM'nin kendi path'i (`/api/rest/vergi-beyan-rs/v10/ysv-beyanname`); ESB ekibinden teyit
   edilmeli, aksi halde `CORE-00009` (kaynak bulunamadı) alınır. Path'ler property'den
   yönetildiği için (`esb.ysv.beyanname-path`, `esb.ysv.sorgu-path`) kod değişikliği
   gerekmez.
8. **ESB DNS kaydı.** Legacy SOAP client pom'unda "esb.allianz.com.tr olarak bir dns kaydı
   bulunmamakta" notu ve UAT için `10.70.52.149` IP'si var. Adres bu yüzden
   `${ESB_SERVER:...}` üzerinden okunuyor ve `helm/values/<ortam>.yaml` içindeki
   `global.overrides.esb.server` ile IP olarak override edilebiliyor. Her ortamda hangi
   adresin geçerli olduğu ESB ekibinden alınmalı.
9. **`global.overrides.esb.server` → `ESB_SERVER` eşlemesi — DOĞRULANMAMIŞ VARSAYIM.**
    Uygulama `esb.base-url` değerini `${ESB_SERVER:...}` ifadesiyle okuyor, yani pod'da
    `ESB_SERVER` adında bir ortam değişkeni bekliyor. `helm/chart/values.yaml` içindeki
    `global.overrides.esb.server` alanını Allianz `springboot-deployment` subchart'ının bu
    ortam değişkenine çevirdiği **varsayıldı**; subchart `oci://harbor.allianz-tr.local/middleware`
    üzerinden geldiği ve bu repoda kaynağı bulunmadığı için sözleşmesi görülemedi.
    Middleware ekibiyle doğrulanmalı.

    Varsayım tutmazsa uygulama kodu değişmez, yalnızca `helm/values/<ortam>.yaml`
    değişir: değişken `app.env` altında doğrudan tanımlanır. Her values dosyasında bunun
    yorum satırı hâlinde örneği duruyor:

    ```yaml
    springboot-deployment:
      app:
        env:
          # Anahtar adı subchart sözleşmesine göre custom / variables / extraEnv olabilir.
          custom:
            - name: ESB_SERVER
              value: http://10.70.52.149:12000
    ```

    Bu durumda `global.overrides.esb.server` alanı kullanılmadan kalır; iki yöntem aynı anda
    kullanılmamalı.
