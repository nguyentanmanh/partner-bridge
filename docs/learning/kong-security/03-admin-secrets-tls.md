# Admin API, secret và TLS

## Admin API không phải public API

Proxy `8300` là cửa cho client. Admin API `8301` là cửa quản trị Kong. Trong
Compose, cổng quản trị được bind như sau:

```yaml
ports:
  - "8300:8300"
  - "127.0.0.1:8301:8301"
```

`127.0.0.1` nghĩa là chỉ máy đang chạy Docker truy cập được qua host port. Nếu
đổi thành `8301:8301`, cổng có thể lắng nghe trên mọi network interface của máy.
Đây chưa phải cơ chế bảo vệ đủ cho production, nhưng giúp tránh mở nhầm Admin API
trong lab.

Kiểm tra binding:

```bash
docker compose -f compose.yaml -f compose.kong-security.yaml ps kong-security-lab
```

Kết quả mong đợi: proxy hiển thị `0.0.0.0:8300`, Admin API hiển thị
`127.0.0.1:8301`.

## Credential trong lab

Các API key nằm thẳng trong `kong.yml` để người học nhìn thấy quan hệ giữa
Consumer và Credential. Cách này chỉ phù hợp với dữ liệu giả.

Không commit credential thật vào Git. Production cần ít nhất:

- Nơi lưu secret riêng và quyền đọc tối thiểu.
- Quy trình cấp, đổi và thu hồi credential.
- Không ghi API key, Authorization header hoặc provider response nhạy cảm vào log.
- Credential khác nhau giữa development, staging và production.

`hide_credentials: true` ngăn `X-Api-Key` được chuyển tiếp lên Payment Service.
Nếu tắt cấu hình này, credential đi thêm một chặng không cần thiết và có thể xuất
hiện trong log hoặc error của backend.

## HTTP của lab và HTTPS production

Lab dùng HTTP để dễ quan sát request. HTTP không mã hóa API key hay payload trên
đường truyền. Khi triển khai thật cần TLS ở phía client → Kong. Với đường
Kong → backend, dùng private network có kiểm soát và cân nhắc mTLS nếu backend
cần xác minh chính Kong là bên gọi.

TLS trả lời “đường truyền có được mã hóa và đầu bên kia có đúng không?”. Nó
không thay thế authentication, authorization hoặc kiểm tra owner dữ liệu.

## Community và Enterprise

Phần thực hành của bài này chạy hoàn toàn bằng Kong Community: `key-auth`, `acl`,
`request-transformer` và giới hạn host binding. Khi học Enterprise, có thể mở
rộng sang quản trị người dùng Admin API/Kong Manager, RBAC, audit log, OIDC và
quản lý nhiều team/workspace. Các tính năng đó không được giả lập trong lab này.

## Checklist kết thúc bài

- [ ] Giải thích được `401` khác `403`.
- [ ] Key hợp lệ nhưng sai ACL group bị từ chối.
- [ ] Header giả mạo không quyết định client scope.
- [ ] Partner khác không đọc được payment đã tạo.
- [ ] Admin API chỉ bind vào loopback của host.
- [ ] Biết vì sao key trong Git chỉ được là dữ liệu giả.
- [ ] Biết TLS không thay thế authentication và authorization.
