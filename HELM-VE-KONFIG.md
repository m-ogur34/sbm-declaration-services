# Helm, Profil ve Konfigürasyon — Nasıl Çalışıyor

> Amaç: `sbm-declaration-services` uygulamasının konfigürasyonunun (özellikle
> **DB bağlantısının**) her ortamda nereden geldiğini adım adım anlatmak.
> Helm'e yeni başlayan biri için yazıldı.

---

## 0. Kısa cevap (acelesi olanlar için)

- **`src/main/resources/application.yml`'i ortamlara göre elle DEĞİŞTİRMİYORSUN.**
  Aktif profil, k8s'te Helm tarafından `SPRING_PROFILES_ACTIVE` ortam değişkeni ile
  enjekte ediliyor. Lokalde bu değişken yok → `dev` profili devreye giriyor.
- **DB kullanıcı adı / şifre / URL koda veya git'e yazılmıyor.** Vault'tan geliyor.
  İki yol var: lokalde uygulama Vault'u kendisi okuyor; k8s'te Vault ajanı okuyup
  ortam değişkeni olarak veriyor.

---

## 1. Konfigürasyon katmanları

Uygulama ayarları 5 kaynaktan birleşir. Aşağı satır yukarıyı **ezer**:

| # | Kaynak | Nerede | Ne içerir | Image'a gömülü mü? |
|---|---|---|---|---|
| 1 | `application.yml` | `src/main/resources/` | Tüm ortamların ortak temeli: port, context-path, jackson, JPA, actuator | Evet |
| 2 | `application-<profil>.yml` | `src/main/resources/` | Sadece `dev` (lokal) ve ileride `local`. Vault host'u, `show-sql` | Evet |
| 3 | `common-configs/application.yml` | `helm/chart/` | k8s ortamlarının **ortak** ek ayarı: datasource pool, `esb.ysv.*` path'leri, `sbm.company-code`, log | Hayır — ConfigMap olarak mount |
| 4 | `configs/application-<profil>.yml` | `helm/chart/` | **Ortama özel**: `token-management.*` (base-url, path, user-name...), `esb.base-url` | Hayır — ConfigMap olarak mount |
| 5 | Ortam değişkenleri | Pod'a Helm/Vault enjekte eder | `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `ESB_SERVER` | — |

**Neden bu kadar katman?**
- 1–2 uygulamanın kendi varsayılanları; internetsiz/Helm'siz de (lokal) çalışsın diye.
- 3–4 operasyon ekibinin image'ı yeniden derlemeden değiştirebildiği ayarlar.
- 5 sır (şifre) ve gerçekten ortamdan ortama değişen tek tük değer.

`token-management.*` **bilerek** `application.yml`'de ve `common-configs`'te YOK.
Çünkü hepsi ortam bazlı ve `TokenManagementProperties` `@NotBlank` — eksik bırakılan
bir ortamda uygulama **ilk SBM çağrısında değil, açılışta** patlar (erken hata =
kolay teşhis).

---

## 2. Aktif profil nasıl belirleniyor

`application.yml` içinde:

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

Okunuşu: "`SPRING_PROFILES_ACTIVE` ortam değişkeni **varsa** onu kullan, **yoksa**
`dev`."

| Ortam | `SPRING_PROFILES_ACTIVE` | Aktif profil | Devreye giren `application-<profil>.yml` |
|---|---|---|---|
| Lokal (senin Mac / IntelliJ) | tanımsız | `dev` | `src/main/resources/application-dev.yml` |
| SC-TEST | `sc-test` | `sc-test` | `helm/chart/configs/application-sc-test.yml` |
| SC-UAT | `sc-uat` | `sc-uat` | `.../application-sc-uat.yml` |
| PREP | `prep` | `prep` | `.../application-prep.yml` |
| PROD / DR | `prod` | `prod` | `.../application-prod.yml` |

Bu değişkeni kim koyuyor? → `helm/values/<ortam>.yaml` içindeki:

```yaml
springboot-deployment:
  app:
    env:
      springboot:
        profile: sc-test      # <-- burası
```

`springboot-deployment` alt-chart'ı (OCI registry'den geliyor,
`oci://harbor.allianz-tr.local/middleware`) bu değeri alıp pod'a
`SPRING_PROFILES_ACTIVE=sc-test` olarak veriyor.

> ⚠️ **Doğrulanmamış varsayım (README "Açık Konular" #11):** Alt-chart'ın kaynağı
> incelenemedi. `app.env.springboot.profile` → `SPRING_PROFILES_ACTIVE` eşlemesini
> yaptığı **varsayıldı**. VDI'da `kubectl exec <pod> -- env | grep SPRING` ile
> doğrula. Yapmıyorsa `app.env` altına `custom: [{name: SPRING_PROFILES_ACTIVE,
> value: sc-test}]` gibi doğrudan tanımla.

**Cevap (Soru 4):** Hayır, `application.yml`'deki satırı elle değiştirmene gerek
yok. Ortam farkını `helm/values/<ortam>.yaml` → `springboot.profile` yapıyor.

---

## 3. ConfigMap'ler Spring'e nasıl ulaşıyor

`helm/chart/templates/` altında iki şablon var:

- `common-configmap.yaml` → `helm/chart/common-configs/` altındaki her dosyayı
  `<bundle>-common-config` adlı bir ConfigMap'e koyar.
- `configmap.yaml` → `helm/chart/configs/` altındaki **tüm** profil dosyalarını
  `<bundle>-config` adlı ConfigMap'e koyar (hepsi mount edilir; Spring sadece aktif
  profilinkini işler).

Bu ConfigMap'ler pod'a bir dizin olarak bağlanır (tipik olarak `/config` veya
`/workspace/config`) ve Spring Boot o dizindeki `application*.yml` dosyalarını
otomatik yükler. **Bu bağlama (mount path + Spring'e tanıtma) işini alt-chart
yapıyor** — yine alt-chart sözleşmesine bağlı; VDI'da `kubectl describe pod` ile
volume mount'u ve `SPRING_CONFIG_*` env'lerini kontrol et.

Yükleme sırası (Spring Boot precedence, sonraki öncekini ezer):
`application.yml` (jar) → `application-<profil>.yml` (jar) →
`/config/application.yml` (common ConfigMap) →
`/config/application-<profil>.yml` (ortam ConfigMap) → ortam değişkenleri.

---

## 4. DB bağlantısı — uçtan uca

`common-configs/application.yml` datasource'un **sadece** sabit kısmını verir:

```yaml
spring:
  datasource:
    driver-class-name: oracle.jdbc.OracleDriver
    hikari: { pool-name: sbm-declaration-pool, maximum-pool-size: 10, ... }
```

`url`, `username`, `password` **yok**. Onlar Vault'tan gelir. Ortama göre 3 senaryo:

### 4.a Lokal (`local` profili — eklenecek)

`application-local.yml`:
```yaml
spring:
  cloud:
    vault:
      enabled: false
  datasource:
    url: jdbc:oracle:thin:@//localhost:1521/XEPDB1
    username: ysv
    password: ysv
```
Vault yok, düz metin — sadece kendi makinende. `db/local/local_setup.sql` ile
kurduğun şemaya bağlanır.

### 4.b Dev (`dev` profili — VDI'da lokal çalıştırma)

`application-dev.yml`:
```yaml
spring:
  config:
    import: vault://
  cloud:
    vault:
      host: int-vault-local-test.allianz.com.tr
      port: 443
      scheme: https
      authentication: token
      kv: { backend: kv, enabled: true }
      default-context: DEV/apps/sbm-declaration-services
```
Uygulama açılırken **kendisi** Vault'a bağlanır (`spring-cloud-starter-vault-config`,
`local-development` maven profilinde). Vault'taki `kv/DEV/apps/sbm-declaration-services`
altındaki `spring.datasource.url` / `spring.datasource.username` /
`spring.datasource.password` anahtarları doğrudan Spring ayarına geçer.
→ Bunun için Vault token'ı ve VDI/VPN ağı gerekir.

### 4.c k8s (sc-test / sc-uat / prep / prod)

Burada uygulama Vault'a bağlanmaz. **Vault ajanı** (init/sidecar) pod başlarken
`helm/values/<ortam>.yaml` içindeki template'i render eder:

```yaml
secretManager:
  vault:
    secret: kv/data/TEST
    template: |
      {{ with secret "kv/data/TEST/data-source/opusAgencyDataSource" -}}
        export SPRING_DATASOURCE_USERNAME="{{ .Data.data.username }}"
        export SPRING_DATASOURCE_PASSWORD="{{ .Data.data.password }}"
        export SPRING_DATASOURCE_URL="{{ .Data.data.url }}"
      {{- end }}
```

Sonuç: pod içinde `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` **ortam değişkenleri**
oluşur. Spring Boot bu isimleri otomatik olarak `spring.datasource.*` ayarına
bağlar (relaxed binding). `helm/chart/templates/secret.yaml` boş bir placeholder —
şifre orada durmuyor.

| Ortam | Vault path |
|---|---|
| SC-TEST | `kv/data/TEST/data-source/opusAgencyDataSource` |
| SC-UAT | `kv/data/UAT/data-source/opusAgencyDataSource` |
| PREP | `kv/data/PREP/data-source/opusAgencyDataSource` |
| PROD / DR | `kv/data/PROD/data-source/opusAgencyDataSource` |

OPUS Oracle adresleri (referans — `Ortamlarin.DB.Erisim.bilgileri.docx`):
- SC-UAT: `//opusuat-scan.allianz-tr.local:1521/OPSSCUAT`
- SC-PREP: `//opusprep-scan.allianz-tr.local:1521/opusprep`
- SC-PROD: `//opusprod-scan.allianz-tr.local:1453/OPUSAUX`

---

## 5. "DB bağlantıları doğru oluyor mu?" — kontrol listesi

Kod tarafı **doğru kurgulanmış**; şüpheli olan noktalar ortam/altyapı tarafında ve
VDI'da doğrulanmalı:

- [ ] Alt-chart `app.env.springboot.profile` → `SPRING_PROFILES_ACTIVE` yapıyor mu?
      (`kubectl exec <pod> -- env | grep SPRING_PROFILES_ACTIVE`)
- [ ] Alt-chart ConfigMap'leri Spring'in gördüğü bir yere mount ediyor mu?
      (`kubectl describe pod` → Mounts; `env | grep SPRING_CONFIG`)
- [ ] Vault path'leri (`kv/data/<ENV>/data-source/opusAgencyDataSource`) gerçekten
      var mı ve `username/password/url` anahtarlarını içeriyor mu? (Vault ekibi)
- [ ] `dev` profili için Vault context `DEV/apps/sbm-declaration-services` dolu mu?
- [ ] Pod'un OPUS Oracle'a ağ erişimi (firewall/servis-mesh) açık mı?
- [ ] `ojdbc8` + `orai18n` jar'ları image'da mı? (pom'da var, `k8s-profile` repackage
      ile jar'a giriyor)
- [ ] Açılışta log: `HikariPool-1 - Start completed` görülüyor mu?

---

## 6. Deploy komutu (örnek)

```bash
helm upgrade --install sbm-declaration-services ./helm/chart \
  -f helm/values/sc-test.yaml \
  -n <namespace>
```

`Chart.yaml` `springboot-deployment` alt-chart'ına bağımlı; ilk kez öncesinde:
```bash
helm dependency update ./helm/chart
```

DB şeması ayrı: `db/setup_db.sql` (ve gerekirse `db/rollback_db.sql`) ilgili ortamın
Oracle'ında DBA tarafından çalıştırılır — uygulama `ddl-auto: none`, tablo yaratmaz.

---

## 7. Özet diyagram

```
helm/values/<ortam>.yaml
   ├── springboot.profile: sc-test ──► (alt-chart) ──► ENV: SPRING_PROFILES_ACTIVE=sc-test
   ├── secretManager.vault.template ──► (Vault ajanı) ──► ENV: SPRING_DATASOURCE_URL/USERNAME/PASSWORD
   └── global.overrides.esb.server ──► (alt-chart) ──► ENV: ESB_SERVER

helm/chart/common-configs/application.yml   ─┐
helm/chart/configs/application-sc-test.yml   ─┼─► ConfigMap ─► /config/*.yml ─► Spring
src/main/resources/application.yml (jar)     ─┘

Spring Boot birleştirir (env en yüksek öncelik):
   application.yml  <  /config/application.yml  <  /config/application-sc-test.yml  <  ENV
                                                                                      │
   spring.datasource.url/username/password  ◄── SPRING_DATASOURCE_* (ENV) ────────────┘
   spring.profiles.active = sc-test         ◄── SPRING_PROFILES_ACTIVE (ENV)
```
