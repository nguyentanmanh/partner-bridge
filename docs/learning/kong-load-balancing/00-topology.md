# Route, Service, Upstream và Target

## Mục tiêu

Trước khi cân bằng tải, phải phân biệt bốn entity:

```text
Route -> Service -> Upstream -> Target A
                            `-> Target B
```

## Ví dụ: tổng đài và đội xử lý hồ sơ

Ví dụ Kong là tổng đài của một công ty:

| Kong     | Ví dụ                                                    |
|----------|----------------------------------------------------------|
| Route    | Quy tắc nhận diện: “cuộc gọi này thuộc phòng thanh toán” |
| Service  | Phiếu hướng dẫn chuyển cuộc gọi và được chờ bao lâu      |
| Upstream | Tên của cả đội xử lý thanh toán                          |
| Target   | Một nhân viên cụ thể trong đội                           |

Tổng đài không chuyển cuộc gọi thẳng tới “một người cố định”. Nó chuyển tới
đội thanh toán, rồi chọn một nhân viên đang làm việc.

## Đọc cấu hình thực tế

Service trỏ tới tên Upstream:

```yaml
services:
  - name: payment-lab-service
    host: payment-lab-upstream
    port: 8080
```

Upstream chứa hai Target thật:

```yaml
upstreams:
  - name: payment-lab-upstream
    targets:
      - target: payment-lab-a:8080
      - target: payment-lab-b:8080
```

`host: payment-lab-upstream` khớp tên Kong Upstream nên Kong sử dụng load
balancer thay vì chỉ DNS tới một backend.

## Xem entity qua Admin API

```bash
curl -s http://127.0.0.1:8101/services
curl -s http://127.0.0.1:8101/routes
curl -s http://127.0.0.1:8101/upstreams
curl -s http://127.0.0.1:8101/upstreams/payment-lab-upstream/targets/all
```

Tìm:

- Route `payment-lab-route`;
- Service `payment-lab-service`;
- Upstream `payment-lab-upstream`;
- Targets `payment-lab-a:8080` và `payment-lab-b:8080`.

## Điều không được nhầm

- Kong Service không phải Java process/container.
- Upstream không phải payment provider A/B.
- Target là Payment Service instance, không phải provider stub.
- Java Payment Service vẫn chọn `PROVIDER_A` hoặc `PROVIDER_B` từ request body.

## Thực hành

Vẽ lại đường đi của `GET /api/v1/payments/{id}` và ghi tên entity cụ thể của
lab vào từng ô.

## Checklist

- [ ] Phân biệt Route, Service, Upstream và Target.
- [ ] Biết Service trỏ tới Upstream bằng trường `host`.
- [ ] Biết một Upstream có nhiều Target.
- [ ] Không nhầm Target với payment provider.

Next: [Round-robin và weight](01-round-robin.md).
