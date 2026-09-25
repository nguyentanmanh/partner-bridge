# Health check và failover

Làm một Target ngừng hoạt động, quan sát Kong loại nó khỏi pool, rồi đưa nó trở
lại khi hồi phục.

## Ví dụ: tổng đài gọi thử nhân viên

Tổng đài định kỳ gọi thử từng nhân viên:

```text
“A có online không?” -> Có
“B có online không?” -> Không lần 1
“B có online không?” -> Không lần 2
                         ↓
               Ngừng chuyển cuộc gọi cho B
```

Khi B trả lời lại, tổng đài đưa B về đội nhận việc.

## Active health check hiện tại

Kong gọi `/actuator/health` mỗi 5 giây:

```yaml
healthy:
  interval: 5
  successes: 1
unhealthy:
  interval: 5
  http_failures: 2
  tcp_failures: 2
  timeouts: 2
```

Hai lần lỗi liên tiếp làm Target `UNHEALTHY`; một lần thành công đưa Target trở
lại `HEALTHY`.

## Docker healthcheck khác Kong health check

| Cơ chế | Dùng để làm gì? |
| --- | --- |
| Docker healthcheck | Container state và thứ tự startup trong Compose |
| Kong health check | Quyết định Target có được nhận API traffic không |

Container có thể còn `running`, nhưng Kong vẫn loại nó nếu endpoint health
không đáp ứng đúng.

## Bước 1: Xem trạng thái ban đầu

```bash
curl -s http://127.0.0.1:8101/upstreams/payment-lab-upstream/health
```

Cả A và B phải `HEALTHY`.

## Bước 2: Làm Target B lỗi

```bash
docker compose -f compose.yaml -f compose.kong-load-balancing.yaml stop \
  payment-lab-b
```

Đợi 10–15 giây rồi xem lại health. B phải thành `UNHEALTHY`.

## Bước 3: Quan sát failover

```bash
for i in {1..4}; do
  curl -s http://localhost:8100/lab/instance
  echo
done
```

Mọi response phải đến từ `payment-lab-a`. Kong đã loại B khỏi vòng phân phối.

## Bước 4: Khôi phục

```bash
docker compose -f compose.yaml -f compose.kong-load-balancing.yaml start \
  payment-lab-b
```

Đợi B healthy và một chu kỳ Kong check:

```bash
docker compose -f compose.yaml -f compose.kong-load-balancing.yaml ps
curl -s http://127.0.0.1:8101/upstreams/payment-lab-upstream/health
```

Gọi `/lab/instance` lại; A và B phải cùng xuất hiện.

## Điều cần nhớ

Failover giảm lỗi do gửi request vào backend đã chết, nhưng không đảm bảo dữ
liệu của A đã tồn tại ở B.

## Checklist

- [ ] Phân biệt Docker healthcheck và Kong health check.
- [ ] Quan sát B chuyển `HEALTHY -> UNHEALTHY`.
- [ ] Thấy mọi request chuyển sang A.
- [ ] Khôi phục B và thấy B trở lại round-robin.
- [ ] Biết Prometheus/Grafana chỉ quan sát, không thực hiện failover.

Next: [Timeout, retry và dữ liệu phân tán](03-timeout-retry-state.md).
