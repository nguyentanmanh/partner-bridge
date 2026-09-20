# Kong Gateway POC (Step 6)

Client → Kong → Payment Service → PROVIDER_A / PROVIDER_B.

`kong.yml` is the DB-less source of truth. Compose pins `kong:3.9.3`, sets
`KONG_DATABASE=off` and mounts the file read-only. No Kong database or custom
plugin is used. Start from the repository root with `docker compose up -d --build`;
inspect with `docker compose ps`; stop with `docker compose down`.

| Port | Purpose |
| --- | --- |
| `8000` | Kong HTTP proxy, the only published Payment API entry point |
| `127.0.0.1:8001` | Kong Admin API, local host only |
| `9101`, `9102` | Existing local WireMock stub/debug ports |

The backend is reachable only on the Compose network. Its gateway profile uses
`http://provider-a:8080` and `http://provider-b:8080`, never container-local
`localhost`. It requires `X-Authenticated-Client-Id`; missing/invalid identity
is rejected with `400` rather than falling back to `local-poc-client`. The
default ID is for local/direct development outside the gateway profile only.
The Spring profile configuration lives in the service at
`services/payment-integration-service/src/main/resources/application-gateway.yml`;
only Kong configuration and Kong-specific documentation live in this directory.

## Kong entities

- Service `payment-integration-service` points to backend port 8080. Connect,
  read and write timeouts are 2/10/10 seconds; retries are disabled to avoid
  duplicate create attempts. Read timeout exceeds the backend provider timeout
  (3 seconds), allowing backend `502`/`504` to pass through.
- Route `canonical-payment-v1` accepts GET/POST at `/api/v1/payments` (including
  payment ID paths). `strip_path: false` retains the canonical path;
  `preserve_host: false` sends the backend's Compose host. Kong does not rewrite
  payment JSON, status or error bodies; `Idempotency-Key` passes unchanged.
- Consumers `partner-a` and `partner-b` have stable matching `custom_id` values
  and distinct dummy local credentials. They are not production secrets.
- `key-auth` accepts only `X-Api-Key`, not query/body, and hides the credential
  from the upstream. Missing/invalid keys are rejected at Kong with `401`.
- `request-transformer` removes client-supplied `X-Authenticated-Client-Id`
  and `X-Client-Id`, then sets the former from authenticated
  `X-Consumer-Custom-ID`. It runs after key-auth by Kong plugin priority. The
  backend reads only the vendor-neutral internal header for create scope.
  GET also uses that scope; a different consumer receives `404` for another
  consumer's payment ID.
- `correlation-id` uses the canonical `Request-ID`, generating it when absent,
  honoring a caller-provided value and echoing it downstream. The backend uses
  that same header for logs and responses; no competing `X-Request-ID` is added.
- `rate-limiting` allows 60 requests/minute per authenticated consumer with
  `policy: local`. This counter is accurate only on one Kong node; multiple
  nodes need a shared strategy such as Redis.

Local POC API keys: `partner-a` → `local-poc-partner-a-key`; `partner-b` →
`local-poc-partner-b-key`. Do not use real credentials here.

```bash
curl -i http://localhost:8000/api/v1/payments \
  -H 'X-Api-Key: local-poc-partner-a-key' \
  -H 'Request-ID: local-gateway-req-1' \
  -H 'Idempotency-Key: local-gateway-idem-1' \
  -H 'Content-Type: application/json' \
  -d '{"providerCode":"PROVIDER_A","merchantReference":"ORDER-SUCCESS-GATEWAY-001","amount":{"value":"100000","currency":"VND"},"description":"Local fake payment"}'
```

Run `provider-stubs/smoke-test.sh` and `scripts/gateway-smoke-test.sh` after
starting the stack. The gateway script uses fresh fake IDs each run, reads
WireMock journals per Request-ID, checks consumer isolation/spoofing, replay,
error pass-through, rate limiting and backend outage. It temporarily stops and
restores the backend to test upstream failure; this resets backend in-memory
payments. It leaves the rest of the Compose stack running.

## Error ownership and operations

Kong owns `401` authentication, `429` rate limit and upstream errors when the
backend is unavailable (`502`/`503`, or `504` if connecting times out, as observed
on this local Docker network). These are Kong-generated and are not forced into the
canonical `ApiError`. Backend owns `400` validation, `409` conflict, `422`
business rejection, provider-malformed `502`, provider-timeout `504` and
canonical payment responses. Kong preserves backend status/body.

After changing `kong.yml`, run `docker compose restart kong` to load the mounted
declarative config again; check `curl http://127.0.0.1:8001/services` and the
gateway smoke test. DB-less Admin API is read-only for entity CRUD. This POC
does not provide shared rate counters, distributed idempotency, durable payment
storage, backend identity transport authentication, or production secret/TLS
management. Restarting backend loses in-memory idempotency and payment data.

The canonical OpenAPI contract and backend domain/application layers do not
depend on Kong. To change gateways later, reproduce edge routing/authenticated
identity/correlation/rate policies and preserve the same canonical path,
headers, status and body; the provider adapters and payment use case remain.
