# Kong Observability

| Chủ đề                                                               |                                                              |
|----------------------------------------------------------------------|--------------------------------------------------------------|
| [Khái niệm](kong-observability/00-overview.md)                       | Log, metric, trace, Prometheus và Grafana khác nhau thế nào? |
| [Terminal và structured log](kong-observability/01-terminal-logs.md) | Một request cụ thể đi đâu, mất bao lâu và lỗi ở đâu?         |
| [Prometheus và PromQL](kong-observability/02-prometheus-metrics.md)  | Hệ thống có bao nhiêu traffic, lỗi và P95 latency ra sao?    |
| [Grafana dashboard](kong-observability/03-grafana-dashboard.md)      | Làm sao nhìn xu hướng rồi quay lại log để điều tra?          |

```text
Hiểu bản đồ
    ↓
Điều tra một request
    ↓
Đo các request theo thời gian
    ↓
Trực quan để vận hành
```

## Khởi động hạ tầng dùng chung

Đứng tại repository root:

```bash
cd /Users/manhnguyen/Works/TMO/projects/partner-bridge
docker compose -f compose.yaml -f compose.kong-observability.yaml config --quiet
docker compose -f compose.yaml -f compose.kong-observability.yaml up -d --build \
  provider-a provider-b \
  payment-observability-a payment-observability-b \
  kong-observability-lab prometheus-observability grafana-observability
```

Kiểm tra:

```bash
docker compose -f compose.yaml -f compose.kong-observability.yaml ps
```

| Thành phần               | URL                     |
|--------------------------|-------------------------|
| Kong proxy               | `http://localhost:8200` |
| Kong Admin API và metric | `http://127.0.0.1:8201` |
| Prometheus               | `http://127.0.0.1:9090` |
| Grafana                  | `http://127.0.0.1:3000` |

## Bản đồ file

| File                                                                | Vai trò                            |
|---------------------------------------------------------------------|------------------------------------|
| `compose.kong-observability.yaml`                                   | Khởi động toàn bộ                  |
| `gateways/kong/labs/observability/kong.yml`                         | Kong Route, Upstream và plugin     |
| `observability/kong-lab/prometheus.yml`                             | Prometheus scrape Kong mỗi 5 giây  |
| `observability/kong-lab/grafana/provisioning`                       | Tự đăng ký datasource và dashboard |
| `observability/kong-lab/grafana/dashboards/kong-observability.json` | Dashboard Grafana                  |

## Dừng

```bash
docker compose -f compose.yaml -f compose.kong-observability.yaml stop \
  grafana-observability prometheus-observability kong-observability-lab \
  payment-observability-a payment-observability-b
```

Named volumes giữ lịch sử Prometheus và Grafana. Không dùng `down -v` nếu chưa
muốn xóa dữ liệu.
