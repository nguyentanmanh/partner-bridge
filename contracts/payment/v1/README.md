# PartnerBridge Canonical Payment API v1

## Mục tiêu

Contract này là nguồn chuẩn dùng chung cho Payment API của PartnerBridge. Nó mô
tả payload, header, status code và error contract mà backend, API gateway và bộ
E2E test phải cùng tuân thủ, không phụ thuộc vào sản phẩm gateway hay cách tích
hợp nội bộ với từng provider.

Phạm vi v1 MVP chỉ gồm tạo payment và tra cứu trạng thái payment.

## Canonical model

Canonical model là mô hình trung lập của PartnerBridge. Client chỉ làm việc với
các khái niệm chung như `Money`, `PaymentResponse`, `PaymentStatus` và hai mã
logic `PROVIDER_A`, `PROVIDER_B`. Payload, credential, endpoint và mã trạng thái
riêng của provider không được rò rỉ qua API này. Adapter của từng provider chịu
trách nhiệm chuyển đổi giữa mô hình riêng và canonical model.

## Biểu diễn amount

`Money.value` là `string`, không phải `number` hay `double`, để tránh sai số
floating point khi truyền và xử lý tiền. Giá trị dùng dạng thập phân thuần,
dương, không có dấu, exponent hoặc dấu phân cách hàng nghìn; ví dụ `"100000"`.
`Money.currency` là mã tiền tệ ba ký tự viết hoa, ví dụ `VND`.

Contract chỉ kiểm tra hình dạng chung của giá trị. Các quy tắc như số chữ số
thập phân cho từng currency, giới hạn giao dịch và currency mà provider hỗ trợ
là business validation và có thể trả về HTTP `422`.

## Quy tắc Request-ID

- `Request-ID` bắt buộc trên cả `POST` và `GET`.
- Caller tạo giá trị này cho mỗi logical request; độ dài từ 1 đến 128 ký tự ASCII
  hiển thị, không có khoảng trắng.
- Gateway và backend phải chuyển tiếp nguyên giá trị để correlation trong log,
  trace và error response.
- Response trả lại cùng `Request-ID`. Trường `requestId` trong `ApiError` cũng
  chứa giá trị đó.
- `Request-ID` dùng cho observability, không thay thế `Idempotency-Key`.

## Quy tắc Idempotency-Key

- `Idempotency-Key` bắt buộc với `POST /api/v1/payments` và không dùng cho `GET`.
- Key do client tạo, dài từ 1 đến 128 ký tự ASCII hiển thị, không có khoảng trắng,
  và được scope theo client.
- Gửi lại cùng key với cùng canonical request phải cho cùng logical result và
  không tạo thêm payment/provider transaction.
- Dùng lại key với payload khác trả về HTTP `409`.
- `merchantReference` cũng unique theo client. Reference trùng trả về HTTP `409`.
- Thời gian lưu idempotency record và hành vi HTTP cụ thể khi replay một request
  đã thành công cần được SA chốt trước khi implement.

## Cách sử dụng contract

- Backend dùng contract để implement request validation, canonical DTO, response
  mapping và error handling.
- Gateway import hoặc tham chiếu contract để cấu hình route, kiểm tra các header
  bắt buộc, request/response validation và policy mà không sửa nghĩa của API.
- E2E test sinh hoặc viết test trực tiếp từ operation, schema, example và status
  code trong contract.
- Mọi thay đổi hành vi public phải bắt đầu từ contract và được review trước khi
  đồng bộ sang backend, gateway và test.

## Ngoài phạm vi MVP

- Refund, cancel, capture và các thao tác payment khác.
- Callback/webhook và callback URL do client cung cấp.
- Authentication/authorization phức tạp; cơ chế xác định client sẽ được thiết kế
  ở bước riêng.
- Payload, credential, endpoint hoặc tên thật của provider.
- API quản trị provider, reconciliation, settlement và reporting.
- Gateway-specific extension cho Kong, WSO2, MuleSoft hoặc sản phẩm khác.
