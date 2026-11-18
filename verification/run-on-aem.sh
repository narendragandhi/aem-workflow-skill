#!/usr/bin/env bash
# Runs the skill's Java templates in real workflows on a LOCAL AEM author, then removes
# everything it created (bundle, models, test page, service user, config).
#
#   AEM_SDK_API=<version your author runs> verification/run-on-aem.sh [all|deploy|scenarios|cleanup]
#
# Environment:
#   AEM_SDK_API   aem-sdk-api version matching the running author (required for deploy/all),
#                 e.g. 2026.7.27083.20260713T105930Z-260600. A newer API can import package
#                 versions the author does not export, and the bundle will not resolve.
#   AEM_URL       default http://localhost:4502 (only localhost/127.0.0.1 unless ALLOW_REMOTE=1)
#   AEM_USER      default admin
#   AEM_PASSWORD  default admin
#   KEEP=1        leave models, content and bundle in place after the run (for inspection)
set -euo pipefail

cd "$(dirname "$0")/.."
AEM_URL="${AEM_URL:-http://localhost:4502}"
AEM_USER="${AEM_USER:-admin}"
AEM_PASSWORD="${AEM_PASSWORD:-admin}"
BSN="com.example.aemworkflowskill.verification"
SERVLET="$AEM_URL/bin/aem-workflow-skill/verify"
MODELS="/var/workflow/models/aem-workflow-skill-it"
PAGE="/content/aem-workflow-skill-it/page"
CMD="${1:-all}"

case "$AEM_URL" in
  http://localhost:*|http://127.0.0.1:*) ;;
  *) [ "${ALLOW_REMOTE:-0}" = 1 ] || { echo "Refusing non-local AEM_URL ($AEM_URL); set ALLOW_REMOTE=1"; exit 2; } ;;
esac

PASSED=0
FAILED=0
CSRF=""

curl_aem() { curl -sS -u "$AEM_USER:$AEM_PASSWORD" "$@"; }
json() { python3 -I -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
post() { curl_aem -H "CSRF-Token: $CSRF" -X POST "$@"; }

csrf() { CSRF=$(curl_aem "$AEM_URL/libs/granite/csrf/token.json" | json "d['token']"); }

check() { # check <name> <expected> <actual>
  if [ "$2" = "$3" ]; then
    PASSED=$((PASSED + 1)); echo "  PASS $1"
  else
    FAILED=$((FAILED + 1)); echo "  FAIL $1: expected '$2', got '$3'"
  fi
}

deploy() {
  : "${AEM_SDK_API:?set AEM_SDK_API to the aem-sdk-api version your author runs}"
  echo "Building against aem-sdk-api $AEM_SDK_API"
  mvn -q -B -f verification/pom.xml package -DskipTests -Daem.sdk.api="$AEM_SDK_API"
  local jar
  jar=$(ls verification/target/aem-workflow-skill-verification-*.jar)
  curl_aem -o /dev/null -X POST -F action=uninstall "$AEM_URL/system/console/bundles/$BSN" || true
  curl_aem -o /dev/null -F action=install -F bundlestart=start -F bundlestartlevel=20 \
    -F "bundlefile=@$jar" "$AEM_URL/system/console/bundles"
  for _ in $(seq 1 30); do
    state=$(curl_aem "$AEM_URL/system/console/bundles/$BSN.json" | json "d['data'][0]['state']" 2>/dev/null || true)
    [ "$state" = "Active" ] && break
    sleep 1
  done
  [ "$state" = "Active" ] || { echo "Bundle not active (state: ${state:-unknown})"; exit 1; }
  # Servlet registration can lag bundle activation slightly
  for _ in $(seq 1 15); do
    code=$(curl_aem -o /dev/null -w "%{http_code}" "$SERVLET?op=page")
    [ "$code" = 200 ] && break
    sleep 1
  done
  csrf
  post "$SERVLET?op=configure" >/dev/null
  echo "Deployed bundle and service user"
}

# The workflow engine caches models by id and version, so every run creates its own models.
RUN="r$(date +%s)"

setup() {
  csrf
  post "$SERVLET?op=setup&run=$RUN" >/dev/null
  echo "Created models $MODELS/$RUN-* and page $PAGE"
}

start() { # start <model> -> instance id
  post "$SERVLET?op=start&model=$MODELS/$RUN-$1&payload=$PAGE" | json "d['id']"
}

state() { # state <id> -> JSON
  curl_aem -G "$SERVLET" --data-urlencode "op=state" --data-urlencode "id=$1"
}

wait_for() { # wait_for <id> <python condition on d> [seconds]
  local deadline=$((SECONDS + ${3:-30}))
  while [ $SECONDS -lt $deadline ]; do
    if [ "$(state "$1" | json "bool($2)")" = True ]; then return 0; fi
    sleep 1
  done
  echo "  timed out waiting for: $2"; state "$1"; echo; return 1
}

# Complete the current participant work item exactly as the Inbox "Complete" dialog does:
# POST /bin/workflow/inbox with cmd=advance, the chosen "Next Step" route and a comment.
complete() { # complete <id> <route name> <comment>
  local id="$1" name="$2" comment="$3"
  local s item route
  s=$(state "$id")
  item=$(echo "$s" | json "[w for w in d['workItems'] if w['type']=='PARTICIPANT'][0]['id']")
  route=$(echo "$s" | json "[r['id'] for w in d['workItems'] if w['type']=='PARTICIPANT' for r in w['routes'] if r['name']=='$name'][0]")
  post "$AEM_URL/bin/workflow/inbox" -o /dev/null \
    --data-urlencode "cmd=advance" --data-urlencode ":status=browser" \
    --data-urlencode "_charset_=utf-8" --data-urlencode "item=$item" \
    --data-urlencode "route-$item=$route" --data-urlencode "comment-$item=$comment"
}

# What the Inbox does first for a Dialog Participant Step: POST the dialog's ./ fields
# to the payload.
dialog_to_payload() { # dialog_to_payload <field=value>
  post "$AEM_URL$PAGE" -o /dev/null --data-urlencode ":status=browser" --data-urlencode "$1"
}

meta() { # meta <id> <key>
  state "$1" | json "d['metadata'].get('$2', '')"
}

scenarios() {
  setup
  echo "Scenario: reviewer picks Approve in the Inbox"
  id=$(start approval)
  wait_for "$id" "any(w['type']=='PARTICIPANT' for w in d['workItems'])"
  complete "$id" Approve "ship it"
  wait_for "$id" "d['state']=='COMPLETED'"
  check "decision persisted for later steps" "approve" "$(meta "$id" lastDecision)"
  check "approver is the reviewer" "$AEM_USER" "$(meta "$id" lastApprover)"
  check "comment comes from the Complete dialog" "True" \
    "$(state "$id" | json "'ship it' in d['metadata'].get('approvalHistory','')")"

  echo "Scenario: reviewer picks Reject in the Inbox"
  id=$(start approval)
  wait_for "$id" "any(w['type']=='PARTICIPANT' for w in d['workItems'])"
  complete "$id" Reject "needs work"
  wait_for "$id" "d['state']=='COMPLETED'"
  check "reject branch recorded" "reject" "$(meta "$id" lastDecision)"
  check "rejection reason is the comment" "needs work" "$(meta "$id" rejectionReason)"

  echo "Scenario: AI proposal approved, with a reviewer edit from the dialog"
  id=$(start ai)
  wait_for "$id" "any(w['type']=='PARTICIPANT' for w in d['workItems'])"
  check "proposal ready for review" "ready" "$(meta "$id" aiProposalStatus)"
  edit="Reviewer wording: a relaxed surf weekend on the Portuguese coast, lessons included."
  dialog_to_payload "./jcr:content/aiEditedDescription=$edit"
  complete "$id" Approve "edited"
  wait_for "$id" "d['state']=='COMPLETED'"
  check "apply outcome" "applied (edited by reviewer)" "$(meta "$id" aiApplyOutcome)"
  check "page description is the reviewer's edit" "$edit" \
    "$(curl_aem "$SERVLET?op=page" | json "d['description']")"
  check "review field removed from the page" "False" \
    "$(curl_aem "$AEM_URL$PAGE/jcr:content.json" | json "'aiEditedDescription' in d")"

  echo "Scenario: AI proposal rejected"
  id=$(start ai)
  wait_for "$id" "any(w['type']=='PARTICIPANT' for w in d['workItems'])"
  complete "$id" Reject "off brand"
  wait_for "$id" "d['state']=='COMPLETED'"
  check "rejected proposal not applied" "" "$(meta "$id" aiApplyOutcome)"
  check "page description unchanged" "$edit" "$(curl_aem "$SERVLET?op=page" | json "d['description']")"

  if [ "${RUN_JOB_SCENARIO:-0}" = 1 ]; then
    # Needs a healthy Sling job scheduler: the engine polls external steps via scheduled jobs.
    echo "Scenario: long-running job; the workflow waits for the result"
    id=$(start job)
    wait_for "$id" "d['state']=='COMPLETED'" 120
    check "job outcome recorded" "succeeded" "$(meta "$id" jobOutcome)"
  fi
}

cleanup() {
  csrf
  post "$SERVLET?op=cleanup" >/dev/null 2>&1 || true
  curl_aem -o /dev/null -X POST -F action=uninstall "$AEM_URL/system/console/bundles/$BSN" || true
  echo "Removed test workflows, models, content, service user, config and bundle"
}

case "$CMD" in
  deploy) deploy ;;
  scenarios) scenarios ;;
  cleanup) cleanup ;;
  all)
    deploy
    [ "${KEEP:-0}" = 1 ] || trap cleanup EXIT
    scenarios
    ;;
  *) echo "usage: $0 [all|deploy|scenarios|cleanup]"; exit 2 ;;
esac

if [ "$CMD" = all ] || [ "$CMD" = scenarios ]; then
  echo "Passed: $PASSED  Failed: $FAILED"
  [ "$FAILED" = 0 ]
fi
