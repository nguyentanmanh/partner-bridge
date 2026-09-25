# Cấp 3: Grafana dashboard

## Mục tiêu

Dùng dashboard để phát hiện xu hướng, sau đó quay lại Prometheus và terminal để
điều tra. Grafana không thay thế kiến thức PromQL.

## Bước 1: Mở dashboard

Mở [PartnerBridge - Kong Observability Lab](http://127.0.0.1:3000/d/partnerbridge-kong-observability/partnerbridge-kong-observability-lab).

Cho phép anonymous access trên localhost. Chọn time range `Last 15 minutes`
và refresh `5s`.

Nếu chưa có dữ liệu:

```bash
for i in {1..30}; do
  curl -s -o /dev/null \
    http://localhost:8200/api/v1/payments/00000000-0000-0000-0000-000000000000 \
    -H 'X-Api-Key: observability-lab-key' \
    -H "Request-ID: level-3-$i"
done
```

Đợi một chu kỳ scrape khoảng 5 giây.

## Bước 2: Đọc từng panel

| Panel | Câu hỏi vận hành | PromQL cốt lõi |
| --- | --- | --- |
| Request rate | Traffic đang tăng hay giảm? | `sum(rate(kong_http_requests_total[1m]))` |
| 5xx error rate | Bao nhiêu phần trăm request là server error? | Tốc độ 5xx / tốc độ tổng |
| P95 total latency | 95% request nhanh hơn bao nhiêu ms? | `histogram_quantile(...)` |
| P95 upstream | Backend/provider có chậm không? | `kong_upstream_latency_ms_bucket` |
| Responses by status | Status nào đang tăng? | Group by `code` |
| Latency breakdown | Thời gian nằm ở Kong hay upstream? | Ba latency histogram |
| Target health | Target nào healthy/unhealthy? | `kong_upstream_target_health` |

Trong Grafana, mở menu của panel và chọn `Inspect` hoặc `Edit` để xem query.
Bạn cần nhận ra các PromQL đã học ở cấp 2.

## Bước 3: Thử nghiệm Target failure

Dừng Target B:

```bash
docker compose -f compose.yaml -f compose.kong-observability.yaml stop \
  payment-observability-b
```

Đợi 10–15 giây. So sánh Admin API với health panel:

```bash
curl -s http://127.0.0.1:8201/upstreams/payment-observability-upstream/health
```

Khôi phục:

```bash
docker compose -f compose.yaml -f compose.kong-observability.yaml start \
  payment-observability-b
```

Kong thực hiện health check và failover. Prometheus chỉ lưu metric; Grafana chỉ
hiển thị metric.

## Bước 4: Workflow điều tra đúng

```text
Grafana: phát hiện 5xx hoặc P95 tăng
  ↓
Prometheus: query theo status/service/route để khoanh vùng
  ↓
Terminal: tìm Request-ID trong structured log
  ↓
Kết luận: Kong, Payment Service hay Provider là nơi cần điều tra
```

Không nên nhìn một panel rồi kết luận ngay nguyên nhân.

## Thực hành

1. Tạo một `401` và một `404`; quan sát panel status.
2. Dừng Target B; quan sát health panel thay đổi.
3. Mở query phía sau panel P95.
4. Chọn một request từ terminal và đối chiếu latency với xu hướng dashboard.
5. Giải thích tình huống upstream P95 tăng nhưng Kong P95 ổn định.

## Troubleshooting

Grafana không có dữ liệu:

```bash
curl -s 'http://127.0.0.1:9090/api/v1/query?query=up'
curl -s http://127.0.0.1:8201/metrics | grep kong_http_requests_total
docker compose -f compose.yaml -f compose.kong-observability.yaml logs --tail=100 \
  prometheus-observability grafana-observability
```

Nếu metric chưa tồn tại, gửi traffic qua port `8200`, đợi 5 giây và refresh.

## Checklist

- [ ] Hiểu câu hỏi vận hành của từng panel.
- [ ] Xem được PromQL phía sau panel.
- [ ] Biết Grafana không thu thập hoặc lưu Kong metric.
- [ ] Biết Grafana không thực hiện health check/failover.
- [ ] Đi được từ dashboard → Prometheus → Request-ID/log.
- [ ] Không kết luận nguyên nhân chỉ từ một biểu đồ.


> Log giúp điều tra một request; Prometheus lưu và truy vấn metric theo thời
> gian; Grafana trực quan hóa PromQL. Observability giúp thấy và hiểu vấn đề,
> nhưng không tự sửa hệ thống.
