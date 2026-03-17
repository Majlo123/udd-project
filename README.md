# UDD – Digital Document Management  
## Forensic Reports – Search & Analytics Microservice

## 📌 Overview
UDD is a microservice-based system for managing forensic reports. It enables automatic PDF generation, full-text and structured search using Elasticsearch, and real-time analytics with Kibana.

---

## 🏗️ Architecture
Angular (Frontend - 4200)
        │
        ▼
Spring Boot (Backend - 8080)
        │
        ▼
Elasticsearch (Search Engine)
        │
 ┌──────┼──────────────┐
 ▼      ▼              ▼
MinIO   Tika       PostgreSQL

Logstash ─────▶ Kibana (Analytics Dashboard)

---

## 🚀 Features
- Full-text search (PDF content)
- Structured & Boolean search
- Fuzzy search
- Geo search
- Automatic PDF generation
- MinIO document storage
- Kibana analytics dashboards

---

## ⚙️ Tech Stack
- Angular
- Spring Boot
- Elasticsearch
- MinIO
- Apache Tika
- PostgreSQL
- Kibana + Logstash
- Docker

---

## ▶️ Run
```bash
docker compose up -d --build
```

## ⏹️ Stop
```bash
docker compose down
```

---

## 📡 Services
- Frontend: http://localhost:4200
- Backend: http://localhost:8080/api
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601
- MinIO: http://localhost:9001
