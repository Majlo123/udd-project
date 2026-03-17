# UDD - Digital Document Management
## Forensic Reports - Search & Analytics Microservice

## 🏗️ Architecture
```
Angular (Frontend - 4200) → Spring Boot (Backend - 8080) → Elasticsearch
                                     ↓
                     MinIO | Tika | PostgreSQL
                                     ↓
                           Logstash → Kibana
```

---

## 📌 Overview
UDD is a microservice-based system for managing forensic reports with automatic PDF generation, full-text search, geo search, and real-time analytics.

---

## ⚙️ Requirements
- Docker & Docker Compose

---

## 🚀 Running
```bash
docker compose up -d --build
```

## ⏹️ Stop
```bash
docker compose down
```

---

## 🔍 Features
- Full-text search (via Apache Tika)
- Structured & Boolean search
- Fuzzy search
- Geo search
- Automatic PDF generation
- MinIO storage
- Kibana analytics dashboards

---

## 📡 Services
- Frontend: http://localhost:4200
- Backend: http://localhost:8080/api
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601
- MinIO: http://localhost:9001

---

## 🔎 Example Queries
```
classification:Ransomware AND city:Sombor
(malwareName:WannaCry OR description:"search validation") AND NOT classification:Spyware
```

---

## 📊 Analytics
- Top cities
- Top investigators
- Threat distribution
- Real-time monitoring via Kibana

---

## 🧪 Local Development
Run infrastructure only:
```bash
docker compose up -d elasticsearch kibana logstash minio postgres tika
```

---

## 🛠️ Troubleshooting
- Check PostgreSQL port (5433)
- Ensure Tika is running (9998)
- Check Logstash logs
- Verify API via: http://localhost:4200/api/reports

---

## 📡 API

### Reports
- POST /api/reports
- GET /api/reports
- GET /api/reports/{id}
- DELETE /api/reports/{id}
- GET /api/reports/{id}/download

### Search
- POST /api/search
- GET /api/search/simple?q=...
- POST /api/search/geo

---

## 📌 About
Scalable microservice system for forensic reports with automated PDF generation, Elasticsearch full-text & geo search, and real-time analytics using Kibana and Logstash.
