# Timeout, retry và dữ liệu phân tán

“Kong chuyển được traffic sang B” chưa có nghĩa payment data và retry đã an toàn.

## Ví dụ: hai nhân viên có hai cuốn sổ riêng

A và B cùng xử lý hồ sơ nhưng mỗi người giữ một cuốn sổ:

```text
A đã ghi payment P vào sổ A
B mở sổ B và không thấy P
```

Tổng đài có thể chuyển cuộc gọi từ A sang B, nhưng không tự sao chép hai cuốn
sổ. Trong lab, mỗi Payment Service giữ payment và idempotency trong memory
riêng.

## Thử Payment request thật

Thiếu API key, Kong trả `401`:

```bash
curl -i http://localhost:8100/api/v1/payments/00000000-0000-0000-0000-000000000000 \
  -H 'Request-ID: load-balancing-no-key'
```

Key hợp lệ, request tới backend và trả `404` vì payment không tồn tại:

```bash
curl -i http://localhost:8100/api/v1/payments/00000000-0000-0000-0000-000000000000 \
  -H 'X-Api-Key: load-balancing-lab-key' \
  -H 'Request-ID: load-balancing-valid-key'
```

Tạo payment:

```bash
curl -i http://localhost:8100/api/v1/payments \
  -H 'X-Api-Key: load-balancing-lab-key' \
  -H 'Request-ID: load-balancing-create-001' \
  -H 'Idempotency-Key: load-balancing-idem-001' \
  -H 'Content-Type: application/json' \
  -d '{"providerCode":"PROVIDER_A","merchantReference":"ORDER-SUCCESS-LB-001","amount":{"value":"100000","currency":"VND"},"description":"Load balancing lab"}'
```

Create có thể vào A, còn GET tiếp theo có thể vào B và trả `404`. Đây không
phải lỗi Kong; B thật sự chưa có payment trong memory của nó.

## Ba loại timeout

```yaml
connect_timeout: 2000
read_timeout: 10000
write_timeout: 10000
retries: 0
```

Ẩn dụ cuộc điện thoại:

| Timeout | Ví dụ                                | Ý nghĩa          |
|---------|--------------------------------------|---------------------------|
| Connect | Chờ bên kia bắt máy                  | Mở connection tới backend |
| Write   | Đọc hết nội dung yêu cầu cho bên kia | Gửi request xuống backend |
| Read    | Chờ bên kia trả lời                  | Chờ backend response      |

Provider timeout trong backend là 3 giây, ngắn hơn Kong read timeout 10 giây:

```text
Provider timeout sau 3s
  -> backend phân loại lỗi
  -> backend trả canonical 504
  -> Kong vẫn còn thời gian chuyển response cho client
```

Nếu Kong timeout trước backend, client chỉ nhận lỗi gateway chung.

## Vì sao retries bằng 0?

Tình huống unknown outcome:

```text
1. Kong gửi POST tới A
2. A tạo payment thành công
3. Response A -> Kong bị mất
4. Kong không biết operation đã thành công
5. Nếu retry sang B, B có thể tạo payment lần hai
```

“Không nhận được response” không đồng nghĩa với “operation chưa xảy ra”.

Retry GET thường an toàn hơn vì chỉ đọc. Retry POST payment cần shared durable
idempotency và thiết kế rõ ràng.

## Trước khi scale production cần gì?

- Shared database cho payment state.
- Durable idempotency store.
- Unique constraint cho idempotency key/reference.
- Transaction boundary rõ ràng.
- Provider hỗ trợ idempotency hoặc status inquiry/reconciliation.
- Chính sách xử lý unknown outcome.

## Responsibility boundary

| Concern                 | Owner                      |
|-------------------------|----------------------------|
| Chia traffic            | Kong                       |
| Phát hiện Target lỗi    | Kong                       |
| Failover traffic        | Kong                       |
| Payment persistence     | Payment Service/database   |
| Shared idempotency      | Payment Service/database   |
| Provider reconciliation | Payment application/domain |

## Xem log khi chẩn đoán

```bash
docker compose -f compose.yaml -f compose.kong-load-balancing.yaml logs --tail=100 \
  kong-load-balancing-lab payment-lab-a payment-lab-b
```

## Checklist

- [ ] Phân biệt connect, write và read timeout.
- [ ] Hiểu timeout budget giữa Kong, backend và provider.
- [ ] Giải thích được unknown outcome.
- [ ] Biết vì sao Payment POST giữ `retries: 0`.
- [ ] Biết failover traffic không đồng bộ dữ liệu.
- [ ] Liệt kê được điều kiện để retry/scale an toàn hơn.

> Kong chịu trách nhiệm routing, load balancing và failover traffic; ứng dụng
> và database chịu trách nhiệm data consistency, durable idempotency và retry
> an toàn.
