# PartnerBridge

- [Canonical Payment API v1](contracts/payment/v1/README.md)
- [Local payment provider stubs](provider-stubs/README.md)
- [Kong Gateway DB-less POC](gateways/kong/README.md)

## Architecture Documents

- [PartnerBridge architecture](docs/architecture/README.md)

Step 6 flow: Client → Kong (`localhost:8000`) → payment-integration-service →
PROVIDER_A / PROVIDER_B. Kong authenticates, sets trusted consumer identity,
correlates requests and rate-limits; backend still owns canonical validation,
idempotency, provider routing/mapping and payment errors.

```bash
docker compose up -d --build
docker compose ps
provider-stubs/smoke-test.sh
scripts/gateway-smoke-test.sh
docker compose down
```

Only Kong's proxy (`8000`) and local-only Admin API (`127.0.0.1:8001`) are
published for the payment path. Stub ports `9101`/`9102` remain published for
their existing smoke tests. The backend has no host port in the default Compose
stack. See the gateway README for local dummy keys, curl examples, reload and
POC limitations. This is not a production deployment.
