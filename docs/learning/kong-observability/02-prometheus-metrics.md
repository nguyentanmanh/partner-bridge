# Prometheus, metric và PromQL

## Mục tiêu

Chuyển từ điều tra một request sang đo nhiều request theo thời gian:

```text
Cấp 1: request abc-123 mất bao lâu?
Cấp 2: P95 của mọi request trong 5 phút là bao nhiêu?
```

## Bước 1: Xem metric thô

```bash
curl -s http://127.0.0.1:8201/metrics | grep kong_http_requests_total
```

Ví dụ:

```text
kong_http_requests_total{code="404",service="payment-observability-service"} 12
```

Các loại metric cơ bản:

| Loại      | Ví dụ           | Hành vi                             |
|-----------|-----------------|-------------------------------------|
| Counter   | Tổng request    | Chỉ tăng, reset khi process restart |
| Gauge     | Target health   | Có thể tăng hoặc giảm               |
| Histogram | Request latency | Đếm quan sát theo các bucket        |

## Bước 2: Kiểm tra Prometheus scrape

```bash
curl -G -s http://127.0.0.1:9090/api/v1/query \
  --data-urlencode 'query=up{job="kong-observability-lab"}'
```

Giá trị `1` nghĩa là scrape thành công; `0` nghĩa là target down.

## Bước 3: Tạo traffic

```bash
for i in {1..30}; do
  curl -s -o /dev/null \
    http://localhost:8200/api/v1/payments/00000000-0000-0000-0000-000000000000 \
    -H 'X-Api-Key: observability-lab-key' \
    -H "Request-ID: level-2-$i"
done
```

Đợi ít nhất 5 giây để Prometheus scrape.

## Bước 4: PromQL

Mở [Prometheus](http://127.0.0.1:9090) và chạy từng query.

Tổng request:

```promql
sum(kong_http_requests_total)
```

Tổng request theo status:

```promql
sum(kong_http_requests_total) by (code)
```

Request rate:

```promql
sum(rate(kong_http_requests_total[1m]))
```

Đọc từ trong ra ngoài:

1. lấy counter `kong_http_requests_total`;
2. `rate(...[1m])` tính tốc độ tăng trung bình mỗi giây trong một phút;
3. `sum(...)` cộng các Route/Service/Consumer series.

Request rate theo status:

```promql
sum(rate(kong_http_requests_total[1m])) by (code)
```

Tỷ lệ lỗi 5xx:

```promql
100 * sum(rate(kong_http_requests_total{code=~"5.."}[5m]))
  / clamp_min(sum(rate(kong_http_requests_total[5m])), 0.001)
```

## Bước 5: P95

```promql
histogram_quantile(
  0.95,
  sum(rate(kong_request_latency_ms_bucket[5m])) by (le)
)
```

Nếu kết quả là `200 ms`, khoảng 95% request hoàn thành trong tối đa 200 ms và
5% request còn lại chậm hơn. P95 cho thấy tail latency tốt hơn average.

So sánh ba lớp:

```promql
histogram_quantile(0.95, sum(rate(kong_request_latency_ms_bucket[5m])) by (le))
histogram_quantile(0.95, sum(rate(kong_upstream_latency_ms_bucket[5m])) by (le))
histogram_quantile(0.95, sum(rate(kong_kong_latency_ms_bucket[5m])) by (le))
```

| Metric           | Đo gì?                             |
|------------------|------------------------------------|
| Request latency  | Tổng Kong + upstream               |
| Upstream latency | Thời gian Payment Service phản hồi |
| Kong latency     | Routing và plugin trong Kong       |

## Bước 6: Target health metric

```promql
kong_upstream_target_health{
  upstream="payment-observability-upstream",
  state="healthy"
}
```

Giá trị `1` nghĩa là series “healthy” đang đúng cho Target đó.

## Checklist

- [ ] Phân biệt counter, gauge và histogram.
- [ ] Biết Prometheus pull `/metrics` mỗi 5 giây.
- [ ] Giải thích được vì sao counter cần `rate()`.
- [ ] Viết được query nhóm request theo HTTP status.
- [ ] Giải thích được P95 bằng lời của mình.
- [ ] Phân biệt request, upstream và Kong latency.

Next: [Grafana dashboard](03-grafana-dashboard.md).
