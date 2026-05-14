#!/usr/bin/env bash
# AssessX full smoke-test
# Usage: TOKEN=<jwt> bash scripts/smoke-test.sh
# Optional: BASE_URL=http://localhost:8080

set -uo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:?Usage: TOKEN=<jwt> bash scripts/smoke-test.sh}"
TS=$(date +%s)

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

PASSED=0; FAILED=0
BODY=""; STATUS=""

h() { echo -e "\n${CYAN}${BOLD}== $* ==${RESET}"; }

check() {
  local label="$1" expected="$2" actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo -e "  ${GREEN}+${RESET} $label"
    ((PASSED++)) || true
  else
    echo -e "  ${RED}-${RESET} $label  (expected ${YELLOW}${expected}${RESET}, got ${YELLOW}${actual}${RESET})"
    ((FAILED++)) || true
  fi
}

jf() {
  local key="${1#.}"
  local val
  val=$(echo "$BODY" | sed -n "s/.*\"${key}\":\"\\([^\"]*\\)\".*/\\1/p" | head -1)
  if [[ -z "$val" ]]; then
    val=$(echo "$BODY" | sed -n "s/.*\"${key}\":\\([^,}\\[\"]*\\).*/\\1/p" | head -1 | tr -d ' \r')
  fi
  echo "$val"
}

call() {
  local method="$1" path="$2"; shift 2
  local delim="---HTTPSTATUS---"
  local raw
  raw=$(curl -s -w "${delim}%{http_code}" \
    -X "$method" "${BASE}${path}" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    "$@" 2>/dev/null)
  STATUS="${raw##*${delim}}"
  BODY="${raw%${delim}*}"
}

call_noauth() {
  local method="$1" path="$2"; shift 2
  local delim="---HTTPSTATUS---"
  local raw
  raw=$(curl -s -w "${delim}%{http_code}" \
    -X "$method" "${BASE}${path}" \
    -H "Content-Type: application/json" \
    "$@" 2>/dev/null)
  STATUS="${raw##*${delim}}"
  BODY="${raw%${delim}*}"
}

jf_last() {
  local key="${1#.}"
  local val
  val=$(echo "$BODY" | sed -n "s/.*\"${key}\":\"\\([^\"]*\\)\".*/\\1/p" | tail -1)
  if [[ -z "$val" ]]; then
    val=$(echo "$BODY" | sed -n "s/.*\"${key}\":\\([^,}\\[\"]*\\).*/\\1/p" | tail -1 | tr -d ' \r')
  fi
  echo "$val"
}



h "0. Health check"

call GET /auth/me
check "Server is reachable (GET /auth/me => 200)" "200" "$STATUS"

MY_ID=$(jf '.id')
MY_ROLE=$(jf '.role')
echo -e "  ${BOLD}Logged in as: id=${MY_ID}, role=${MY_ROLE}${RESET}"

if [[ -z "$MY_ID" || "$MY_ID" == "null" ]]; then
  echo -e "  ${RED}Cannot parse user id from response. Raw body:${RESET}"
  echo "  $BODY"
  exit 1
fi



h "1. Groups"

call GET /api/groups
check "GET /api/groups => 200" "200" "$STATUS"

call POST /api/groups -d "{\"name\":\"TEST-GROUP-SMOKE-${TS}\"}"
check "POST /api/groups => 201" "201" "$STATUS"
GROUP_ID=$(jf '.id')
echo -e "  Created group id=${GROUP_ID}"

call POST /api/groups -d "{\"name\":\"TEST-GROUP-SMOKE-${TS}\"}"
check "POST /api/groups duplicate => 409" "409" "$STATUS"

call POST /api/groups -d '{"name":""}'
check "POST /api/groups empty name => 400" "400" "$STATUS"

call GET /api/groups/${GROUP_ID}/students
check "GET /api/groups/{id}/students => 200" "200" "$STATUS"



h "2. Tests (multiple-choice)"

TEST_BODY='{"title":"Smoke Test Quiz","questions":"[{\"id\":\"q1\",\"text\":\"2+2?\",\"options\":[\"3\",\"4\",\"5\"]}]","answers":"{\"q1\":\"4\"}","points":10,"timeLimitSec":300}'

call POST /api/tests -d "$TEST_BODY"
check "POST /api/tests => 201" "201" "$STATUS"
TEST_ID=$(jf '.id')
echo -e "  Created test id=${TEST_ID}"

call GET /api/tests
check "GET /api/tests => 200" "200" "$STATUS"

call GET /api/tests/${TEST_ID}
check "GET /api/tests/{id} => 200" "200" "$STATUS"

call GET /api/tests/99999
check "GET /api/tests/99999 => 404" "404" "$STATUS"

call PUT /api/tests/${TEST_ID} -d "$TEST_BODY"
check "PUT /api/tests/{id} => 200" "200" "$STATUS"

call POST /api/tests/${TEST_ID}/submit -d '{"answers":{"q1":"4"}}'
check "POST /api/tests/{id}/submit correct => 200" "200" "$STATUS"
check "  correct answer earns full points (earnedPoints=10)" "10" "$(jf '.earnedPoints')"

call POST /api/tests/${TEST_ID}/submit -d '{"answers":{"q1":"3"}}'
check "POST /api/tests/{id}/submit wrong => 200" "200" "$STATUS"
check "  wrong answer earns 0 points" "0" "$(jf '.earnedPoints')"



h "3. Assignments"

call POST /api/assignments -d "{\"groupId\":${GROUP_ID},\"testId\":${TEST_ID}}"
check "POST /api/assignments (test) => 201" "201" "$STATUS"
ASSIGN_ID=$(jf '.id')
echo -e "  Created assignment id=${ASSIGN_ID}"

call GET /api/assignments
check "GET /api/assignments => 200" "200" "$STATUS"

call GET /api/assignments/my
check "GET /api/assignments/my => 200" "200" "$STATUS"

call POST /api/tests/${TEST_ID}/submit \
  -d "{\"assignmentId\":${ASSIGN_ID},\"answers\":{\"q1\":\"4\"}}"
check "POST /api/tests/{id}/submit with assignmentId => 200" "200" "$STATUS"



h "4. Results"

call GET /api/results/my
check "GET /api/results/my => 200" "200" "$STATUS"

call GET /api/results/group/${GROUP_ID}
check "GET /api/results/group/{id} => 200" "200" "$STATUS"



h "5. Code Practices — CRUD"

PRACTICE_BODY='{"title":"Smoke Sum","description":"Implement sum(a,b)","points":20,"timeLimitSec":30,"unitTests":["Solution s = new Solution();\nassert s.sum(2,3)==5;","Solution s = new Solution();\nassert s.sum(1,1)==2;"]}'

call POST /api/practices -d "$PRACTICE_BODY"
check "POST /api/practices => 201" "201" "$STATUS"
PRACTICE_ID=$(jf '.id')
echo -e "  Created practice id=${PRACTICE_ID}"

call GET /api/practices
check "GET /api/practices => 200" "200" "$STATUS"

call GET /api/practices/${PRACTICE_ID}
check "GET /api/practices/{id} => 200" "200" "$STATUS"
check "  unitTestCount == 2" "2" "$(jf '.unitTestCount')"

call GET /api/practices/99999
check "GET /api/practices/99999 => 404" "404" "$STATUS"

call PUT /api/practices/${PRACTICE_ID} -d "$PRACTICE_BODY"
check "PUT /api/practices/{id} => 200" "200" "$STATUS"

call POST /api/practices -d '{"description":"x","points":10,"timeLimitSec":30}'
check "POST /api/practices missing title => 400" "400" "$STATUS"



h "6. Code Practices — Submit (Docker sandbox)"

CORRECT_CODE='public class Solution { public int sum(int a, int b) { return a + b; } }'
WRONG_CODE='public class Solution { public int sum(int a, int b) { return 0; } }'

call POST /api/practices/${PRACTICE_ID}/submit \
  -d "{\"code\":\"${CORRECT_CODE}\"}"
check "POST /api/practices/{id}/submit correct => 200" "200" "$STATUS"
PT=$(jf '.passedTests'); TT=$(jf '.totalTests')
check "  correct solution: all tests pass (${PT}/${TT})" "$TT" "$PT"

call POST /api/practices/${PRACTICE_ID}/submit \
  -d "{\"code\":\"${WRONG_CODE}\"}"
check "POST /api/practices/{id}/submit wrong => 200" "200" "$STATUS"
check "  wrong solution: passedTests == 0" "0" "$(jf '.passedTests')"

call POST /api/practices/${PRACTICE_ID}/submit -d '{"code":""}'
check "POST /api/practices/{id}/submit empty code => 400" "400" "$STATUS"



h "7. Submit з Assignment (збереження Result)"

call POST /api/assignments -d "{\"groupId\":${GROUP_ID},\"practiceId\":${PRACTICE_ID}}"
check "POST /api/assignments (practice) => 201" "201" "$STATUS"
PRACTICE_ASSIGN_ID=$(jf '.id')
echo -e "  Created practice assignment id=${PRACTICE_ASSIGN_ID}"

call POST /api/practices/${PRACTICE_ID}/submit \
  -d "{\"assignmentId\":${PRACTICE_ASSIGN_ID},\"code\":\"${CORRECT_CODE}\"}"
check "POST /api/practices/{id}/submit with assignmentId => 200" "200" "$STATUS"

call GET /api/results/my
check "GET /api/results/my => 200" "200" "$STATUS"



h "8. AI Hint (крок 8)"

echo -e "  ${YELLOW}(Запит до Ollama може зайняти до 60 сек...)${RESET}"

call POST /api/practices/${PRACTICE_ID}/hint \
  -d "{\"assignmentId\":${PRACTICE_ASSIGN_ID},\"currentCode\":\"${WRONG_CODE}\"}" \
  --max-time 70
check "POST /api/practices/{id}/hint (перша) => 200" "200" "$STATUS"

HINT_TEXT=$(jf '.hint')
HINT_AT=$(jf '.hintUsedAt')

if [[ -n "$HINT_TEXT" && "$HINT_TEXT" != "null" ]]; then
  echo -e "  ${GREEN}✓${RESET} hint text is non-empty"
  echo -e "  ${BOLD}Hint preview:${RESET} ${HINT_TEXT:0:120}"
  ((PASSED++)) || true
else
  echo -e "  ${RED}✗${RESET} hint text is empty or null"
  echo -e "  Raw body: $BODY"
  ((FAILED++)) || true
fi

if [[ -n "$HINT_AT" && "$HINT_AT" != "null" ]]; then
  echo -e "  ${GREEN}✓${RESET} hintUsedAt is set: ${HINT_AT}"
  ((PASSED++)) || true
else
  echo -e "  ${RED}✗${RESET} hintUsedAt is null"
  ((FAILED++)) || true
fi

call POST /api/practices/${PRACTICE_ID}/hint \
  -d "{\"assignmentId\":${PRACTICE_ASSIGN_ID}}"
check "POST /api/practices/{id}/hint повторно => 409" "409" "$STATUS"

call POST /api/practices/${PRACTICE_ID}/submit \
  -d "{\"assignmentId\":${PRACTICE_ASSIGN_ID},\"code\":\"${CORRECT_CODE}\"}"
check "POST /api/practices/{id}/submit після hint => 200" "200" "$STATUS"

call GET /api/results/my
LAST_MAX=$(jf_last '.maxPoints')
check "  maxPoints після hint = 10 (50% від 20)" "10" "$LAST_MAX"

call POST /api/practices/${PRACTICE_ID}/hint \
  -d '{"currentCode":"public class Solution {}"}'
check "POST /api/practices/{id}/hint без assignmentId => 400" "400" "$STATUS"

call POST /api/practices/99999/hint \
  -d '{"assignmentId":99999}'
check "POST /api/practices/99999/hint => 404" "404" "$STATUS"



h "9. Bulk CSV Import"

CSV_FILE="./smoke-import-${TS}.csv"
EMPTY_FILE="./smoke-import-empty-${TS}.csv"

printf 'task_name,task_description,max_score,test_class_name,test_method_name,test_code\r\nSmokeSumImport,Implement sum(a+b),15,SolutionTest,testSum,"Solution s = new Solution();assert s.sum(1,2)==3;"\r\nSmokeSumImport,Implement sum(a+b),15,SolutionTest,testSum2,"Solution s = new Solution();assert s.sum(0,0)==0;"\r\n' > "$CSV_FILE"

IMPORT_RAW=$(curl -s -w "---HTTPSTATUS---%{http_code}" \
  -X POST "${BASE}/api/practices/import" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@${CSV_FILE};type=text/csv" 2>/dev/null)
STATUS="${IMPORT_RAW##*---HTTPSTATUS---}"
BODY="${IMPORT_RAW%---HTTPSTATUS---*}"

check "POST /api/practices/import valid CSV => 201" "201" "$STATUS"
check "  createdPractices == 1" "1" "$(jf '.createdPractices')"
check "  addedTests == 2" "2" "$(jf '.addedTests')"

touch "$EMPTY_FILE"
EMPTY_RAW=$(curl -s -w "---HTTPSTATUS---%{http_code}" \
  -X POST "${BASE}/api/practices/import" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@${EMPTY_FILE};type=text/csv" 2>/dev/null)
STATUS="${EMPTY_RAW##*---HTTPSTATUS---}"
BODY="${EMPTY_RAW%---HTTPSTATUS---*}"
check "POST /api/practices/import empty file => 400" "400" "$STATUS"

printf 'task_name,task_description,max_score,test_class_name,test_method_name,test_code\r\nSmokeSumImport,Implement sum(a+b),15,SolutionTest,testSum3,"Solution s = new Solution();assert s.sum(5,5)==10;"\r\n' > "$CSV_FILE"

IMPORT_RAW=$(curl -s -w "---HTTPSTATUS---%{http_code}" \
  -X POST "${BASE}/api/practices/import" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@${CSV_FILE};type=text/csv" 2>/dev/null)
STATUS="${IMPORT_RAW##*---HTTPSTATUS---}"
BODY="${IMPORT_RAW%---HTTPSTATUS---*}"

check "POST /api/practices/import upsert existing => 201" "201" "$STATUS"
check "  createdPractices == 0 (upsert)" "0" "$(jf '.createdPractices')"
check "  addedTests == 1 (upsert)" "1" "$(jf '.addedTests')"

rm -f "$CSV_FILE" "$EMPTY_FILE"

call GET /api/practices
SMOKE_IMPORT_ID=$(echo "$BODY" | sed 's/},{/\n{/g' | grep 'SmokeSumImport' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo -e "  Imported practice id=${SMOKE_IMPORT_ID}"



h "10. Bulk CSV Import (Tests)"

TEST_CSV_FILE="./smoke-test-import-${TS}.csv"
TEST_EMPTY_FILE="./smoke-test-import-empty-${TS}.csv"

printf 'test_title,question_text,option_a,option_b,option_c,option_d,correct_option,points,time_limit_sec\r\nSmokeTestImport,What is JVM?,Verifier,Virtual Machine,VM,None,b,20,600\r\nSmokeTestImport,What does OOP stand for?,Old,Object-Oriented,Open,None,b,20,600\r\n' > "$TEST_CSV_FILE"

TIMPORT_RAW=$(curl -s -w "---HTTPSTATUS---%{http_code}" \
  -X POST "${BASE}/api/tests/import" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@${TEST_CSV_FILE};type=text/csv" 2>/dev/null)
STATUS="${TIMPORT_RAW##*---HTTPSTATUS---}"
BODY="${TIMPORT_RAW%---HTTPSTATUS---*}"

check "POST /api/tests/import valid CSV => 201" "201" "$STATUS"
check "  createdTests == 1" "1" "$(jf '.createdTests')"
check "  updatedTests == 0" "0" "$(jf '.updatedTests')"

touch "$TEST_EMPTY_FILE"
TEMPTY_RAW=$(curl -s -w "---HTTPSTATUS---%{http_code}" \
  -X POST "${BASE}/api/tests/import" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@${TEST_EMPTY_FILE};type=text/csv" 2>/dev/null)
STATUS="${TEMPTY_RAW##*---HTTPSTATUS---}"
BODY="${TEMPTY_RAW%---HTTPSTATUS---*}"
check "POST /api/tests/import empty file => 400" "400" "$STATUS"

printf 'test_title,question_text,option_a,option_b,option_c,option_d,correct_option,points,time_limit_sec\r\nSmokeTestImport,Updated question?,A,B,C,D,a,20,600\r\n' > "$TEST_CSV_FILE"

TUPDATE_RAW=$(curl -s -w "---HTTPSTATUS---%{http_code}" \
  -X POST "${BASE}/api/tests/import" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@${TEST_CSV_FILE};type=text/csv" 2>/dev/null)
STATUS="${TUPDATE_RAW##*---HTTPSTATUS---}"
BODY="${TUPDATE_RAW%---HTTPSTATUS---*}"

check "POST /api/tests/import upsert existing => 201" "201" "$STATUS"
check "  createdTests == 0 (upsert)" "0" "$(jf '.createdTests')"
check "  updatedTests == 1 (upsert)" "1" "$(jf '.updatedTests')"

rm -f "$TEST_CSV_FILE" "$TEST_EMPTY_FILE"

call GET /api/tests
SMOKE_TEST_IMPORT_ID=$(echo "$BODY" | sed 's/},{/\n{/g' | grep 'SmokeTestImport' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo -e "  Imported test id=${SMOKE_TEST_IMPORT_ID}"



h "11. Authorization checks"

call_noauth GET /api/tests
check "GET /api/tests without token => 401" "401" "$STATUS"

call_noauth GET /api/practices -H "Authorization: Bearer invalid.token.here"
check "GET /api/practices with invalid token => 401" "401" "$STATUS"


h "12. Cleanup"
call DELETE /api/assignments/${PRACTICE_ASSIGN_ID}
check "DELETE /api/assignments/{id} (practice) => 204" "204" "$STATUS"

call DELETE /api/assignments/${ASSIGN_ID}
check "DELETE /api/assignments/{id} (test) => 204" "204" "$STATUS"

call DELETE /api/practices/${PRACTICE_ID}
check "DELETE /api/practices/{id} => 204" "204" "$STATUS"

if [[ -n "$SMOKE_IMPORT_ID" ]]; then
  call DELETE /api/practices/${SMOKE_IMPORT_ID}
  check "DELETE /api/practices/{id} (imported) => 204" "204" "$STATUS"
fi

call DELETE /api/tests/${TEST_ID}
check "DELETE /api/tests/{id} => 204" "204" "$STATUS"

if [[ -n "$SMOKE_TEST_IMPORT_ID" ]]; then
  call DELETE /api/tests/${SMOKE_TEST_IMPORT_ID}
  check "DELETE /api/tests/{id} (imported) => 204" "204" "$STATUS"
fi

call DELETE /api/groups/${GROUP_ID}
check "DELETE /api/groups/{id} => 204" "204" "$STATUS"


echo -e "  ${GREEN}Passed: ${PASSED}${RESET}   ${RED}Failed: ${FAILED}${RESET}"

[[ $FAILED -eq 0 ]]
