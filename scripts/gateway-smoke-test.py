#!/usr/bin/env python3
"""Local Kong -> Payment API -> WireMock checks; standard library only."""
import json
import subprocess
import sys
import time
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

PROXY = "http://127.0.0.1:8000"
ADMIN = "http://127.0.0.1:8001"
PROVIDERS = {"PROVIDER_A": "http://127.0.0.1:9101", "PROVIDER_B": "http://127.0.0.1:9102"}
KEYS = {"partner-a": "local-poc-partner-a-key", "partner-b": "local-poc-partner-b-key"}
RUN = "gateway-" + uuid.uuid4().hex[:12]
seen_ids = {}


def http(method, url, body=None, headers=None, timeout=15):
    data = None if body is None else json.dumps(body).encode()
    request = Request(url, data=data, headers=headers or {}, method=method)
    try:
        response = urlopen(request, timeout=timeout)
    except HTTPError as error:
        response = error
    with response:
        return response.status, response.read().decode(), dict(response.headers)


def check(condition, description):
    if not condition:
        raise AssertionError(description)
    print("PASS", description, flush=True)


def body(provider, scenario, suffix):
    return {"providerCode": provider, "merchantReference": f"ORDER-{scenario}-{RUN}-{suffix}",
            "amount": {"value": "100000", "currency": "VND"}, "description": "Gateway local fake payment"}


def create(label, consumer, payload, key, request_id=True, extra_headers=None):
    group = label.split("-replay")[0].split("-conflict")[0]
    rid = f"{RUN}-{label}"
    seen_ids.setdefault(group, set()).add(rid)
    headers = {"Content-Type": "application/json", "Idempotency-Key": key}
    if consumer:
        headers["X-Api-Key"] = KEYS.get(consumer, consumer)
    if request_id:
        headers["Request-ID"] = rid
    if extra_headers:
        headers.update(extra_headers)
    status, raw, response_headers = http("POST", PROXY + "/api/v1/payments", payload, headers)
    print(json.dumps({"label": label, "requestId": rid if request_id else None,
                      "status": status, "response": json.loads(raw)}, ensure_ascii=False), flush=True)
    return status, raw, response_headers, rid


def provider_calls(group):
    counts = {}
    for provider, url in PROVIDERS.items():
        status, raw, _ = http("GET", url + "/__admin/requests")
        assert status == 200
        matched = []
        for event in json.loads(raw)["requests"]:
            request = event["request"]
            ids = [value for name, values in request.get("headers", {}).items()
                   if name.lower() == "request-id"
                   for value in (values if isinstance(values, list) else [values])]
            if any(value in seen_ids.get(group, set()) for value in ids):
                matched.append(request)
        counts[provider] = len(matched)
    return counts


def backend_log_contains(request_id):
    result = subprocess.run(["docker", "compose", "logs", "--no-color", "payment-integration-service"],
                            capture_output=True, text=True, check=True)
    return f"requestId={request_id}" in result.stdout


def backend_health():
    result = subprocess.run(["docker", "compose", "exec", "-T", "payment-integration-service",
                             "curl", "--fail", "--silent", "http://localhost:8080/actuator/health"],
                            capture_output=True, text=True)
    return result.returncode == 0 and '"status":"UP"' in result.stdout


def main():
    print("Gateway smoke run", RUN, flush=True)
    status, raw, _ = http("GET", ADMIN)
    check(status == 200 and json.loads(raw)["configuration"]["database"] == "off", "Kong DB-less Admin health")
    for provider, url in PROVIDERS.items():
        check(http("GET", url + "/health")[0] == 200, provider + " health")
    check(backend_health(), "backend actuator health inside Compose network")
    for path in ("services", "routes", "consumers", "plugins"):
        check(http("GET", ADMIN + "/" + path)[0] == 200, "Kong declarative " + path + " loaded")

    unauth = body("PROVIDER_A", "SUCCESS", "unauth")
    for label, consumer in (("no-key", None), ("bad-key", "invalid-local-key")):
        result = create(label, consumer, unauth, RUN + "-unauth")
        check(result[0] == 401 and provider_calls(label) == {"PROVIDER_A": 0, "PROVIDER_B": 0}
              and not backend_log_contains(result[3]), label + " blocked before backend/provider")
    for location in ("query", "body"):
        rid = f"{RUN}-key-in-{location}"
        url = PROXY + "/api/v1/payments"
        payload = dict(unauth)
        if location == "query":
            url += "?X-Api-Key=" + KEYS["partner-a"]
        else:
            payload["X-Api-Key"] = KEYS["partner-a"]
        status, _, _ = http("POST", url, payload, {"Content-Type": "application/json",
                                                    "Request-ID": rid, "Idempotency-Key": RUN + "-unauth"})
        seen_ids[location] = {rid}
        check(status == 401 and sum(provider_calls(location).values()) == 0 and not backend_log_contains(rid),
              "API key in " + location + " rejected before backend/provider")

    shared = body("PROVIDER_A", "SUCCESS", "shared")
    key = RUN + "-shared-key"
    first = create("shared-a", "partner-a", shared, key)
    check(first[0] == 201 and json.loads(first[1])["status"] == "SUCCEEDED"
          and first[2].get("Request-ID") == first[3] and backend_log_contains(first[3]),
          "consumer A create, canonical body and preserved correlation ID")
    replay = create("shared-a-replay", "partner-a", shared, key)
    check(replay[:2] == first[:2] and provider_calls("shared-a") == {"PROVIDER_A": 1, "PROVIDER_B": 0},
          "consumer A replay exact 201/body, one provider call")
    changed = dict(shared, description="Different fake payload")
    conflict = create("shared-a-conflict", "partner-a", changed, key)
    check(conflict[0] == 409 and json.loads(conflict[1])["code"] == "PAY-IDEMPOTENCY-001"
          and provider_calls("shared-a") == {"PROVIDER_A": 1, "PROVIDER_B": 0},
          "idempotency conflict retained, no extra provider call")

    second = create("shared-b", "partner-b", shared, key,
                    extra_headers={"X-Authenticated-Client-Id": "partner-a", "X-Client-Id": "partner-a",
                                   "X-Consumer-Custom-ID": "partner-a"})
    check(second[0] == 201 and json.loads(second[1])["paymentId"] != json.loads(first[1])["paymentId"]
          and provider_calls("shared-b") == {"PROVIDER_A": 1, "PROVIDER_B": 0},
          "consumer B independent scope; spoofed identity ignored")
    second_replay = create("shared-b-replay", "partner-b", shared, key)
    check(second_replay[:2] == second[:2] and provider_calls("shared-b") == {"PROVIDER_A": 1, "PROVIDER_B": 0},
          "consumer B replay stays in B scope")
    first_id = json.loads(first[1])["paymentId"]
    second_id = json.loads(second[1])["paymentId"]
    for consumer, payment_id, expected in (("partner-a", first_id, 200),
                                           ("partner-b", first_id, 404),
                                           ("partner-b", second_id, 200)):
        status, _, _ = http("GET", PROXY + "/api/v1/payments/" + payment_id,
                            headers={"X-Api-Key": KEYS[consumer], "Request-ID": f"{RUN}-get-{consumer}-{payment_id}"})
        check(status == expected, f"{consumer} GET payment scope returns {expected}")

    generated = create("generated-id", "partner-a", body("PROVIDER_B", "SUCCESS", "generated"),
                       RUN + "-generated", request_id=False)
    generated_id = generated[2].get("Request-ID")
    check(generated[0] == 201 and generated_id and backend_log_contains(generated_id),
          "Kong generates Request-ID; backend log and response agree")

    for provider in PROVIDERS:
        for scenario, expected, result in (("PENDING", 201, "PENDING"),
                                           ("FAIL", 422, "PAY-BUSINESS-003"),
                                           ("MALFORMED", 502, "PAY-PROVIDER-002"),
                                           ("TIMEOUT", 504, "PAY-PROVIDER-004")):
            group = provider + "-" + scenario
            payload = body(provider, scenario, group)
            scenario_key = RUN + "-" + group
            original = create(group, "partner-a", payload, scenario_key)
            check(original[0] == expected and json.loads(original[1]).get("status", json.loads(original[1]).get("code")) == result,
                  group + " canonical HTTP/body")
            again = create(group + "-replay", "partner-a", payload, scenario_key)
            if scenario == "TIMEOUT":
                time.sleep(3)
            counts = provider_calls(group)
            check(again[:2] == original[:2] and counts[provider] == 1 and sum(counts.values()) == 1,
                  group + " exact replay and one provider call")

    # Upstream failure is Kong-owned, not a canonical backend ApiError.
    unavailable = body("PROVIDER_A", "SUCCESS", "backend-unavailable")
    stopped = False
    try:
        subprocess.run(["docker", "compose", "stop", "payment-integration-service"], check=True)
        stopped = True
        failure = create("backend-unavailable", "partner-a", unavailable, RUN + "-backend-unavailable")
        gateway_error = json.loads(failure[1])
        check(failure[0] in (502, 503, 504) and "code" not in gateway_error
              and provider_calls("backend-unavailable") == {"PROVIDER_A": 0, "PROVIDER_B": 0},
              "backend unavailable yields Kong-owned upstream error before provider")
    finally:
        if stopped:
            subprocess.run(["docker", "compose", "start", "payment-integration-service"], check=True)
            for _ in range(30):
                if backend_health():
                    break
                time.sleep(1)
            check(backend_health(), "backend restored after upstream-failure test")

    # Last: exhaust B's local consumer quota using GET, then prove a create is blocked.
    limited = False
    for i in range(80):
        headers = {"X-Api-Key": KEYS["partner-b"], "Request-ID": f"{RUN}-rate-get-{i}"}
        status, _, _ = http("GET", PROXY + "/api/v1/payments/00000000-0000-0000-0000-000000000000", headers=headers)
        if status == 429:
            limited = True
            break
    check(limited, "per-consumer local rate limit reached")
    limited_create = create("rate-blocked", "partner-b", body("PROVIDER_A", "SUCCESS", "rate"), RUN + "-rate")
    check(limited_create[0] == 429 and provider_calls("rate-blocked") == {"PROVIDER_A": 0, "PROVIDER_B": 0},
          "rate-limited create blocked before provider")
    other_status, _, _ = http("GET", PROXY + "/api/v1/payments/00000000-0000-0000-0000-000000000000",
                              headers={"X-Api-Key": KEYS["partner-a"], "Request-ID": RUN + "-rate-other-consumer"})
    check(other_status == 404, "consumer A quota remains independent of rate-limited B")
    print("PASS gateway smoke suite", RUN, flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print("FAIL gateway smoke suite:", error, file=sys.stderr, flush=True)
        raise
