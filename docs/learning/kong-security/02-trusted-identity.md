# Trusted identity

## Vấn đề cần giải quyết

Payment Service dùng client identity để scope idempotency và quyền đọc payment.
Nếu backend tin thẳng header do client gửi, Partner A có thể tự ghi
`X-Authenticated-Client-Id: partner-b` để giả danh Partner B.

Lab dùng `request-transformer` theo hai bước:

1. Xóa `X-Authenticated-Client-Id` và `X-Client-Id` do client gửi.
2. Tạo lại `X-Authenticated-Client-Id` từ consumer mà `key-auth` đã xác thực.

## Bước 1: Thử giả danh Partner B

```bash
curl -i http://localhost:8300/api/v1/payments \
  -H 'X-Api-Key: security-partner-a-key' \
  -H 'X-Authenticated-Client-Id: partner-b' \
  -H 'X-Client-Id: partner-b' \
  -H 'Request-ID: security-spoof-test' \
  -H 'Idempotency-Key: security-spoof-idem-1' \
  -H 'Content-Type: application/json' \
  -d '{"providerCode":"PROVIDER_A","merchantReference":"SECURITY-SPOOF-001","amount":{"value":"100000","currency":"VND"},"description":"Security lab"}'
```

Kết quả mong đợi: HTTP `201`. Request thành công không có nghĩa spoof thành
công. Payment được tạo dưới scope `partner-a`, vì API key thuộc Partner A và hai
header giả đã bị Kong xóa.

Sao chép `paymentId` trong response để dùng ở hai lệnh sau:

```bash
PAYMENT_ID=<paymentId-vừa-nhận>
```

## Bước 2: Đọc bằng đúng consumer

```bash
curl -i "http://localhost:8300/api/v1/payments/$PAYMENT_ID" \
  -H 'X-Api-Key: security-partner-a-key' \
  -H 'Request-ID: security-owner-read'
```

Kết quả mong đợi: HTTP `200`.

## Bước 3: Đọc bằng consumer khác

```bash
curl -i "http://localhost:8300/api/v1/payments/$PAYMENT_ID" \
  -H 'X-Api-Key: security-partner-b-key' \
  -H 'Request-ID: security-other-read'
```

Kết quả mong đợi: HTTP `404`. Backend cố ý không trả `403`, tránh xác nhận rằng
payment của partner khác có tồn tại.

## Vì sao backend vẫn phải kiểm tra?

Gateway tạo trusted identity nhưng Payment Service mới là nơi sở hữu dữ liệu và
quy tắc scope. Nếu backend bỏ kiểm tra owner, Kong không thể tự biết payment nào
thuộc partner nào.

Ngược lại, nếu backend được mở trực tiếp ra host hoặc đặt trong network không
đáng tin cậy, một client có thể bỏ qua Kong và tự gửi trusted header. Production
cần network policy hoặc mTLS giữa gateway và backend; tên header không tự tạo ra
trust.

Tiếp theo: [Admin API, secret và TLS](03-admin-secrets-tls.md).
