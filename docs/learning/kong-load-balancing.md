# Kong Load Balancing

| Chủ đề                                                                              |                                                             |
|-------------------------------------------------------------------------------------|-------------------------------------------------------------|
| [Route, Service, Upstream và Target](kong-load-balancing/00-topology.md)            | Mỗi entity đóng vai trò gì trong đường đi request?          |
| [Round-robin và weight](kong-load-balancing/01-round-robin.md)                      | Kong chia traffic cho nhiều backend như thế nào?            |
| [Health check và failover](kong-load-balancing/02-health-failover.md)               | Backend chết được phát hiện và loại khỏi pool ra sao?       |
| [Timeout, retry và dữ liệu phân tán](kong-load-balancing/03-timeout-retry-state.md) | Vì sao failover được traffic chưa có nghĩa payment an toàn? |

```text
Hiểu “ai là ai”
    ↓
Chia việc cho nhiều backend
    ↓
Loại backend bị lỗi
    ↓
Bảo vệ dữ liệu và timeout budget
```

## Khởi động hạ tầng dùng chung

```bash
cd /Users/manhnguyen/Works/TMO/projects/partner-bridge
docker compose -f compose.yaml -f compose.kong-load-balancing.yaml config --quiet
docker compose -f compose.yaml -f compose.kong-load-balancing.yaml up -d --build \
  provider-a provider-b payment-lab-a payment-lab-b kong-load-balancing-lab
```

Kiểm tra:

```bash
docker compose -f compose.yaml -f compose.kong-load-balancing.yaml ps
```

Thấy `payment-lab-a`, `payment-lab-b` và `kong-load-balancing-lab` đều
`healthy`.

| Thành phần                     | URL                     |
|--------------------------------|-------------------------|
| Kong proxy của lab             | `http://localhost:8100` |
| Kong Admin API của lab         | `http://127.0.0.1:8101` |
| Gateway chính, không thuộc lab | `http://localhost:8000` |

## Bản đồ file

| File                                         | Vai trò                                  |
|----------------------------------------------|------------------------------------------|
| `compose.yaml`                               | Network và hai provider stubs dùng chung |
| `compose.kong-load-balancing.yaml`           | Hai backend và Kong riêng của lab        |
| `gateways/kong/labs/load-balancing/kong.yml` | Upstream, Target, health check và plugin |

Bắt đầu từ [Route, Service, Upstream và Target](kong-load-balancing/00-topology.md).

## Dừng

```bash
docker compose -f compose.yaml -f compose.kong-load-balancing.yaml stop \
  kong-load-balancing-lab payment-lab-a payment-lab-b
```

Không dùng `down` nếu muốn giữ gateway chính và provider stubs đang chạy.
