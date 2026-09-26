# PartnerBridge Banking API v1

`openapi.yaml` is the canonical contract for the two Banking API operations:

- `GET /api/v1/accounts/{accountId}`
- `POST /api/v1/transfers`

To publish it to Anypoint Exchange, create a **REST API - OAS** asset, select
`openapi.yaml` as the main file, use API version `v1`, and start with asset
version `1.0.0`.

The Spring Boot implementation is under `services/banking-integration-service`.
The simulated external bank contract and responses are local POC details and are
not part of this public canonical API.
