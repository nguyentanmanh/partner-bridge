# Terminal và structured log

Điều tra một request cụ thể:

```text
Request vào Route nào?
  -> được gửi tới Target nào?
  -> status do Kong hay upstream tạo?
  -> Kong và upstream mất bao lâu?
```

## Bước 1: Theo dõi log realtime

```bash
docker compose -f compose.yaml -f compose.kong-observability.yaml logs -f \
  kong-observability-lab payment-observability-a payment-observability-b
```

Giữ terminal này mở. `Ctrl+C` chỉ ngừng xem log, không dừng container.

## Bước 2: Gửi request có ID dễ tìm

Mở terminal khác:

```bash
curl -i http://localhost:8200/api/v1/payments/00000000-0000-0000-0000-000000000000 \
  -H 'X-Api-Key: observability-lab-key' \
  -H 'Request-ID: level-1-valid-key'
```

Kỳ vọng `404`: API key đã qua Kong, nhưng payment không tồn tại ở backend.

Thử thiếu key:

```bash
curl -i http://localhost:8200/api/v1/payments/00000000-0000-0000-0000-000000000000 \
  -H 'Request-ID: level-1-no-key'
```

Kỳ vọng `401` do Kong `key-auth` tạo.

## Bước 3: Tìm request

```bash
docker compose -f compose.yaml -f compose.kong-observability.yaml logs \
  kong-observability-lab payment-observability-a payment-observability-b \
  | grep 'level-1-valid-key'
```

Kong ghi một JSON structured log gần giống:

```json
{
  "request_id": "level-1-valid-key",
  "upstream_status": "404",
  "latencies": {"kong": 4, "proxy": 13, "request": 19}
}
```

## Bước 4: Đọc các trường quan trọng

| Trường | Ý nghĩa |
| --- | --- |
| `request_id` | ID nối log giữa các thành phần |
| `request.method/uri` | Request nào được gọi |
| `service/route` | Entity Kong đã match |
| `tries[].ip/port` | Target thực sự được chọn |
| `upstream_status` | Status backend trả Kong |
| `response.status` | Status cuối cùng Kong trả client |
| `latencies.kong` | Routing và plugin trong Kong |
| `latencies.proxy` | Thời gian chờ upstream |
| `latencies.request` | Tổng thời gian request tại Kong |

`upstream_status` tồn tại cho biết Kong đã gọi được upstream. Với `401` bị
`key-auth` chặn sớm, request không tới Payment Service.

## Bước 5: Xác định owner của lỗi

| Status | Owner thường gặp trong lab |
| --- | --- |
| `401` | Kong `key-auth` |
| `429` | Kong `rate-limiting` |
| `400` | Payment Service validation |
| `404` | Payment Service |
| `502/503` | Kong hoặc backend tùy tình huống |
| `504` | Backend provider timeout hoặc Kong read timeout |

Status chưa đủ để kết luận. Hãy xem `source`, `upstream_status`, response body
và log backend.

## Thực hành

1. Gửi ba request với ba `Request-ID` khác nhau.
2. Xác định request nào vào Target A, request nào vào B.
3. So sánh `latencies.kong` với `latencies.proxy`.
4. Gửi request thiếu key và chứng minh backend không nhận request.

## Checklist

- [ ] Tìm được một request bằng `Request-ID`.
- [ ] Đọc được Target IP/port trong structured log.
- [ ] Phân biệt Kong latency, proxy latency và total request latency.
- [ ] Giải thích được vì sao `401` không có upstream processing.
- [ ] không kết luận owner của lỗi chỉ dựa vào status code.

Next: [Prometheus và PromQL](02-prometheus-metrics.md).
