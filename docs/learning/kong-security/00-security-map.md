# Bản đồ bảo mật

## Mục tiêu

Sau bài này, bạn cần chỉ ra được ba ranh giới trust trong PartnerBridge và biết
Kong không thay thế phần bảo mật của backend hay provider.

```text
Internet / client không tin cậy
          |
          | X-Api-Key
          v
       [ Kong ]
          |
          | X-Authenticated-Client-Id do Kong tạo
          v
 [ Payment Service ]
          |
          | credential riêng của provider
          v
 [ Provider A / B ]
```

Có thể hình dung Kong là quầy bảo vệ của một tòa nhà. Thẻ ra vào giúp bảo vệ
biết bạn là ai, danh sách quyền cho biết bạn được vào tầng nào. Khi đã qua quầy,
phòng ban bên trong không đọc lại số thẻ; họ tin vào phiếu xác nhận do bảo vệ
cấp.

## Ba câu hỏi khác nhau

| Câu hỏi                              | Thành phần trả lời trong lab |
|--------------------------------------|------------------------------|
| Người gọi là ai?                     | `key-auth`                    |
| Người đó có quyền gọi Payment API?   | `acl`                         |
| Backend dùng identity nào làm scope? | `request-transformer`         |

Authentication và authorization không phải một việc. Một API key hợp lệ chỉ
chứng minh Kong biết consumer đó; consumer vẫn có thể không được phép gọi route.

## Tài sản cần bảo vệ

- Payment API và dữ liệu thuộc từng partner.
- Credential client dùng để vào Kong.
- Credential mà Payment Service dùng để gọi provider.
- Kong Admin API vì nó cho phép xem cấu hình gateway.
- Trusted identity header vì backend dùng nó làm client scope.

## Nếu bỏ từng lớp

| Bỏ lớp nào                  | Điều có thể xảy ra                                        |
|-----------------------------|-----------------------------------------------------------|
| `key-auth`                  | Người lạ có thể đi tới backend                            |
| `acl`                       | Mọi consumer có key hợp lệ đều gọi được mọi route         |
| Xóa và tạo lại identity     | Client có thể thử giả danh partner khác                   |
| Bắt buộc identity ở backend | Request đi vòng qua Kong có thể dùng default identity     |
| Giới hạn Admin API          | Máy khác có thể tiếp cận mặt quản trị nếu network cho phép |

Lab chỉ chứng minh các nguyên tắc trên trong môi trường local. Nó chưa có TLS,
mTLS, secret manager hoặc network policy production.

Tiếp theo: [Authentication và authorization](01-authn-authz.md).
