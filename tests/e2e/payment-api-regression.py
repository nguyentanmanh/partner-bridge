#!/usr/bin/env python3
"""Payment API -> local WireMock regression checks. Python standard library only."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import copy
import json
import threading
import time
from urllib.error import HTTPError
from urllib.request import Request, urlopen
import uuid


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--provider-a-url", default="http://localhost:9101")
    parser.add_argument("--provider-b-url", default="http://localhost:9102")
    args = parser.parse_args()
    run = "fixed-" + uuid.uuid4().hex[:10]
    provider_urls = {"PROVIDER_A": args.provider_a_url, "PROVIDER_B": args.provider_b_url}
    groups = {}
    output_lock = threading.Lock()

    def emit(event):
        with output_lock:
            print(json.dumps(event, ensure_ascii=False), flush=True)

    def http(method, url, body=None, headers=None):
        data = None if body is None else json.dumps(body).encode()
        request = Request(url, data=data, headers=headers or {}, method=method)
        try:
            response = urlopen(request, timeout=15)
        except HTTPError as error:
            response = error
        with response:
            raw = response.read().decode()
            return response.status, raw, dict(response.headers)

    def count_calls(group):
        counts = {}
        for provider, url in provider_urls.items():
            status, raw, _ = http("GET", url + "/__admin/requests")
            assert status == 200
            matches = []
            for event in json.loads(raw)["requests"]:
                request = event["request"]
                request_ids = []
                for name, values in request.get("headers", {}).items():
                    if name.lower() == "request-id":
                        request_ids.extend(values if isinstance(values, list) else [values])
                if any(value in groups[group] for value in request_ids):
                    matches.append({"requestId": request_ids, "method": request["method"],
                                    "url": request["url"], "body": request.get("body"),
                                    "matched": event.get("wasMatched")})
            counts[provider] = {"count": len(matches), "requests": matches}
        return counts

    def request_body(provider, scenario, group):
        return {"providerCode": provider, "merchantReference": f"ORDER-{scenario}-{run}-{group}",
                "amount": {"value": "100000", "currency": "VND"}, "description": "Local regression payment"}

    def create(group, label, body, key=None, language="en", barrier=None):
        request_id = f"{run}-{label}"
        with output_lock:
            groups.setdefault(group, set()).add(request_id)
        headers = {"Content-Type": "application/json", "Request-ID": request_id,
                   "Idempotency-Key": key or f"{run}-{group}", "Accept-Language": language}
        if barrier is not None:
            barrier.wait(timeout=5)
        started = time.monotonic()
        status, raw, response_headers = http("POST", args.base_url + "/api/v1/payments", body, headers)
        elapsed = round(time.monotonic() - started, 3)
        assert response_headers["Request-ID"] == request_id
        emit({"label": label, "group": group, "method": "POST", "headers": headers, "request": body,
              "httpStatus": status, "response": json.loads(raw), "rawResponse": raw, "elapsedSeconds": elapsed})
        return status, raw

    emit({"run": run, "baseUrl": args.base_url, "note": "Only local fake data; no journal reset or mapping changes"})
    for url in [args.base_url + "/actuator/health", args.provider_a_url + "/health", args.provider_b_url + "/health"]:
        assert http("GET", url)[0] == 200, url

    existing_payment_id = None
    for provider, short in [("PROVIDER_A", "A"), ("PROVIDER_B", "B")]:
        for scenario, expected, expected_code in [
            ("SUCCESS", 201, "SUCCEEDED"), ("PENDING", 201, "PENDING"),
            ("FAIL", 422, "PAY-BUSINESS-003"), ("MALFORMED", 502, "PAY-PROVIDER-002"),
            ("TIMEOUT", 504, "PAY-PROVIDER-004")
        ]:
            group = f"{short}-{scenario}"
            body = request_body(provider, scenario, group)
            barrier = threading.Barrier(4)
            with ThreadPoolExecutor(max_workers=4) as executor:
                futures = [executor.submit(create, group, f"{group}-{i}", body, barrier=barrier) for i in range(4)]
                responses = [future.result() for future in futures]
            assert len(set(responses)) == 1, (group, "Concurrent HTTP status/raw body differ")
            status, raw = responses[0]
            assert status == expected, (group, status, raw)
            parsed = json.loads(raw)
            assert parsed.get("status", parsed.get("code")) == expected_code, (group, parsed)
            if scenario == "SUCCESS":
                existing_payment_id = parsed["paymentId"]
            assert create(group, group + "-replay", body, language="vi") == responses[0]
            changed = copy.deepcopy(body)
            changed["description"] = "Different local payload"
            conflict_status, conflict_body = create(group, group + "-payload-conflict", changed)
            assert conflict_status == 409 and json.loads(conflict_body)["code"] == "PAY-IDEMPOTENCY-001"
            conflict_status, conflict_body = create(group, group + "-reference-conflict", body, key=f"{run}-{group}-other")
            assert conflict_status == 409 and json.loads(conflict_body)["code"] == "PAY-PAYMENT-002"
            if scenario == "TIMEOUT":
                # WireMock responds after 5.5s; allow its serve event to finish after the API's 3s timeout.
                time.sleep(3)
            counts = count_calls(group)
            assert counts[provider]["count"] == 1, (group, counts)
            assert sum(value["count"] for value in counts.values()) == 1, (group, counts)
            emit({"check": group, "passed": True, "concurrentRequests": 4,
                  "exactSequentialReplay": True, "providerCalls": counts})

        for variant in ["number", "callback", "money-extra"]:
            group = f"{short}-invalid-{variant}"
            body = request_body(provider, "SUCCESS", group)
            if variant == "number":
                body["amount"]["value"] = 100000
            elif variant == "callback":
                body["callbackUrl"] = "http://localhost/unused-local-test"
            else:
                body["amount"]["extra"] = "undeclared"
            status, raw = create(group, group, body)
            assert status == 400 and json.loads(raw)["code"] == "PAY-VALIDATION-001"
            counts = count_calls(group)
            assert sum(value["count"] for value in counts.values()) == 0
            emit({"check": group, "passed": True, "providerCalls": counts})

    slow_group = "independent-slow"
    fast_group = "independent-fast"
    with ThreadPoolExecutor(max_workers=2) as executor:
        slow = executor.submit(create, slow_group, slow_group, request_body("PROVIDER_A", "TIMEOUT", slow_group))
        time.sleep(0.25)
        started = time.monotonic()
        get_status, get_body, _ = http("GET", args.base_url + "/api/v1/payments/" + existing_payment_id,
                                     headers={"Request-ID": run + "-independent-get"})
        fast_response = create(fast_group, fast_group, request_body("PROVIDER_B", "SUCCESS", fast_group))
        elapsed = time.monotonic() - started
        assert get_status == 200 and fast_response[0] == 201
        assert not slow.done() and elapsed < 2, ("Independent request/GET waited behind slow provider", elapsed)
        assert slow.result()[0] == 504
    time.sleep(3)
    slow_counts, fast_counts = count_calls(slow_group), count_calls(fast_group)
    assert slow_counts["PROVIDER_A"]["count"] == 1 and slow_counts["PROVIDER_B"]["count"] == 0
    assert fast_counts["PROVIDER_B"]["count"] == 1 and fast_counts["PROVIDER_A"]["count"] == 0
    emit({"check": "independent-create-and-get", "passed": True, "elapsedSeconds": round(elapsed, 3),
          "getStatus": get_status, "getResponse": json.loads(get_body),
          "slowProviderCalls": slow_counts, "fastProviderCalls": fast_counts})
    emit({"result": "PASS", "run": run, "checks": 17})


if __name__ == "__main__":
    main()
