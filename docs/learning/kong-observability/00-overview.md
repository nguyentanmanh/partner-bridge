# Observability

```text
Client -> Kong -> Payment Service -> Provider
           |
           +-> metric -> Prometheus -> Grafana
           |
           +-> structured log -> Terminal
```

## Monitoring và Observability

- **Monitoring** trả lời các câu hỏi đã biết trước, ví dụ traffic, error rate và
Target health.
- **Observability** giúp điều tra cả câu hỏi chưa biết trước bằng
dữ liệu hệ thống đã phát ra. Monitoring là một phần của Observability.

## Ba loại tín hiệu

| Tín hiệu | Ví dụ                        | Dùng để trả lời |
| --- |------------------------------| --- |
| Log | Camera an ninh               | Request cụ thể đã xảy ra chuyện gì? |
| Metric | Đồng hồ trên bảng điều khiển | Hệ thống đang nhanh, chậm hay nhiều lỗi? |
| Trace | Bản đồ hành trình            | Thời gian của request nằm ở service/span nào? |

## Vai trò từng công cụ

### Kong

Kong xử lý traffic và tạo access/error log, structured JSON log và các metric
request/status/latency/health tại `/metrics`.

### Prometheus

Prometheus là kho time-series. Cứ 5 giây nó chủ động **pull** dữ liệu từ:

```text
http://kong-observability-lab:8201/metrics
```

Kong không push metric sang Prometheus.

### Grafana

Grafana không thu thập và không lưu metric của Kong. Nó gửi PromQL tới
Prometheus rồi vẽ kết quả thành dashboard.

```text
Kong tạo số liệu
  -> Prometheus lấy và lưu
  -> PromQL tính toán
  -> Grafana hiển thị
```

## Hai góc nhìn bổ sung nhau

```text
Metric: “5xx tăng từ 1% lên 10%”
Log:    “request abc-123 lỗi khi gọi Target 172.x.x.x”
```

Metric phát hiện và khoanh vùng xu hướng; log giải thích một sự kiện cụ thể.

## Kiểm tra hạ tầng

```bash
curl -s http://127.0.0.1:8201/metrics | head -20
curl -s 'http://127.0.0.1:9090/api/v1/query?query=up'
curl -s http://127.0.0.1:3000/api/health
```

## Checklist

- [ ] Phân biệt được log, metric và trace.
- [ ] Biết Prometheus pull metric, không nhận push từ Kong trong lab này.
- [ ] Biết Grafana đọc dữ liệu từ Prometheus.
- [ ] Biết Grafana không thực hiện health check hoặc failover.
- [ ] Vẽ lại được sơ đồ Kong → Prometheus → Grafana.

Next [Terminal và structured log](01-terminal-logs.md).
