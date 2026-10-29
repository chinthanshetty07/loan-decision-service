#!/usr/bin/env bash
# Walks through the main flows against a running service (docker compose up).
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
RUN="$(date +%s)"

json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
token() {
  curl -fsS -X POST "$BASE/api/v1/auth/token" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" | json 'd["accessToken"]'
}
submit() { # name pan income amount tenure [idempotency-key]
  curl -fsS -X POST "$BASE/api/v1/applications" -H "Authorization: Bearer $OFFICER" \
    -H 'Content-Type: application/json' ${6:+-H "Idempotency-Key: $6"} \
    -d "{\"productType\":\"PERSONAL\",\"applicantName\":\"$1\",\"pan\":\"$2\",\"monthlyIncome\":$3,\"existingEmi\":0,\"requestedAmount\":$4,\"tenureMonths\":$5}"
}
show() { json '"%-9s score=%-4s emi=%-9s foir=%-6s %s" % (d["status"], d["creditScore"], d["proposedEmi"], d["foir"], "; ".join(d["reasons"]))'; }

OFFICER="$(token officer officer123)"
RISK="$(token risk risk123)"

echo "== 1. Strong applicant (score 780)"
submit 'Asha Rao' ABCDE0480F 90000 300000 24 | show

echo "== 2. Weak applicant (score 640, low income, long tenure)"
submit 'Vikram Das' FGHIJ0340K 22000 900000 72 | show

echo "== 3. Borderline score (715) goes to an underwriter"
REFERRED="$(submit 'Neha Pillai' KLMNO0415P 80000 300000 24)"
echo "$REFERRED" | show

echo "== 4. Name on sanctions watchlist is never auto-decided"
submit 'Ravi Shellwala' UVWXY0500Z 150000 300000 24 | show

echo "== 5. Network retry with the same Idempotency-Key returns the original decision"
submit 'Meera Iyer' LMNOP0450Q 80000 200000 12 "demo-$RUN" | json '"   first  id=" + d["id"]'
submit 'Meera Iyer' LMNOP0450Q 80000 200000 12 "demo-$RUN" | json '"   replay id=" + d["id"]'

echo "== 6. Risk admin lowers PERSONAL min score to 680 (new policy version, no redeploy)"
curl -fsS -X PUT "$BASE/api/v1/policies/PERSONAL" -H "Authorization: Bearer $RISK" -H 'Content-Type: application/json' \
  -d '{"minCreditScore":680,"referBand":20,"maxFoir":0.50,"minMonthlyIncome":25000,"maxLoanAmount":1500000,"maxTenureMonths":60,"annualInterestRate":12.5}' \
  | json '"   PERSONAL policy now v%d, minCreditScore=%d" % (d["version"], d["rules"]["minCreditScore"])'
echo "   Same borderline applicant under the new policy:"
submit 'Neha Pillai' KLMNO0415P 80000 300000 24 | show

echo "== 7. Underwriter approves the application referred in step 3"
REF_ID="$(echo "$REFERRED" | json 'd["id"]')"
curl -fsS -X POST "$BASE/api/v1/applications/$REF_ID/review" -H "Authorization: Bearer $RISK" \
  -H 'Content-Type: application/json' -d '{"outcome":"APPROVED","note":"Salary slips verified"}' | show

echo "== 8. Audit trail for that application"
curl -fsS "$BASE/api/v1/applications/$REF_ID/audit" -H "Authorization: Bearer $OFFICER" \
  | json '"\n".join("   %-24s %-8s %s" % (e["eventType"], e["actor"], e["detail"]) for e in d)'

echo "== 9. Credit bureau outage (PAN digits 9999): 3 attempts each, then referred instead of failing"
submit 'Kiran Shah' PQRST9999K 90000 300000 24 | show
submit 'Kiran Shah' PQRST9999K 90000 300000 24 | show
curl -fsS "$BASE/actuator/circuitbreakers" \
  | json '"   circuit breaker: %s (failure rate %s percent)" % (d["circuitBreakers"]["creditBureau"]["state"], d["circuitBreakers"]["creditBureau"]["failureRate"])'
echo "   While open, healthy applicants are referred at once instead of waiting on a dead bureau:"
submit 'Asha Rao' ABCDE0480F 90000 300000 24 | show
