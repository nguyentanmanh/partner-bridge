# Đánh giá TAD CLI với PartnerBridge/Kong

Đánh giá được thực hiện trên `../tad-cli` tại commit
`c92557dbf3eb6289b3a9f9a3995909e1e09c8d16`, package `@deepseek-ai/dsh` phiên
bản `0.1.1-rc.2`. Working tree của TAD sạch trước và sau khi kiểm tra. Các thay
đổi tích hợp Kong đã có sẵn trong PartnerBridge và không thuộc phạm vi đánh giá
này.

## TAD hiện làm gì

TAD là bản fork của DeepSeek Harness: một agent làm việc với source/file/shell qua các plugin, thêm SSO và quản lý phiên Arkan. CLI trong `apps/cli/src/args.ts` nhận task dạng prompt cho profile `headless`, `tui` hoặc `web`; nó không có command nhận OpenAPI, import gateway config, quản lý Kong Service/Route, hoặc tạo Kong declarative YAML. Package `packages/api/gateway` là RPC Host/Client nội bộ của TAD, không phải connector Kong. Tìm kiếm `kong` trong source và package lockfile không thấy integration/package Kong.

Command tree từ `tad --help` và source:

```text
tad [--profile <name> [task/arguments]] [--patch <file>]
    [--dump-config | --dump-default-config]
tad web [arguments]
tad plugin --profile <name> <pnpm arguments>
tad login | logout | whoami | token | status | workorders
tad session run | machine | workorder | link | register
tad --version
```

`--dump-config` chỉ compose các lớp cấu hình plugin của **TAD**; nó không đọc Kong hoặc tạo diff gateway. `plugin` có thể sửa profile/dependency dưới `$DSH_HOME`. Một phiên agent có các công cụ đọc/sửa file và chạy shell; chúng có thể tác động tới repository hoặc local Admin API theo quyền chạy và cấu hình sandbox/approval, nhưng đây là khả năng agent tổng quát, không phải quy trình Kong có schema, plan/diff và apply riêng. Công cụ file có thể trình bày diff; plan mode là hướng dẫn có người review, còn sandbox/approval là chính sách độc lập. Không có rollback Kong tự động được xác định trong source.

Profile `headless` mặc định định tuyến model DeepSeek qua tham chiếu credential `DEEPSEEK_API_KEY`; `DEEPSEEK_BASE_URL` là endpoint tùy chọn. Profile khác có thể chọn model/credential khác, ví dụ `VERTEX_KEY_API_KEY` trong tài liệu cài đặt. Các lệnh Arkan dùng SSO, binding/machine/work order và lease riêng. Chạy một prompt agent có thể gửi nội dung task và dữ liệu agent đọc được tới AI provider đã cấu hình. Trong môi trường đánh giá, các biến `DEEPSEEK_API_KEY`, `VERTEX_KEY_API_KEY`, `ARKAN_LEASE_ID` và `ARKAN_WORKORDER_ID` đều vắng mặt; chỉ kiểm tra tên biến, không đọc giá trị secret từ nơi khác. Vì vậy không chạy prompt với contract/source và không gọi dịch vụ bên ngoài.

## Chạy local và kết quả thử nghiệm

Repository khai `pnpm@11.7.0` và `pnpm-lock.yaml`; README của fork yêu cầu Node >=24. Đã dùng Node 24.19.0, `pnpm@11.7.0` qua cache `pnpm dlx`, `pnpm install --frozen-lockfile` và `pnpm run build`. Build script còn gọi `npm`, nên đã cấp `npm` qua cache `pnpm dlx` thay vì cài toàn cục. Không thay package/lockfile. `tad --version` trả `0.1.1-rc.2`; `tad --help` in command tree trên. `--profile headless --dump-default-config` với `$DSH_HOME` tạm trong `/tmp` trả 333 dòng cấu hình TAD và không khởi động model. Lệnh dump có thể tạo/ghi file profile trong `$DSH_HOME`, vì vậy không chạy với home thật.

Input thử được của TAD là flags/command của CLI và profile plugin config. Output đã quan sát là help/version, artifact build và bản dump cấu hình TAD. **Không có output OpenAPI → Kong plan/diff**; không có command phù hợp để thực hiện use case đó một cách kiểm chứng. Không tạo file cấu hình TAD trong PartnerBridge vì chưa có schema Kong tương ứng.

## Kong DB-less hiện tại

Admin API local `http://127.0.0.1:8001` qua GET báo Kong `3.9.3`, `database=off`. GET Service cho `payment-integration-service:8080`, connect/read/write timeout `2000/10000/10000` ms, `retries=0`; GET Route cho `GET`/`POST` tại `/api/v1/payments`, `strip_path=false`, `preserve_host=false`. Chúng khớp `gateways/kong/kong.yml` và đường dẫn trong `contracts/payment/v1/openapi.yaml`. Đây là so sánh thủ công từ nguồn chuẩn và Admin GET, **không phải plan/diff do TAD tạo**.

Source Kong trong container (`kong/db/strategies/off/init.lua`) cho phép select/page và trả `operation_unsupported` cho insert/update/upsert/delete; `kong/api/endpoints.lua` map lỗi đó thành HTTP `405`. Đã kiểm chứng bằng một POST tạo Service tạm tới Admin API: HTTP `405`, body `cannot create 'services' entities when not using a database`; GET lại tên đó trả `404`. Không gửi PATCH/DELETE và không thay đổi Kong. TAD không có connector gọi các thao tác này, cũng không có writer/validator/diff cho `gateways/kong/kong.yml`. Agent tổng quát có thể đọc Admin GET nếu được cấp công cụ và model, nhưng việc này chưa được thử trong lần đánh giá và cũng không chứng minh TAD hỗ trợ ghi cấu hình DB-less.

Không áp dụng thay đổi Kong. Để đạt use case OpenAPI → declarative diff, bước sau cần chọn rõ một trong ba hướng: TAD sinh YAML/diff để review; phát triển connector DB-less; hoặc thử database mode trong một POC tách biệt. Chưa chọn hướng nào. Với kế hoạch nhiều gateway, TAD hiện hữu chỉ là agent tổng quát, chưa có adapter/config contract riêng cho Kong hay gateway khác.

## Kiểm chứng và giới hạn

- `pnpm run build`: pass ở cả lần trước và lần kiểm tra lại. `pnpm run test` lần đầu có 14.664 pass, 2 fail (`hmr-config`, `acp-snapshot`); chạy riêng hai file đó có 68/68 pass. Hai lần full-suite tiếp theo đều có 14.668 pass, 1 fail (`user-patches`: `user patch addition was not applied` sau 10 giây). Chạy riêng `user-patches` vẫn 1 fail, 15 pass. Đây là lỗi test/HMR của TAD cần điều tra; không sửa TAD để làm bộ test pass.
- `pnpm run lint`: fail với 31 vấn đề trong source TAD hiện tại, chủ yếu ở Arkan và `tui/seekarkan/tsconfig.json`. Không chạy `lint:fix` hoặc sửa TAD để che lỗi.
- PartnerBridge: `./mvnw verify` pass (69 test, 0 fail/error) khi chạy với quyền bind localhost; lần chạy trong sandbox bị từ chối socket test, không phải lỗi source. `docker compose config --quiet`, `docker compose ps`, provider smoke và gateway smoke đều pass sau kiểm tra lại. Gateway smoke kiểm tra key-auth, scope hai consumer, replay cùng key không gọi provider thêm, PENDING/422/502/504, outage và rate limit; run ID mới `gateway-59934701c0a7`.
- `git diff --check` của PartnerBridge pass. TAD không có tracked diff sau khi dọn executable bit do install tạo. Artifact build và `node_modules` bị `.gitignore` bỏ qua; không thêm credential hay file tạm vào Git.

Kết quả: **TAD chưa được tích hợp để quản lý Kong DB-less**. Hiện chưa có
command/connector và chưa tạo được plan/diff để review. Payment API và
declarative config của Kong không bị thay đổi trong quá trình đánh giá.
