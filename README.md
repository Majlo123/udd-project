# UDD - Digital Document Management
## Forensic Reports - Search and Analytics Microservice

### Architecture
```text
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│   Angular    │────▶│  Spring Boot │────▶│  Elasticsearch   │
│   Frontend   │     │   REST API   │     │  (Full-text +    │
│  (port 4200) │     │  (port 8080) │     │   Geo Search)    │
└──────────────┘     └──────┬───────┘     └──────────────────┘
                            │
                    ┌───────┼───────┐
                    ▼       ▼       ▼
              ┌─────────┐ ┌─────┐ ┌──────────┐
              │  MinIO  │ │Tika │ │PostgreSQL│
              │(port 9000 │(9998│ │(port 5433│
              │  /9001) │ │)    │ │)         │
              └──────────┘ └─────┘ └──────────┘

┌──────────────┐     ┌──────────────────┐
│   Logstash   │────▶│   Kibana         │
│ (stats logs) │     │   (port 5601)    │
└──────────────┘     │   - Dashboards   │
                     └──────────────────┘
