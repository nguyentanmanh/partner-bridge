# Kong Trong PartnerBridge

| Thứ tự | Chủ đề                                                        | File chạy                          | Tài liệu                                   | Cổng                                                          |
|-------:|---------------------------------------------------------------|------------------------------------|--------------------------------------------|---------------------------------------------------------------|
|      0 | Kong Foundations: DB-less, Route, Service, Consumer và plugin | `compose.yaml`                     | [Bắt đầu](../onboarding/how-kong-works.md) | Proxy `8000`, Admin `8001`                                    |
|      1 | Kong Upstream, load balancing và failover                     | `compose.kong-load-balancing.yaml` | [Bắt đầu](kong-load-balancing.md)          | Proxy `8100`, Admin `8101`                                    |
|      2 | Kong Observability: log, metric và dashboard                  | `compose.kong-observability.yaml`  | [Bắt đầu](kong-observability.md)           | Proxy `8200`, Admin `8201`, Prometheus `9090`, Grafana `3000` |

```text
Foundations
  -> Load balancing và failover
  -> Observability
```

## Quy tắc chạy lab

Các lab nâng cao là Compose override:

```bash
docker compose -f compose.yaml -f <file-lab> <command>
```

`compose.yaml` cung cấp network và provider stubs; file lab bổ sung Kong và
Payment Service riêng cho bài học. Mỗi trang lab có lệnh khởi động/dừng chính
xác, Hãy mở trang “Bắt đầu” trước khi chạy.
