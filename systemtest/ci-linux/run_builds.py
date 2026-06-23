"""
Trigger builds, wait for completion, validate results.

Environment variables:
  TC_URL  - TeamCity server URL (default: http://localhost:8111)
"""

import os
import sys
import time
import xml.etree.ElementTree as ET
import requests

TC_URL = os.environ.get("TC_URL", "http://localhost:8111")
ADMIN_AUTH = ("admin", "admin")
BUILD_TIMEOUT = 300
POLL_INTERVAL = 10

BUILD_CONFIGS = [
    "RunCommand_Disp",
    "RunBuild_DefaultTask",
    "RunTests_Basic",
]


def make_session():
    session = requests.Session()
    session.auth = ADMIN_AUTH
    session.headers.update({"Accept": "application/json"})
    r = session.get(f"{TC_URL}/authenticationTest.html?csrf")
    csrf = r.text.strip()
    if r.status_code == 200 and len(csrf) < 100 and "\n" not in csrf:
        session.headers.update({"X-TC-CSRF-Token": csrf})
        return session
    print(f"ERROR: Authentication failed. Response: {csrf[:200]}")
    sys.exit(1)


def trigger_build(session, config_id):
    r = session.post(
        f"{TC_URL}/app/rest/buildQueue",
        headers={"Content-Type": "application/json"},
        json={"buildType": {"id": config_id}}
    )
    if r.status_code not in (200, 201):
        print(f"  ERROR triggering {config_id}: {r.status_code}: {r.text}")
        return None
    build_id = r.json()["id"]
    print(f"  Queued: build #{build_id}")
    return build_id


def wait_for_build(session, build_id):
    start = time.time()
    while time.time() - start < BUILD_TIMEOUT:
        r = session.get(f"{TC_URL}/app/rest/builds/id:{build_id}")
        if r.status_code == 200:
            data = r.json()
            if data.get("state") == "finished":
                return data.get("status", "UNKNOWN")
        time.sleep(POLL_INTERVAL)
    return "TIMEOUT"


def download_build_log(session, build_id):
    r = session.get(
        f"{TC_URL}/httpAuth/downloadBuildLog.html?buildId={build_id}",
        headers={"Accept": "text/plain"}
    )
    if r.status_code == 200:
        return r.text
    return ""


def download_artifact(session, build_id, path):
    r = session.get(
        f"{TC_URL}/httpAuth/app/rest/builds/id:{build_id}/artifacts/content/{path}",
        headers={"Accept": "application/octet-stream"}
    )
    if r.status_code == 200:
        return r.content
    return None


def validate_junit_xml(content):
    try:
        root = ET.fromstring(content)
        if root.tag != "testsuites":
            return False, f"Root tag is '{root.tag}', expected 'testsuites'"
        suites = root.findall("testsuite")
        if len(suites) < 1:
            return False, "No <testsuite> elements found"
        return True, f"Valid JUnit XML with {len(suites)} test suite(s)"
    except ET.ParseError as e:
        return False, f"XML parse error: {e}"


def run_and_validate(session, config_id, retries=2):
    print("\n" + "=" * 60)
    print(f"BUILD: {config_id}")
    print("=" * 60)

    for attempt in range(retries):
        build_id = trigger_build(session, config_id)
        if build_id is None:
            return False

        print(f"  Waiting for completion (timeout={BUILD_TIMEOUT}s)...")
        status = wait_for_build(session, build_id)
        print(f"  Status: {status}")

        if status != "SUCCESS":
            log = download_build_log(session, build_id)
            if attempt < retries - 1 and "agent didn't respond" in log.lower():
                print(f"  Agent not ready, retrying ({attempt+1}/{retries})...")
                time.sleep(30)
                continue
            print(f"  Build log (last 500 chars):\n{log[-500:]}")
            return False
        break

    log = download_build_log(session, build_id)

    if config_id == "RunCommand_Disp":
        if "hello from MATLAB" in log:
            print("  PASS: Log contains 'hello from MATLAB'")
        else:
            print("  FAIL: Log missing 'hello from MATLAB'")
            return False

    elif config_id == "RunBuild_DefaultTask":
        if "Build Successful" in log:
            print("  PASS: Build framework completed successfully")
        else:
            print("  FAIL: Build log missing 'Build Successful' message")
            return False

    elif config_id == "RunTests_Basic":
        content = download_artifact(session, build_id, "results.xml")
        if content is None:
            print("  FAIL: Could not download JUnit XML artifact")
            return False
        ok, info = validate_junit_xml(content)
        if ok:
            print(f"  PASS: {info}")
        else:
            print(f"  FAIL: {info}")
            return False

    return True


def main():
    print("=" * 60)
    print("SYSTEM TEST: End-to-End Build Validation")
    print("=" * 60)

    session = make_session()
    results = {}

    for config_id in BUILD_CONFIGS:
        passed = run_and_validate(session, config_id)
        results[config_id] = "PASS" if passed else "FAIL"

    print("\n" + "=" * 60)
    print("RESULTS SUMMARY")
    print("=" * 60)
    all_pass = True
    for config_id, result in results.items():
        print(f"  [{result}] {config_id}")
        if result != "PASS":
            all_pass = False

    passed_count = sum(1 for r in results.values() if r == "PASS")
    print(f"\n{passed_count}/{len(results)} builds passed.")

    if not all_pass:
        sys.exit(1)
    print("\nAll system tests passed!")


if __name__ == "__main__":
    main()
