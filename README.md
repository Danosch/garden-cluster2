# Garden Cluster2 Setup Guide

Diese Anleitung beschreibt Schritt für Schritt, wie du **Garden Cluster2** mit `kind`, Helm-Monitoring und der Quarkus-Garden-App aufsetzt. Einfach die Befehle kopieren und ausführen.

---

## Voraussetzungen

Stelle sicher, dass folgende Tools installiert und in deiner PATH verfügbar sind:

* Docker (Desktop oder Engine)
* Kind v0.17+ (`go install sigs.k8s.io/kind@v0.17.0`)
* kubectl v1.25+
* Helm v3+
* Java 21
* Maven 3.6+

---

## Projektstruktur

```text
/
├── .gitignore
├── .env               # Umgebungsvariablen für Docker Compose
├── docker-compose.yaml
├── Dockerfile
├── kind-cluster.yaml
├── k8s/
│   ├── configmap.yaml
│   ├── deployment.yaml
│   ├── hpa.yaml
│   ├── ingress.yaml
│   ├── mongo-values.yaml
│   ├── netpol-whitelist.yaml
│   ├── pv.yaml
│   ├── pvc.yaml
│   ├── secret.yaml
│   └── service.yaml
├── monitoring/
│   ├── service-monitor-garden.yaml
│   ├── values-grafana.yaml
│   ├── values-loki.yaml
│   ├── values-otel.yaml
│   ├── values-prom.yaml
│   └── values-tempo.yaml
├── src/
│   └── main/
│       ├── java/com/garden/…   (Quarkus-Code)
│       └── resources/
├── target/            # Build-Ausgabe
└── pom.xml            # Maven Project Object Model
```

---

## 1. Kind-Cluster starten

```bash
kind create cluster --name garden-cluster2 --config kind-cluster.yaml
kubectl cluster-info --context kind-garden-cluster2
kubectl create namespace garden
kubectl create namespace monitoring
```

---

## 2. Monitoring-Stack installieren

### 2.1 Helm-Repositories hinzufügen

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add cortex https://cortexproject.github.io/cortex-helm-chart
helm repo update
```

### 2.2 Prometheus (Cortex/Mimir)

```bash
helm upgrade --install cortex cortex/cortex \
  --namespace monitoring \
  -f monitoring/values-prom.yaml
```

### 2.3 Loki (Logs)

```bash
helm upgrade --install loki grafana/loki \
  --namespace monitoring \
  -f monitoring/values-loki.yaml \
  --set loki.useTestSchema=true
```

### 2.4 Tempo (Traces)

```bash
helm upgrade --install tempo grafana/tempo \
  --namespace monitoring \
  -f monitoring/values-tempo.yaml
```

### 2.5 Grafana UI

```bash
helm upgrade --install grafana grafana/grafana \
  --namespace monitoring \
  -f monitoring/values-grafana.yaml
```

---

## 3. Metrics Server (für HPA)

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl wait --for=condition=Available apiservice v1beta1.metrics.k8s.io --timeout=60s
```

---

## 4. Quarkus-Garden-App bauen & Docker-Image

```bash
mvn clean package -DskipTests
docker build -t garden-app:latest .
```

---

## 5. Deployment in Namespace `garden`

### 5.1 MongoDB einrichten

```bash
kubectl apply -f k8s/mongo-values.yaml    # Secret & ConfigMap für MongoDB
kubectl apply -f k8s/pv.yaml              # PersistentVolume
kubectl apply -f k8s/pvc.yaml             # PersistentVolumeClaim
kubectl apply -f k8s/deployment.yaml      # MongoDB Deployment
kubectl apply -f k8s/service.yaml         # MongoDB Service
```

### 5.2 Garden-App deployen

```bash
kubectl apply -f k8s/deployment.yaml      # App Deployment
kubectl apply -f k8s/service.yaml         # App Service (Port 8080)
kubectl apply -f k8s/ingress.yaml         # Ingress (optional)
```

### 5.3 Horizontal Pod Autoscaler

```bash
kubectl apply -f k8s/hpa.yaml             # HPA aktivieren
kubectl get hpa -n garden                 # Status prüfen
```

---

## 6. Endpunkte & Beispiele

### Health & Metrics

* Liveness Probe:

  ```bash
  curl http://localhost:8080/q/health/live
  ```
* Readiness Probe:

  ```bash
  curl http://localhost:8080/q/health/ready
  ```
* Prometheus-Metriken:

  ```bash
  curl http://localhost:8080/q/metrics
  ```

### Garden API (`/garden`)

| Methode | Pfad                 | Beschreibung         |
| ------- | -------------------- | -------------------- |
| GET     | `/garden`            | Welcome-Endpoint     |
| POST    | `/garden/create`     | Garten erstellen     |
| GET     | `/garden/all`        | Alle Gärten          |
| GET     | `/garden/{gardenId}` | Garten nach ID       |
| PUT     | `/garden/{gardenId}` | Garten aktualisieren |
| DELETE  | `/garden/{gardenId}` | Garten löschen       |

### Tree API (`/garden/tree`)

| Methode | Pfad                       | Beschreibung        |
| ------- | -------------------------- | ------------------- |
| POST    | `/garden/tree/create`      | Baum erstellen      |
| GET     | `/garden/tree/all`         | Alle Bäume          |
| GET     | `/garden/{gardenId}/trees` | Bäume eines Gartens |
| DELETE  | `/garden/tree/{treeId}`    | Baum löschen        |

### Plant API (`/garden/plant`)

| Methode | Pfad                        | Beschreibung           |
| ------- | --------------------------- | ---------------------- |
| POST    | `/garden/plant/create`      | Pflanze erstellen      |
| GET     | `/garden/plant/all`         | Alle Pflanzen          |
| GET     | `/garden/{gardenId}/plants` | Pflanzen eines Gartens |
| DELETE  | `/garden/plant/{plantId}`   | Pflanze löschen        |

### Beispielaufrufe

* **Hello**

  ```bash
  curl http://localhost:8080/garden
  ```

* **Garten erstellen**

  ```bash
  curl -X POST http://localhost:8080/garden/create \
    -H 'Content-Type: application/json' \
    -d '{"name":"Mein Garten","description":"Test"}'
  ```

* **Alle Gärten abrufen**

  ```bash
  curl http://localhost:8080/garden/all
  ```

---

## 7. Alternative: Docker Compose

```yaml
version: '3.8'
services:
  mongo:
    image: mongo:6.0
    ports:
      - '27017:27017'
  garden:
    build: .
    image: garden-app:latest
    ports:
      - '8080:8080'
    env_file:
      - .env
```

```bash
docker-compose up --build
```

---

*Hinweis: Passe bei Bedarf Ports und Pfade in **\`\`** oder Helm-Werten an.*
