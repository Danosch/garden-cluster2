# Garden Cluster2 Setup Guide

Diese Anleitung beschreibt Schritt für Schritt, wie du **Garden Cluster2** mit `kind`, Helm-Monitoring und der Quarkus-Garden-App aufsetzt. Einfach die Befehle kopieren und ausführen.

---

## Voraussetzungen

Stelle sicher, dass folgende Tools installiert und in deiner PATH verfügbar sind:

- Docker (Desktop oder Engine)
- Kind v0.17+ (`go install sigs.k8s.io/kind@v0.17.0`)
- kubectl v1.25+
- Helm v3+
- Java 21
- Maven 3.6+

---

## Projektstruktur

```
/
├── .gitignore
├── .env               # Umgebungsvariablen für Docker Compose (In-Memory DB)
├── docker-compose.yaml
├── Dockerfile
├── kind-cluster.yaml
├── k8s/
│   ├── configmap.yaml
│   ├── deployment.yaml
│   ├── hpa.yaml
│   ├── ingress.yaml
│   ├── garden-postgres-secret.yaml
│   ├── netpol-whitelist.yaml
│   ├── pv.yaml
│   ├── pvc.yaml
│   └── service.yaml
├── monitoring/
│   ├── kube-prometheus-stack/
│   │   └── servicemonitor-garden.yaml
│   ├── values-prom.yaml
│   ├── values-grafana.yaml
│   ├── values-loki.yaml
│   ├── values-tempo.yaml
│   └── values-otel.yaml
├── src/
│   └── main/
│       ├── java/com/garden/...   # Quarkus-Code
│       └── resources/
├── target/            # Build-Ausgabe
└── pom.xml            # Maven Project Object Model
```

---

## 1. Kind-Cluster starten

```bash
kind create cluster --name garden-cluster2 --config kind-cluster.yaml
kubectl cluster-info --context kind-garden-cluster2
docker ps  # prüfen, dass Node läuft
kubectl create namespace garden
kubectl create namespace monitoring
```

---

## 2. Monitoring-Stack installieren

### 2.1 Helm-Repositories hinzufügen

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

### 2.2 Prometheus (kube-prometheus-stack)

```bash
helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
  -n monitoring -f monitoring/values-prom.yaml
```

### 2.3 Loki (Logs)

```bash
helm upgrade --install loki grafana/loki \
  -n monitoring -f monitoring/values-loki.yaml
```

### 2.4 Tempo (Traces)

```bash
helm upgrade --install tempo grafana/tempo \
  -n monitoring -f monitoring/values-tempo.yaml
```

### 2.5 OpenTelemetry Collector

```bash
helm upgrade --install otel-collector open-telemetry/opentelemetry-collector \
  -n monitoring -f monitoring/values-otel.yaml
```

### 2.6 Grafana UI

```bash
helm upgrade --install grafana grafana/grafana \
  -n monitoring -f monitoring/values-grafana.yaml
```

---

## 3. Metrics Server (für HPA)

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl wait --for=condition=Available apiservice v1beta1.metrics.k8s.io --timeout=60s
```

---

## 4. Datenbank: PostgreSQL

```bash
helm upgrade --install postgresql bitnami/postgresql \
  --namespace garden \
  --set global.postgresql.postgresqlDatabase=gardendb \
  --set auth.username=admin,auth.password=secret,auth.database=gardendb
```

---

## 5. Quarkus-Garden-App bauen & Docker-Image

```bash
mvn clean package -DskipTests
docker build -t garden-app:latest .
```

---

## 6. Deployment in Namespace `garden`

### 6.1 Garden-App deployen

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/garden-postgres-secret.yaml
kubectl apply -f k8s/pv.yaml
kubectl apply -f k8s/pvc.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml   # optional
kubectl apply -f k8s/hpa.yaml
```

---

## 7. Endpunkte & Beispiele

### Health & Metrics

```bash
curl http://localhost:8080/q/health/live
curl http://localhost:8080/q/health/ready
curl http://localhost:8080/q/metrics
```

### Garden API

| Methode | Pfad                        | Beschreibung         |
| ------- | --------------------------- | -------------------- |
| GET     | `/garden`                   | Willkommen-Endpoint  |
| POST    | `/garden/create`            | Garten erstellen     |
| GET     | `/garden/all`               | Alle Gärten          |
| GET     | `/garden/{gardenId}`        | Garten nach ID       |
| PUT     | `/garden/{gardenId}`        | Garten aktualisieren |
| DELETE  | `/garden/{gardenId}`        | Garten löschen       |

### Tree API

| Methode | Pfad                              | Beschreibung    |
| ------- | --------------------------------- | --------------- |
| POST    | `/garden/tree/create`             | Baum erstellen  |
| GET     | `/garden/tree/all`                | Alle Bäume      |
| GET     | `/garden/{gardenId}/trees`        | Bäume pro Garten|
| DELETE  | `/garden/tree/{treeId}`           | Baum löschen    |

### Plant API

| Methode | Pfad                                | Beschreibung       |
| ------- | ----------------------------------- | ------------------ |
| POST    | `/garden/plant/create`              | Pflanze erstellen  |
| GET     | `/garden/plant/all`                 | Alle Pflanzen      |
| GET     | `/garden/{gardenId}/plants`         | Pflanzen pro Garten|
| DELETE  | `/garden/plant/{plantId}`           | Pflanze löschen    |

---

## 8. Port-Forwards

```bash
# Monitoring
kubectl port-forward -n monitoring svc/grafana 3000:3000 &
kubectl port-forward -n monitoring svc/loki-gateway 3100:80 &
kubectl port-forward -n monitoring svc/tempo 3200:3200 &
kubectl port-forward -n monitoring svc/otel-collector-opentelemetry-collector 4317:4317 &
kubectl port-forward -n monitoring svc/otel-collector-opentelemetry-collector 4318:4318 &

# App
kubectl port-forward -n garden svc/garden-service 8080:80 &
```

---

## 9. Bekannte Einschränkungen

- **Loki & Tempo**: installiert, laufen, liefern aber aktuell keine Logs/Traces.
- **Prometheus**: funktioniert einwandfrei.
- **PostgreSQL**: läuft im Garden-Namespace.

---

*Passe bei Bedarf Ports oder Pfade in den Befehlen an.*

---

## Abschluss

- **Prometheus-Metriken** funktionieren einwandfrei und lassen sich unter `http://localhost:8080/q/metrics` oder direkt im Prometheus-UI abrufen.
- **Garden-App** ist per Port-Forward auf `http://localhost:8080` erreichbar; alle Health-, API- und Metrics-Endpunkte können aufgerufen werden.
- **Loki & Tempo** sind installiert und laufen, liefern jedoch derzeit keine vollständigen Logs oder Traces. Eine genaue Untersuchung hierfür steht noch aus.

---

### Hinweise zu Windows/WSL

Auf Windows-Systemen (insbesondere mit WSL2) kann es Probleme mit Ingress und Load-Balancern geben. Die Host-Port-Weiterleitung und DNS-Auflösung sind unter Windows oft fehleranfällig. Ich empfehle:

- **Linux oder macOS** für eine stabile Kind/Ingress-Erfahrung.
- Unter Windows nur für schnelle Tests nutzen, aber nicht für produktives Aufsetzen.
- Falls Windows unumgänglich ist: Ingress-Controller wie **nginx-ingress** mit HostNetwork-Modus oder alternative Port-Forward-Strategien nutzen.