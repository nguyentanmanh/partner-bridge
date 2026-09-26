# Authentication và authorization

## Mục tiêu

Thử ba trường hợp để phân biệt rõ `401`, `403` và request đã qua gateway.

Dùng một payment ID không tồn tại để không tạo dữ liệu:

```bash
URL=http://localhost:8300/api/v1/payments/00000000-0000-0000-0000-000000000000
```

## Bước 1: Không gửi API key

```bash
curl -i "$URL" -H 'Request-ID: security-no-key'
```

Kết quả mong đợi: HTTP `401`.

Kong chưa biết người gọi là ai nên request dừng tại `key-auth`, chưa tới Payment
Service. Nếu bỏ `key-auth`, endpoint sẽ mất lớp nhận diện ở gateway.

## Bước 2: Key hợp lệ nhưng không có quyền

```bash
curl -i "$URL" \
  -H 'X-Api-Key: security-audit-reader-key' \
  -H 'Request-ID: security-forbidden'
```

Kết quả mong đợi: HTTP `403`.

Kong nhận ra consumer `audit-reader`, nhưng consumer này thuộc nhóm `audit-only`.
Plugin `acl` của route chỉ cho nhóm `payment-clients` đi qua. Đây là
authorization: đã biết bạn là ai nhưng bạn không có quyền vào.

Nếu chỉ có `key-auth` mà không có `acl`, mọi consumer có credential hợp lệ sẽ
gọi được Payment API.

## Bước 3: Key hợp lệ và có quyền

```bash
curl -i "$URL" \
  -H 'X-Api-Key: security-partner-a-key' \
  -H 'Request-ID: security-allowed'
```

Kết quả mong đợi: HTTP `404` với error body của Payment Service. `404` ở đây
không phải lỗi của bài: request đã qua cả authentication và authorization, sau
đó backend không tìm thấy payment ID.

## Xem cấu hình đã load

```bash
curl -s http://127.0.0.1:8301/consumers
curl -s http://127.0.0.1:8301/plugins
```

Admin API chỉ dùng để quan sát trong lab DB-less. Không gửi credential thật lên
đây và không mở cổng `8301` ra mạng ngoài.

Tiếp theo: [Trusted identity](02-trusted-identity.md).
