# UDD - Upravljanje Digitalnim Dokumentima
## Forenzički Izveštaji - Mikroservis za Pretragu i Analitiku

### Arhitektura
```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│   Angular    │────▶│  Spring Boot │────▶│  Elasticsearch   │
│  Frontend    │     │   REST API   │     │  (Full-text +    │
│  (port 4200) │     │  (port 8080) │     │   Geo Search)    │
└──────────────┘     └──────┬───────┘     └──────────────────┘
                            │
                    ┌───────┼───────┐
                    ▼       ▼       ▼
              ┌─────────┐ ┌─────┐ ┌──────────┐
              │  MinIO   │ │Tika │ │PostgreSQL│
              │(port 9000│ │(9998│ │(port 5433│
              │  /9001)  │ │)    │ │)         │
              └──────────┘ └─────┘ └──────────┘

┌──────────────┐     ┌──────────────────┐
│  Logstash    │────▶│  Kibana          │
│ (stats logs) │     │  (port 5601)     │
└──────────────┘     │  - Dashboards    │
                     └──────────────────┘
```

### Potreban Softver
- **Docker & Docker Compose** — to je sve! Ceo sistem se podiže jednom komandom.

> Za lokalni razvoj (opcionalno): Java 17+, Node.js 18+, Maven

---

## Zašto se PDF automatski generiše?

Sistem **automatski generiše PDF** iz podataka forme — nema dupliranja podataka!

**Tok rada:**
1. Popuniš formu (ime, organizacija, malver, opis, grad, koordinate...)
2. Sistem **sam kreira profesionalni PDF** iz tih podataka
3. PDF se čuva u MinIO (object storage)
4. Apache Tika izvlači tekst iz generisanog PDF-a → indeksira u Elasticsearch (`content` polje)
5. Na taj način dobijaš i **strukturiranu pretragu** (po poljima) i **full-text pretragu** (po celom tekstu)

**Zašto je PDF uopšte potreban?**
- MinIO služi kao **trajno skladište** dokumenata — uvek možeš preuzeti originalni izveštaj
- Tika ekstrakcija omogućava **pretragu po celom tekstu** napisanog izveštaja
- PDF je univerzalan format za deljenje i arhiviranje

**Opcioni ručni upload:** Ako već imaš gotov PDF (npr. skeniran dokument, izveštaj treće strane), možeš isključiti auto-generisanje i uploadovati sopstveni fajl.

---

### Pokretanje

#### Jedna komanda — sve radi
```bash
cd UDD
docker compose up -d --build
```

Ovo podiže svih **8 servisa**: Elasticsearch, Kibana, Logstash, MinIO, PostgreSQL, Tika, **Spring Boot backend**, **Angular frontend (nginx)**.

Prvi build traje ~5 minuta (Maven dependencies + npm install). Svaki sledeći je brz zahvaljujući Docker kešu.

#### Zaustavljanje
```bash
docker compose down
```

#### Pristup servisima
| Servis         | URL                          | Opis                          |
|----------------|------------------------------|-------------------------------|
| Frontend       | http://localhost:4200         | Angular SPA                   |
| Backend API    | http://localhost:8080/api     | Spring Boot REST API          |
| Elasticsearch  | http://localhost:9200         | Search engine                 |
| Kibana         | http://localhost:5601         | Vizuelizacija i dashboardi    |
| MinIO Console  | http://localhost:9001         | Object storage (minioadmin/minioadmin) |
| PostgreSQL     | localhost:5433                | DB (`udd_db`, `udd_user`)     |

---

## Tutorijal: prvi test od 0 do rezultata (korak po korak)

### Korak 1: Šta unosiš u formu (frontend /upload)

Otvori `http://localhost:4200` → `Novi Izveštaj`. Popuni:

- Forenzički istražitelj: `Mihajlo Bogdanovic`
- Organizacija: `CyberSec d.o.o.`
- Naziv malvera: `WannaCry-Test`
- Klasifikacija: `Ransomware`
- Grad: `Sombor`
- Latitude: `45.7742`
- Longitude: `19.1122`
- Opis: `Test incident for upload and search validation. IOC: SHA256 abc123-test-hash, C2 domain example-c2.test. Keywords: FTN, Programming, Master Studies, Cloud Deployment`

**Automatsko generisanje PDF-a je uključeno po default-u** — samo klikni `Kreiraj Izveštaj`.

> Sistem će sam napraviti PDF iz ovih podataka, sačuvati ga u MinIO, i indeksirati tekst u Elasticsearch.

### Korak 2: Brzi sanity check posle unosa

- Otvori `Svi Izveštaji` i proveri da je stavka upisana.
- Probaj `Download PDF` dugme da proveriš MinIO putanju.

### Korak 3: Testiraj pretragu (tačno šta da kucaš)

#### A) Prosta pretraga

U `Pretraga` stranici (advanced OFF):

- `FTN`
- `Cloud Deployment`
- `abc123-test-hash`

Očekivanje: dokument se pojavljuje jer je to tekst iz PDF-a (`content`).

#### B) Napredna boolean pretraga

Uključi `Napredna (Boolean) pretraga` i testiraj:

**Pozitivni primeri (TREBA da vrate rezultat):**

```text
classification:Ransomware AND city:Sombor
```

```text
(malwareName:WannaCry-Test OR description:"search validation") AND NOT classification:Spyware
```

```text
forensicInvestigator:"Mihajlo Bogdanovic" AND organization:CyberSec
```

Očekivanje: dokument se vraća za sva 3 upita.

**Kontra-primeri (NE TREBA da vrate rezultat):**

```text
classification:Phishing AND city:Sombor
```
*Nema rezultata — klasifikacija je Ransomware, ne Phishing.*

```text
forensicInvestigator:"Petar Petrovic"
```
*Nema rezultata — taj forenzičar ne postoji u bazi.*

```text
classification:Ransomware AND NOT city:Sombor
```
*Nema rezultata — jedini Ransomware izveštaj JE iz Sombora, pa ga NOT izbacuje.*

```text
malwareName:WannaCry-Test AND classification:DDoS
```
*Nema rezultata — WannaCry-Test je klasifikovan kao Ransomware, ne DDoS.*

```text
city:Beograd OR city:Nis
```
*Nema rezultata — nema izveštaja iz Beograda ni Niša.*

```text
(classification:Trojan OR classification:Worm) AND organization:CyberSec
```
*Nema rezultata — CyberSec ima samo Ransomware, nema Trojan/Worm.*

> Ako kontra-primeri ipak vraćaju rezultate, znači da pretraga ne radi ispravno!

#### C) Fuzzy pretraga (greška u kucanju)

Advanced OFF:

- `Ransmware` (namerno pogrešno)
- `WannaCri-Test`

Očekivanje: zbog `fuzziness=AUTO`, sistem često ipak vrati rezultat.

### Korak 4: Geo pretraga

Idi na `Geo Pretraga`:

- Latitude: `45.7742`
- Longitude: `19.1122`
- Radius: `20`

Klik `Pretraži u radijusu`.

Očekivanje: dokument iz Sombora je pronađen i prikazan marker.

### Korak 5: Kibana testiranje — detaljno (analytics)

#### 5.1 Kreiranje Data View-a

1. **Otvori Kibanu**: `http://localhost:5601`

2. **Navigacija do Data Views**:
   - Gornji levi meni (☰) → **Management** → **Stack Management**
   - U levom meniju odaberi **Kibana** → **Data Views**

3. **Kreiraj novi Data View**:
   - Klikni **Create data view** (plavo dugme gore-desno)
   - **Name**: `forensic-stats-*`
   - **Index pattern**: `forensic-stats-*` (ovo pokriva sve indekse kao `forensic-stats-2026.02.21`)
   - **Timestamp field**: Odaberi `@timestamp`
   - Klikni **Save data view to Kibana**

4. **Proveri polja** (screenshot sa Fields (13)):
   - Posle kreiranja ostani na stranici Data View-a
   - U **Fields (13)** tabu vidiš sva polja koja Logstash parsira:
     - `@timestamp` (date) — vreme događaja (iz log timestamp-a)
     - `_id`, `_index`, `_score`, `_source` — Elasticsearch metadata
     - **`forenzicar`** (text) + **`forenzicar.keyword`** (keyword) — ime forenzičara
     - **`grad`** (text) + **`grad.keyword`** (keyword) — grad gde se desio incident
     - **`tip_pretnje`** (text) + **`tip_pretnje.keyword`** (keyword) — klasifikacija pretnje

> **Napomena**: `.keyword` polja su agregatabilna (za vizualizacije), dok `text` polja su pretraživa (za full-text search).

> **Tehnički detalj**: Logstash koristi **dissect filter** (brži od Grok za fiksni format) za parsiranje:
> ```
> "2026-02-21 14:43:48.288 [STATS] :: Sombor :: Mihajlo Bogdanovic :: Ransomware"
>  ↓ dissect ↓
> grad=Sombor, forenzicar=Mihajlo Bogdanovic, tip_pretnje=Ransomware
> ```

**Ako nema podataka**:
- Sačekaj 10–30 sekundi (Logstash polling interval)
- Proveri da li backend ima logove: `docker logs udd-backend | grep STATS`
- Proveri Logstash: `docker logs udd-logstash | grep "forensic-stats"`
- Proveri da li se fajl `statistics.log` kreira: `docker exec udd-backend ls -la /app/logs/`
- Proveri da nema `_dissectfailure` tag: `curl "http://localhost:9200/forensic-stats-*/_search?q=tags:_dissectfailure"`

---

#### 5.2 Discover — Pregled sirovih događaja

**Discover je glavna stranica za real-time pregled log događaja** — funkcioniše odlično za:
- Praćenje novih izveštaja u realnom vremenu
- Filtriranje po gradu, forenzičaru, ili tipu pretnje
- Detekcija anomalija (npr. neobičan grad ili tip pretnje)

1. **Otvori Discover**:
   - Gornji levi meni (☰) → **Analytics** → **Discover**
   - Gore levo, izaberi Data View: `forensic-stats-*`

2. **Podesi vremenski opseg**:
   - Gore desno, klikni na kalendar (npr. `Last 15 minutes`)for
   - Odaberi **Today** ili **Last 7 days** da uhvatiš sve unose

3. **Proveri događaje**:
   - Svaki red predstavlja jedan log događaj (screenshot pokazuje „2 hits")
   - Klikni `>` šipku pored reda da razviješ detalje
   - U razvijenom view-u vidiš sva 3 polja: `grad: Sombor`, `forenzicar: Mihajlo Bogdanovic`, `tip_pretnje: Ransomware`

4. **Filter test**:
   - Gore u search bar-u: `grad: Sombor`
   - Proveri da se filtriraju samo događaji iz Sombora (histogram se ažurira)
   - Dodaj još filtera: `tip_pretnje: Ransomware`
   - Kombinuj: `grad: Sombor AND tip_pretnje: Ransomware`

5. **Field Statistics** (leva strana):
   - Klikni na polje `grad` u Selected fields — vidiš top vrednosti (screenshot pokazuje „Sombor: 2 (100%)")
   - Klikni na vrednost „Sombor" → automatski dodaje filter
   - Koristi ovo za brzo istraživanje distribucije podataka

---

#### 5.3 Kreiranje vizualizacija

> **Napomena (Kibana 8.x)**: U modernim verzijama Kibane, vizualizacije se kreiraju kroz **Lens** editor (preporučeni pristup), ne kroz "Aggregation based" opcije. Lens automatski prepoznaje najbolji tip grafikona na osnovu podataka koje odabereš.

**A) Vizualizacija 1: Top Gradovi (Bar Chart)**

1. **Navigacija**: Gornji levi meni (☰) → **Analytics** → **Visualize Library**

2. **Kreiraj vizualizaciju**:
   - Klikni **Create visualization** (plavo dugme)
   - Odaberi **Lens** (prva opcija u dijalogu "New visualization")

3. **Konfiguracija u Lens editoru**:
   - **Data view**: Gore levo, odaberi `forensic-stats-*`
   - **Drag and drop polja**:
     1. Iz leve strane (Available fields), pronađi **`grad.keyword`**
     2. **Prevuci** ga u **Horizontal axis** (X-osa) zonu
     3. Lens će automatski primeniti **Top 5 values** agregaciju i kreirati **Bar Vertical** chart
   - **Podešavanje**:
     - Klikni na `grad.keyword` u Horizontal axis → **Number of values**: promeni na `10` (top 10 gradova)
     - **Display name**: `Grad`
     - **Y-osa** (Vertical axis) je već postavljena na `Count of records` — to je broj incidenata
   
4. **Vremenski opseg**: Gore desno calendar ikona → postavi **Last 30 days**

5. **Sačuvaj**:
   - Gore desno **Save** dugme
   - **Title**: `Top Gradovi po Incidentima`
   - **Description** (opciono): `Bar chart pokazuje gradove sa najviše forenzičkih izveštaja`
   - **Save and return** (ili **Save and add to dashboard** ako želiš odmah da dodaš na dashboard)

**Očekivano**: Bar chart koji pokazuje gradove sa najviše forenzičkih izveštaja. Npr. ako imaš 2 izveštaja iz Sombora, Sombor će biti na vrhu sa visinom bara = 2.

---

**B) Vizualizacija 2: Top Forenzičari (Tag Cloud)**

1. **Kreiraj novu**: **Visualize Library** → **Create visualization** → **Lens**

2. **Konfiguracija**:
   - **Data view**: `forensic-stats-*`
   - **Prevuci polje**: Iz leve strane, prevuci **`forenzicar.keyword`** u centralni panel
   - Lens će automatski kreirati **Bar Vertical**. Moramo promeniti tip:
     - Gore desno, klikni na **Chart type** ikonu (trenutno pokazuje bar chart)
     - U listi, odaberi **Tag cloud**
   - **Podešavanje**:
     - Klikni na `forenzicar.keyword` u konfiguraciji → **Number of values**: `20`
     - **Display name**: `Forenzičar`

3. **Sačuvaj**:
   - **Title**: `Top Forenzičari`
   - **Description**: `Najaktivniji forenzički istražitelji (broj obrađenih incidenata)`
   - **Save**

**Očekivano**: Oblak reči (Tag Cloud) gde su najaktivniji forenzičari prikazani većim fontom. Npr. "Mihajlo Bogdanovic" će biti najveći ako ima najviše izveštaja.

---

**C) Vizualizacija 3: Udeo Pretnji (Donut Chart)**

1. **Kreiraj novu**: **Visualize Library** → **Create visualization** → **Lens**

2. **Konfiguracija**:
   - **Data view**: `forensic-stats-*`
   - **Prevuci polje**: Prevuci **`tip_pretnje.keyword`** u centralni panel
   - **Promeni tip grafikona**:
     - Gore desno **Chart type** → odaberi **Donut**
   - **Podešavanje**:
     - Klikni na `tip_pretnje.keyword` → **Number of values**: `10`
     - **Display name**: `Tip Pretnje`
   - **Dodatno stilizovanje** (opciono):
     - U desnom panelu pod **Appearance** možeš:
       - Omogućiti **Show labels** (prikaz procenata na segmentima)
       - **Legend position**: Right ili Bottom

3. **Sačuvaj**:
   - **Title**: `Udeo Pretnji po Tipu`
   - **Description**: `Raspodela forenzičkih incidenata po klasifikaciji pretnje`
   - **Save**

**Očekivano**: Donut grafikon (krofna) gde svaki segment predstavlja klasifikaciju pretnje (Ransomware, Trojan, APT, DDoS...). Veličina segmenta = broj incidenata te vrste. Ako imaš samo Ransomware test podatke, biće jedan veliki segment sa 100%.

---

#### 5.4 Kreiranje Dashboard-a

1. **Navigacija**: Gornji levi meni (☰) → **Analytics** → **Dashboard**

2. **Kreiraj novi**:
   - Klikni **Create dashboard** (plavo dugme)
   - **Add panel** (gore levo)

3. **Dodaj sve 3 vizualizacije**:
   - U modalu "Add from library" potražite:
     - `Top Gradovi po Incidentima`
     - `Top Forenzičari (Tag Cloud)`
     - `Udeo Pretnji po Tipu`
   - Klikni na svaku da je dodaš

4. **Rasporedi panele**:
   - Drag & drop panele da lepo izgledaju
   - Povuci ivice da promeniš veličinu

5. **Sačuvaj dashboard**:
   - Gore desno **Save**
   - **Title**: `Forenzički Pregled — UDD`
   - **Description**: `Statistika novih izveštaja iz Logstash pipeline-a`
   - **Save**

**Bonus**: Omogući **Auto-refresh**:
- Gore desno, klikni na kalendar → **Refresh every** → odaberi `10 seconds` ili `1 minute`
- Dashboard će se automatski ažurirati kada Logstash indeksira nove događaje

---

#### 5.5 Test sa novim podacima

1. **Unesi još jedan izveštaj** na `http://localhost:4200`:
   - Forenzički istražitelj: `Ana Jovanović`
   - Organizacija: `SecOps Tim`
   - Naziv malvera: `Emotet-Sample`
   - Klasifikacija: `Trojan`
   - Grad: `Beograd`
   - Koordinate: `44.7866`, `20.4489`
   - Opis: `Banking trojan propagated via phishing emails.`

2. **Sačekaj 10–30 sekundi**, pa:
   - **Discover**: Refresh stranicu — vidi se novi događaj
   - **Dashboard**: Automatski refresh će pokazati Beograd + Ana Jovanović + Trojan segment
   - **Vizualizacije**: Donut će sada imati 2 segmenta (Ransomware + Trojan)

3. **Provera filtera na dashboard-u**:
   - Klikni na segment `Trojan` u donut chart-u → filteruje dashboard samo na Trojan
   - Gore levo vidiš filter chip `tip_pretnje.keyword: Trojan`
   - Klikni X da ukloniš filter

---

### ✅ Krajnji rezultat Kibana testiranja

- **Data View**: `forensic-stats-*` sa 13 polja
- **Discover**: Real-time pregled log događaja sa filterima
- **3 Vizualizacije**:
  1. Bar chart — Top gradovi
  2. Tag cloud — Najaktivniji forenzičari
  3. Donut chart — Raspodela pretnji po tipu
- **Dashboard**: Centralizovan pregled sa auto-refresh funkcijom

> Kibana sada prikazuje **statistiku upotrebe** sistema — koliko izveštaja podnosi svaki forenzičar, koji gradovi su najugroženiji, i koje su najčešće pretnje.

---

---

### Opciono: Lokalni razvoj (bez Docker-a za front/back)

Ako želiš da menjaš kod i reloaduješ brzo:

```bash
# 1. Podigni samo infrastrukturu
docker compose up -d elasticsearch kibana logstash minio postgres tika

# 2. Backend (koristi localhost profil — application.yml)
cd backend
docker run --rm -v "${PWD}:/app" -w /app maven:3.9.9-eclipse-temurin-17 mvn -DskipTests package
java -jar target/forensic-1.0.0-SNAPSHOT.jar

# 3. Frontend (sa proxy na localhost:8080)
cd frontend
npm install
npm start
```

---

## Kratak troubleshooting

- Ako backend javlja DB auth grešku: proveri da je PostgreSQL na portu `5433` (host) / `5432` (Docker mreža).
- Ako upload radi a nema rezultata u pretrazi: proveri da je Tika dostupna na `http://localhost:9998`.
- Ako Kibana nema stats dokumente: proveri `logstash` container i deljeni `app-logs` volume.
- Ako frontend ne dohvata API: proveri da nginx proxy radi — `curl http://localhost:4200/api/reports`.

### API Endpointi

#### Izveštaji
- `POST /api/reports` - Kreiranje izveštaja (multipart: metadata JSON + opcioni PDF fajl)
- `GET /api/reports` - Lista svih izveštaja
- `GET /api/reports/{id}` - Detalji izveštaja
- `DELETE /api/reports/{id}` - Brisanje izveštaja
- `GET /api/reports/{id}/download` - Preuzimanje PDF-a

#### Pretraga
- `POST /api/search` - Glavna pretraga (prosta/napredna/hibridna)
- `GET /api/search/simple?q=...` - Prosta pretraga
- `POST /api/search/geo` - Geo-lokacijska pretraga

### Napredna Boolean Pretraga (Stack Machine)
Podržava operatore: `AND`, `OR`, `NOT`, zagrade `()`, polja `field:value`, fraze `"..."`

**Primeri:**
```
WannaCry
classification:Ransomware
(malware:WannaCry OR description:"enkripcija fajlova") AND NOT classification:Spyware
forensicInvestigator:"Mihajlo Bogdanović" AND city:Beograd
```

### Kibana Dashboardi
Za **detaljno objašnjenje Kibana testiranja**, vidi **Korak 5** u tutorijalu iznad — uključuje:
- Kreiranje `forensic-stats-*` Data View-a
- Discover — pregled sirovih događaja sa filterima
- 3 vizualizacije: Bar Chart (gradovi), Tag Cloud (forenzičari), Donut Chart (pretnje)
- Kreiranje dashboard-a sa auto-refresh funkcijom
- Test sa novim podacima i provera filtera

**Kratak pregled**:
1. **Data View**: `forensic-stats-*` (Management → Data Views)
2. **Vizualizacije** (Analytics → Visualize Library):
   - Top Gradovi: Bar Chart, Terms na `grad.keyword`
   - Top Forenzičari: Tag Cloud, Terms na `forenzicar.keyword`
   - Udeo Pretnji: Donut Chart, Terms na `tip_pretnje.keyword`
3. **Dashboard** (Analytics → Dashboard): Dodaj sve 3 vizualizacije + omogući auto-refresh
