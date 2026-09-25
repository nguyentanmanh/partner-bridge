# Round-robin và weight

Kong chia request giữa hai backend và hiểu `weight` là tỷ lệ tương
đối, không phải quota tuyệt đối.

## Ví dụ: phát hồ sơ lần lượt

Đội có hai nhân viên A và B. Người điều phối phát:

```text
Hồ sơ 1 -> A
Hồ sơ 2 -> B
Hồ sơ 3 -> A
Hồ sơ 4 -> B
```

Đó là round-robin. Không quan trọng hồ sơ đầu tiên vào A hay B; điều quan trọng
là cả hai cùng nhận việc tương đối đều.

## Cấu hình

```yaml
algorithm: round-robin
targets:
  - target: payment-lab-a:8080
    weight: 100
  - target: payment-lab-b:8080
    weight: 100
```

`100/100` nghĩa là tỷ lệ bằng nhau. Ví dụ `80/20` hướng khoảng 80% traffic tới
A và 20% tới B trong một lượng request đủ lớn; không đảm bảo đúng tỷ lệ ở một
nhóm request rất nhỏ.

## Quan sát bằng endpoint

```bash
for i in {1..6}; do
  curl -s http://localhost:8100/lab/instance
  echo
done
```

Kết quả gần giống:

```json
{
  "instance": {
    "id": "payment-lab-a"
  }
}
{
  "instance": {
    "id": "payment-lab-b"
  }
}
```

Endpoint `/lab/instance` được Kong chuyển tới `/actuator/info` của Target đã
chọn. Nó chỉ phục vụ việc nhìn thấy load balancing trong lab.

## Khi nào dùng weight khác nhau?

- Canary: phiên bản mới chỉ nhận ít traffic.
- Backend có năng lực xử lý khác nhau.
- Chuyển traffic dần từ phiên bản cũ sang phiên bản mới.

Weight không giới hạn số request và không thay thế rate limiting.

## Thực hành

1. Gọi 20 request và đếm A/B.
2. Đổi weight thành `A=80`, `B=20` trong file lab.
3. Restart `kong-load-balancing-lab` và gọi ít nhất 100 request.
4. Đổi lại `100/100` sau khi hoàn thành.

Restart sau khi sửa declarative config:

```bash
docker compose -f compose.yaml -f compose.kong-load-balancing.yaml restart \
  kong-load-balancing-lab
```

## Checklist

- [ ] Nhìn thấy cả A và B nhận request.
- [ ] Biết request đầu tiên không được đảm bảo vào A.
- [ ] Giải thích được `100/100` và `80/20`.
- [ ] Biết weight không phải rate limit.

Next: [Health check và failover](02-health-failover.md).
