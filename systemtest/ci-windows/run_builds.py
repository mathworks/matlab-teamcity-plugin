"""
Trigger all builds, wait for completion, validate results.

Environment variables:
  TC_URL  - TeamCity server URL (default: http://localhost:8111)
"""

import io
import os
import sys
import time
import xml.etree.ElementTree as ET
import zipfile
import requests

TC_URL = os.environ.get("TC_URL", "http://localhost:8111")
ADMIN_AUTH = ("admin", "admin")
BUILD_TIMEOUT = 300
POLL_INTERVAL = 10

BUILD_CONFIGS = [
    "RunCommand_Disp",
    "RunCommand_Version",
    "RunBuild_DefaultTask",
    "RunTests_Basic",
    "RunTests_AllArtifacts",
]


def make_session():
    # Create authenticated session with CSRF token.
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
    # Queue a build for the given configuration and return the build ID.
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
    # Poll until build finishes, return status string.
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
    # Download the full build log as plain text.
    r = session.get(
        f"{TC_URL}/httpAuth/downloadBuildLog.html?buildId={build_id}",
        headers={"Accept": "text/plain"}
    )
    if r.status_code == 200:
        return r.text
    return ""


def list_artifacts(session, build_id):
    # List top-level artifact file names for a build.
    r = session.get(f"{TC_URL}/app/rest/builds/id:{build_id}/artifacts/children/")
    if r.status_code == 200:
        return [f.get("name", "") for f in r.json().get("file", [])]
    return []


def download_artifact(session, build_id, path):
    # Download a specific artifact by path, return raw bytes.
    r = session.get(
        f"{TC_URL}/httpAuth/app/rest/builds/id:{build_id}/artifacts/content/{path}",
        headers={"Accept": "application/octet-stream"}
    )
    if r.status_code == 200:
        return r.content
    return None


def validate_junit_xml(content):
    # Validate JUnit XML: 2 suites, 16 tests, 0 failures.
    try:
        root = ET.fromstring(content)
        if root.tag != "testsuites":
            return False, f"Root tag is '{root.tag}', expected 'testsuites'"

        suites = root.findall("testsuite")
        if len(suites) != 2:
            return False, f"Expected 2 test suites, found {len(suites)}"

        suite_names = {s.get("name") for s in suites}
        expected_suites = {"ParameterizedTestExample", "TestExamples"}
        if suite_names != expected_suites:
            return False, f"Suite names {suite_names} != expected {expected_suites}"

        total_tests = 0
        total_failures = 0
        total_errors = 0
        for suite in suites:
            total_tests += int(suite.get("tests", 0))
            total_failures += int(suite.get("failures", 0))
            total_errors += int(suite.get("errors", 0))

        if total_tests != 16:
            return False, f"Expected 16 tests, found {total_tests}"
        if total_failures != 0:
            return False, f"Expected 0 failures, found {total_failures}"
        if total_errors != 0:
            return False, f"Expected 0 errors, found {total_errors}"

        testcase_names = [tc.get("name", "") for tc in root.iter("testcase")]
        expected_methods = ["testDayofyear", "testNonLeapYear", "testLeapYear",
                           "testInvalidDateFormat", "testCorrectDateFormatButInvalidDate"]
        for method in expected_methods:
            if not any(method in name for name in testcase_names):
                return False, f"Test method '{method}' not found in testcase names"

        return True, f"16 tests, 0 failures, 2 suites ({', '.join(suite_names)})"
    except ET.ParseError as e:
        return False, f"XML parse error: {e}"


def validate_tap(content):
    # Validate TAP output: version 13, plan 1..16, all ok.
    lines = content.strip().split("\n")
    if not lines:
        return False, "Empty TAP file"

    if lines[0].strip() != "TAP version 13":
        return False, f"Expected 'TAP version 13', got '{lines[0].strip()}'"

    plan_line = None
    ok_count = 0
    not_ok_count = 0
    for line in lines:
        line = line.strip()
        if line.startswith("1.."):
            plan_line = line
        elif line.startswith("ok "):
            ok_count += 1
        elif line.startswith("not ok "):
            not_ok_count += 1

    if plan_line != "1..16":
        return False, f"Expected plan '1..16', got '{plan_line}'"
    if ok_count != 16:
        return False, f"Expected 16 ok, got {ok_count}"
    if not_ok_count != 0:
        return False, f"Expected 0 not ok, got {not_ok_count}"

    full_text = "\n".join(lines)
    if "ParameterizedTestExample" not in full_text:
        return False, "ParameterizedTestExample not referenced in TAP output"
    if "TestExamples" not in full_text:
        return False, "TestExamples not referenced in TAP output"

    return True, f"TAP version 13, plan 1..16, 16 ok, 0 not ok"


def validate_pdf(content):
    # Validate PDF: correct header, reasonable size, proper EOF.
    if not content or len(content) < 5:
        return False, "Empty or missing PDF"
    if content[:5] != b"%PDF-":
        return False, f"Invalid header: {content[:5]}"
    if len(content) < 10240:
        return False, f"PDF too small ({len(content)} bytes), expected > 10 KB"
    if b"%%EOF" not in content[-50:]:
        return False, "PDF missing %%EOF termination (possibly truncated)"
    return True, f"{len(content):,} bytes, valid header + EOF"


def validate_coverage_zip(content):
    # Validate HTML coverage ZIP: has index.html, coverage data for dayofyear.m.
    if not content or len(content) < 4:
        return False, "Empty or missing coverage"
    try:
        zf = zipfile.ZipFile(io.BytesIO(content))
    except zipfile.BadZipFile as e:
        return False, f"Invalid ZIP: {e}"

    names = zf.namelist()
    if "index.html" not in names:
        return False, "index.html not found in ZIP"
    if len(names) < 100:
        return False, f"Expected > 100 files, found {len(names)}"

    try:
        overall_data = zf.read("release/coverageData/OverallCoverageData.js").decode("utf-8", errors="replace")
    except KeyError:
        return False, "OverallCoverageData.js not found in ZIP"

    if "dayofyear.m" not in overall_data:
        return False, "dayofyear.m not referenced in coverage data"
    if '"Function"' not in overall_data or '"Statement"' not in overall_data:
        return False, "Missing Function/Statement metrics in coverage data"
    if '"PercentCoverage":0' in overall_data.replace(" ", ""):
        return False, "Coverage reports 0% for a metric"

    zf.close()
    return True, f"{len(content):,} bytes, {len(names)} files, dayofyear.m covered"


def validate_test_report_zip(content):
    # Validate HTML test report ZIP: has index.html, references test suites.
    if not content or len(content) < 4:
        return False, "Empty or missing test report"
    try:
        zf = zipfile.ZipFile(io.BytesIO(content))
    except zipfile.BadZipFile as e:
        return False, f"Invalid ZIP: {e}"

    names = zf.namelist()
    if "index.html" not in names:
        return False, "index.html not found in ZIP"

    try:
        html = zf.read("index.html").decode("utf-8", errors="replace")
    except KeyError:
        return False, "Could not read index.html"

    if "ParameterizedTestExample" not in html:
        return False, "ParameterizedTestExample not in test report HTML"
    if "TestExamples" not in html:
        return False, "TestExamples not in test report HTML"
    if "All tests passed" not in html:
        return False, "'All tests passed' not found in test report"
    if ">16<" not in html and ">16 " not in html and "16</span>" not in html:
        if "16" not in html:
            return False, "Test count 16 not found in report"
    if "R2026a" not in html:
        return False, "R2026a not found in test report"

    zf.close()
    return True, f"{len(content):,} bytes, all tests passed, R2026a, 16 tests"


def run_and_validate(session, config_id):
    # Trigger a build, wait for it, and validate its output.
    print(f"\n{'='*60}")
    print(f"BUILD: {config_id}")
    print(f"{'='*60}")

    build_id = trigger_build(session, config_id)
    if build_id is None:
        return False

    print(f"  Waiting for completion (timeout={BUILD_TIMEOUT}s)...")
    status = wait_for_build(session, build_id)
    print(f"  Status: {status}")

    if status != "SUCCESS":
        log = download_build_log(session, build_id)
        print(f"  Build log (last 500 chars):\n{log[-500:]}")
        return False

    if config_id == "RunCommand_Disp":
        log = download_build_log(session, build_id)
        if "hello from MATLAB" in log:
            print("  PASS: Log contains 'hello from MATLAB'")
        else:
            print("  FAIL: Log missing 'hello from MATLAB'")
            return False

    elif config_id == "RunCommand_Version":
        log = download_build_log(session, build_id)
        if "MATLAB Version" in log:
            print("  PASS: Log contains MATLAB version info")
        else:
            print("  FAIL: Log missing MATLAB version info")
            return False

    elif config_id == "RunBuild_DefaultTask":
        print("  PASS: Build task completed successfully")

    elif config_id == "RunTests_Basic":
        artifacts = list_artifacts(session, build_id)
        print(f"  Artifacts: {artifacts}")
        content = download_artifact(session, build_id, "results.xml")
        if content is None:
            print("  FAIL: Could not download JUnit XML")
            return False
        ok, info = validate_junit_xml(content)
        if ok:
            print(f"  PASS: JUnit XML valid -- {info}")
        else:
            print(f"  FAIL: JUnit XML invalid -- {info}")
            return False

    elif config_id == "RunTests_AllArtifacts":
        artifacts = list_artifacts(session, build_id)
        print(f"  Artifacts: {artifacts}")

        content = download_artifact(session, build_id, "results.xml")
        if not content:
            print("  FAIL: JUnit XML not found")
            return False
        ok, info = validate_junit_xml(content)
        print(f"  JUnit XML: {'PASS' if ok else 'FAIL'} -- {info}")
        if not ok:
            return False

        content = download_artifact(session, build_id, "results.tap")
        if not content:
            print("  FAIL: TAP file not found")
            return False
        ok, info = validate_tap(content.decode("utf-8", errors="replace"))
        print(f"  TAP: {'PASS' if ok else 'FAIL'} -- {info}")
        if not ok:
            return False

        content = download_artifact(session, build_id, "report.pdf")
        if not content:
            print("  FAIL: PDF not found")
            return False
        ok, info = validate_pdf(content)
        print(f"  PDF: {'PASS' if ok else 'FAIL'} -- {info}")
        if not ok:
            return False

        content = download_artifact(session, build_id, "coverage")
        if not content:
            print("  FAIL: HTML Coverage not found")
            return False
        ok, info = validate_coverage_zip(content)
        print(f"  HTML Coverage: {'PASS' if ok else 'FAIL'} -- {info}")
        if not ok:
            return False

        content = download_artifact(session, build_id, "test-report")
        if not content:
            print("  FAIL: HTML Test Report not found")
            return False
        ok, info = validate_test_report_zip(content)
        print(f"  HTML Test Report: {'PASS' if ok else 'FAIL'} -- {info}")
        if not ok:
            return False

    return True


def main():
    print("=" * 60)
    print("SYSTEM TEST: End-to-End Build Validation (Native)")
    print("=" * 60)

    session = make_session()
    results = {}

    for config_id in BUILD_CONFIGS:
        passed = run_and_validate(session, config_id)
        results[config_id] = "PASS" if passed else "FAIL"

    print(f"\n{'='*60}")
    print("RESULTS SUMMARY")
    print(f"{'='*60}")
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
