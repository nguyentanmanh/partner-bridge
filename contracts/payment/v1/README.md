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
- Response header trả lại `Request-ID` của request hiện tại. Với lỗi lần đầu,
  `ApiError.requestId` cũng chứa giá trị đó. Khi replay lỗi đã lưu, toàn bộ body
  giữ nguyên `requestId`, `traceId`, `timestamp` và message/ngôn ngữ của lần đầu;
  header vẫn phục vụ correlation cho lần replay hiện tại.
- `Request-ID` dùng cho observability, không thay thế `Idempotency-Key`.

## Quy tắc Idempotency-Key

- `Idempotency-Key` bắt buộc với `POST /api/v1/payments` và không dùng cho `GET`.
- Key do client tạo, dài từ 1 đến 128 ký tự ASCII hiển thị, không có khoảng trắng,
  và được scope theo client.
- Gửi lại cùng key với cùng canonical request phải cho cùng logical result và
  không tạo thêm payment/provider transaction.
- Dùng lại key với payload khác trả về HTTP `409`.
- `merchantReference` cũng unique theo client. Reference trùng trả về HTTP `409`.
- Trong MVP, client scope lấy từ `partnerbridge.client.default-id`, mặc định
  `local-poc-client`; không nhận client ID từ header tùy ý. Khi có authentication,
  scope phải lấy từ trusted identity do gateway truyền xuống.
- Khi chạy qua gateway, client scope lấy từ `X-Authenticated-Client-Id` do Kong
  thiết lập sau khi xác thực. Request thiếu header này bị từ chối. Default client
  chỉ dùng khi chạy service trực tiếp ở local, không dùng cho Compose gateway.
- Trong gateway profile, GET payment cũng chỉ trả payment thuộc client scope đã
  xác thực; payment thuộc client khác trả `404` để không lộ dữ liệu.
- Reservation cho `(client scope, key)` và `(client scope, merchantReference)`
  được giữ nguyên tử trước khi gọi provider. Cùng reference nhưng key khác trả
  `409`, kể cả khi request trước còn xử lý hoặc chưa xác định kết quả.
- Cùng key/body đồng thời chờ cùng một kết quả; chỉ một request được gọi provider.
  Không giữ global lock trong khi gọi HTTP hoặc chờ kết quả. Payment độc lập và
  GET không phải chờ provider của payment khác.
- So sánh các trường của canonical body đã validate, không tính thứ tự JSON,
  correlation ID, key hoặc các header. Không tự chuẩn hóa giá trị tiền hoặc chuỗi
  description: body khác giá trị vẫn là conflict.
- Replay thành công giữ nguyên HTTP `201` và toàn bộ `PaymentResponse` lần đầu.
  Replay lỗi giữ nguyên HTTP status và toàn bộ `ApiError` đã lưu, kể cả khi
  Request-ID hoặc Accept-Language của lần replay thay đổi. Không thêm replay header.

## Kết quả lỗi và trạng thái nội bộ

Trạng thái xử lý nội bộ gồm `IN_FLIGHT`, `COMPLETED`, `UNKNOWN`, tách biệt với
canonical `PaymentStatus`: `PENDING`, `SUCCEEDED`, `FAILED`. `SUCCESS` là mã
provider/scenario, không phải giá trị status của canonical API.

- Khi đã bắt đầu gọi provider, exception không giải phóng reservation.
- Timeout trả `504`, response không hợp lệ trả `502`. Kết quả thanh toán chưa
  xác định được giữ ở trạng thái xử lý nội bộ `UNKNOWN`; không gán `FAILED`.
- Business rejection vẫn trả `422` và được lưu để replay. Không đổi scenario
  `FAIL` thành `201/FAILED`. `FAILED` chỉ được map khi provider xác nhận trạng thái
  tương ứng trong một response payment hợp lệ (`REJECTED` hoặc `F`).
- Các lỗi provider và lỗi hệ thống sau khi bắt đầu xử lý cũng được ghi nhận để
  replay không tạo cuộc gọi create mới. Không có retry tự động hoặc reconciliation.
- Log tách `callOutcome` (ví dụ `TIMEOUT`, `INVALID_RESPONSE`) khỏi
  `paymentStatus`; `UNKNOWN` trong log không phải enum canonical mới.

## Retention và giới hạn POC

`partnerbridge.idempotency.retention` (environment variable
`PARTNERBRIDGE_IDEMPOTENCY_RETENTION`) mặc định `24h`, bắt buộc lớn hơn 0. Clock
được inject để kiểm thử expiry mà không phải chờ thời gian thật.

TTL response chỉ bắt đầu sau khi lần xử lý đầu kết thúc, không expire
`IN_FLIGHT`. Cleanup response thực hiện khi có create request. Reservation của
`merchantReference` giữ suốt vòng đời process, kể cả với `UNKNOWN`, không bị
xóa theo TTL. Sau TTL, dùng lại reference đã giữ trả `409` và không gọi provider;
key đã expire chỉ có thể dùng với một reference mới chưa được giữ.

Store chỉ nằm trong memory của một instance. Restart mất toàn bộ record,
payment và reservation; không có bảo đảm idempotency qua restart hoặc nhiều
instance. Dữ liệu reference và payment tiếp tục tăng theo thời gian. Đây là cơ
chế học tập cho POC, chưa phải thiết kế production. GET chỉ đọc snapshot payment
đã tạo; chưa có polling hoặc reconciliation để cập nhật trạng thái từ provider.

## Validation canonical JSON

`amount.value` phải là JSON string; number/boolean/null bị từ chối. Các object
`CreatePaymentRequest` và `Money` không nhận field ngoài schema, bao gồm
`callbackUrl`. Field optional có thể bị bỏ qua nhưng không nhận explicit null
khi schema không khai báo nullable. Request không hợp lệ trả `400` và không
gọi provider. Quy tắc deserialization nghiêm ngặt chỉ áp dụng cho Payment API.

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
