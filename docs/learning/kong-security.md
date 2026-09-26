# Kong Security Fundamentals

Bài này chưa bắt đầu bằng TLS hay JWT. Trước hết cần hiểu request đi qua những
ranh giới nào, ai được nhận diện, ai được phép gọi và backend tin dữ liệu nào.

| Bài học                                                           | Câu hỏi cần trả lời                                     |
|--------------------------------------------------------------------|---------------------------------------------------------|
| [Bản đồ bảo mật](kong-security/00-security-map.md)                 | Ta bảo vệ tài sản nào và không tin dữ liệu từ đâu?      |
| [Authentication và authorization](kong-security/01-authn-authz.md) | Vì sao key đúng vẫn có thể bị từ chối?                  |
| [Trusted identity](kong-security/02-trusted-identity.md)           | Làm sao ngăn client tự nhận mình là partner khác?       |
| [Admin API, secret và TLS](kong-security/03-admin-secrets-tls.md)  | Lab đã bảo vệ gì và production còn thiếu gì?            |

```text
Nhận diện người gọi
    ↓
Kiểm tra quyền
    ↓
Tạo identity đáng tin cho backend
    ↓
Thu hẹp bề mặt quản trị và bảo vệ đường truyền
```

## Khởi động lab

Đứng tại repository root:

```bash
cd /Users/manhnguyen/Works/TMO/projects/partner-bridge
docker compose -f compose.yaml -f compose.kong-security.yaml config --quiet
docker compose -f compose.yaml -f compose.kong-security.yaml up -d --build \
  provider-a provider-b payment-security-lab kong-security-lab
```

Kiểm tra:

```bash
docker compose -f compose.yaml -f compose.kong-security.yaml ps
curl -s http://127.0.0.1:8301/status
```

Kết quả mong đợi: bốn container của lab ở trạng thái `healthy`; Admin API trả
thông tin node Kong.

| Thành phần         | URL                     |
|--------------------|-------------------------|
| Kong proxy của lab | `http://localhost:8300` |
| Kong Admin API     | `http://127.0.0.1:8301` |

## Danh tính dùng trong lab

| Consumer       | API key                     | Nhóm              | Được gọi Payment API |
|----------------|-----------------------------|-------------------|-----------------------|
| `partner-a`    | `security-partner-a-key`    | `payment-clients` | Có                    |
| `partner-b`    | `security-partner-b-key`    | `payment-clients` | Có                    |
| `audit-reader` | `security-audit-reader-key` | `audit-only`      | Không                  |

Đây là credential giả và được commit chỉ để học local. Không dùng cách này với
secret thật.

## Bản đồ file

| File                                   | Vai trò                                     |
|----------------------------------------|---------------------------------------------|
| `compose.kong-security.yaml`           | Backend và Kong riêng của lab               |
| `gateways/kong/labs/security/kong.yml` | Consumer, credential, ACL và trusted header |
| `docs/learning/kong-security/`         | Các bài thực hành                           |

## Dừng lab

```bash
docker compose -f compose.yaml -f compose.kong-security.yaml stop \
  kong-security-lab payment-security-lab
```

Không dùng `down` nếu gateway chính hoặc lab khác vẫn đang chạy.
