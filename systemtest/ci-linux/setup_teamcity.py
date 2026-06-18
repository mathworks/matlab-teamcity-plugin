"""
Automated first-run setup for TeamCity server + Docker agent in CI.

Same lifecycle as the local setup but uses the Docker agent instead of a native
host agent. The Docker agent auto-connects via SERVER_URL env var; this script
just waits for it and authorizes it.

Phases:
1. Wait for server HTTP
2. Complete maintenance wizard
3. Wait for REST API
4. Create admin user (via super user token)
5. Verify MATLAB plugin and runners
6. Wait for Docker agent to connect and authorize it
"""

import os
import re
import subprocess
import sys
import time
import requests

TC_URL = os.environ.get("TC_URL", "http://localhost:8111")
ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "admin"
EXPECTED_RUNNERS = ["matlabTestRunner", "matlabBuildRunner", "matlabCommandRunner"]


# --------------- Maintenance Wizard (pre-REST API) ---------------

def wait_for_server_http(timeout=300, interval=10):
    print("Waiting for TeamCity HTTP to respond...")
    start = time.time()
    while time.time() - start < timeout:
        try:
            r = requests.get(f"{TC_URL}/mnt", timeout=10, allow_redirects=False)
            print(f"  Server responded: HTTP {r.status_code}")
            return True
        except (requests.ConnectionError, requests.ReadTimeout):
            pass
        time.sleep(interval)
    print("ERROR: Server did not respond within timeout.")
    return False


def get_maintenance_stage():
    r = requests.get(f"{TC_URL}/mnt", timeout=10, allow_redirects=False)
    if r.status_code == 302:
        return None
    match = re.search(r"Stage:\s*(\S+)", r.text)
    if match:
        return match.group(1)
    return "UNKNOWN"


def get_maintenance_session():
    r = requests.get(f"{TC_URL}/mnt", timeout=10, allow_redirects=False)
    session_id = r.cookies.get("TCSESSIONID")
    cookies = {"TCSESSIONID": session_id} if session_id else {}
    csrf = None
    match = re.search(r'<meta\s+name="tc-csrf-token"\s+content="([^"]+)"', r.text)
    if match:
        csrf = match.group(1)
    return cookies, csrf


def maintenance_post(command, data=None):
    cookies, csrf = get_maintenance_session()
    headers = {}
    if csrf:
        headers["X-TC-CSRF-Token"] = csrf
    r = requests.post(
        f"{TC_URL}/mnt/do/{command}",
        data=data or {},
        cookies=cookies,
        headers=headers,
        timeout=30
    )
    return r.text.strip()


def complete_maintenance_wizard(timeout=600, interval=10):
    print("Completing maintenance wizard...")
    start = time.time()
    last_stage = None

    while time.time() - start < timeout:
        stage = get_maintenance_stage()

        if stage is None:
            print("  Wizard complete -- server is redirecting to main UI.")
            return True

        if stage != last_stage:
            print(f"  Current stage: {stage}")
            last_stage = stage

        if stage == "FIRST_START_SCREEN":
            result = maintenance_post("goNewInstallation", {"restore": "false"})
            print(f"    goNewInstallation: {result}")
        elif stage == "DB_SETTINGS_SCREEN":
            result = maintenance_post("goNewDatabase", {"dbType": "HSQLDB2"})
            print(f"    goNewDatabase (HSQLDB): {result}")
        elif stage == "LICENSE_AGREEMENT_SCREEN":
            result = maintenance_post("acceptLicenseAgreement")
            print(f"    acceptLicenseAgreement: {result}")
        elif stage == "SCHEMA_VERSION_MISMATCH_SCREEN":
            result = maintenance_post("proceedWithUpgrade")
            print(f"    proceedWithUpgrade: {result}")
        elif stage == "APPLICATION_STARTING":
            pass
        else:
            print(f"    Unknown stage '{stage}', waiting...")

        time.sleep(interval)

    print("ERROR: Maintenance wizard did not complete within timeout.")
    return False


# --------------- REST API Setup (post-wizard) ---------------

def wait_for_rest_api(timeout=300, interval=10):
    print("Waiting for REST API...")
    start = time.time()
    while time.time() - start < timeout:
        try:
            r = requests.get(f"{TC_URL}/app/rest/server", timeout=5)
            if r.status_code in (200, 401):
                print("  REST API is ready.")
                return True
        except (requests.ConnectionError, requests.ReadTimeout):
            pass
        time.sleep(interval)
    print("ERROR: REST API did not become ready within timeout.")
    return False


def get_super_user_token():
    result = subprocess.run(
        ["docker", "logs", "teamcity-server"],
        capture_output=True, text=True
    )
    match = re.search(
        r"Super user authentication token:\s+(\d+)", result.stdout + result.stderr
    )
    if not match:
        return None
    return match.group(1)


def make_session(token):
    session = requests.Session()
    session.auth = ("", token)
    session.headers.update({"Accept": "application/json"})
    return session


def get_csrf_token(session):
    r = session.get(f"{TC_URL}/authenticationTest.html?csrf")
    if r.status_code == 200:
        return r.text.strip()
    return None


def accept_license(session, csrf):
    print("Accepting license agreement via REST API...")
    r = session.put(
        f"{TC_URL}/app/rest/server/licensingData/licenseAgreementAccepted",
        headers={"Content-Type": "text/plain", "X-TC-CSRF-Token": csrf},
        data="true"
    )
    if r.status_code in (200, 204):
        print("  License accepted.")
        return True
    if r.status_code == 404:
        print("  License endpoint not available (already accepted). Continuing.")
        return True
    print(f"  WARNING: License accept returned {r.status_code}: {r.text}")
    return False


def create_admin_user(session, csrf):
    print(f"Creating admin user '{ADMIN_USERNAME}'...")
    r = session.post(
        f"{TC_URL}/app/rest/users",
        headers={"Content-Type": "application/json", "X-TC-CSRF-Token": csrf},
        json={
            "username": ADMIN_USERNAME,
            "password": ADMIN_PASSWORD,
            "roles": {
                "role": [{"roleId": "SYSTEM_ADMIN", "scope": "g"}]
            }
        }
    )
    if r.status_code in (200, 201):
        print("  Admin user created.")
        return True
    if r.status_code == 400 and "already exists" in r.text.lower():
        print("  Admin user already exists.")
        return True
    print(f"  ERROR: Create user returned {r.status_code}: {r.text}")
    return False


def verify_plugin(session):
    print("Verifying MATLAB plugin is loaded...")
    r = session.get(f"{TC_URL}/app/rest/server/plugins")
    if r.status_code != 200:
        print(f"  ERROR: Could not fetch plugins: {r.status_code}")
        return False
    plugins = r.json().get("plugin", [])
    matlab_plugins = [p for p in plugins if "matlab" in p.get("name", "").lower()]
    if matlab_plugins:
        p = matlab_plugins[0]
        print(f"  MATLAB plugin found: name={p['name']}, version={p.get('version', '?')}")
        return True
    print("  ERROR: MATLAB plugin not found in loaded plugins.")
    return False


def verify_runners(session, csrf):
    print("Verifying MATLAB runner types...")
    r = session.get(f"{TC_URL}/app/rest/runTypes")
    if r.status_code == 200:
        data = r.json()
        runners = [rt.get("type") for rt in data.get("runType", [])]
        missing = [rt for rt in EXPECTED_RUNNERS if rt not in runners]
        if not missing:
            print(f"  All runners registered: {EXPECTED_RUNNERS}")
            return True
        print(f"  ERROR: Missing runners: {missing}")
        return False

    print("  /app/rest/runTypes not available. Verifying via temporary build config...")
    project_id = "PluginVerification"
    session.post(
        f"{TC_URL}/app/rest/projects",
        headers={"Content-Type": "application/json", "X-TC-CSRF-Token": csrf},
        json={"name": "Plugin Verification", "id": project_id,
              "parentProject": {"locator": "_Root"}}
    )
    all_ok = True
    for runner in EXPECTED_RUNNERS:
        config_id = f"verify_{runner}"
        session.post(
            f"{TC_URL}/app/rest/buildTypes",
            headers={"Content-Type": "application/json", "X-TC-CSRF-Token": csrf},
            json={"name": config_id, "id": config_id, "project": {"id": project_id}}
        )
        r = session.post(
            f"{TC_URL}/app/rest/buildTypes/id:{config_id}/steps",
            headers={"Content-Type": "application/json", "X-TC-CSRF-Token": csrf},
            json={"name": runner, "type": runner, "properties": {
                "property": [{"name": "MatlabPathKey", "value": "/opt/matlab"}]
            }}
        )
        if r.status_code in (200, 201):
            print(f"    {runner}: OK")
        else:
            print(f"    {runner}: FAILED ({r.status_code})")
            all_ok = False

    session.delete(
        f"{TC_URL}/app/rest/projects/id:{project_id}",
        headers={"X-TC-CSRF-Token": csrf}
    )
    return all_ok


# --------------- Docker Agent Wait & Authorize ---------------

def wait_for_agent(session, timeout=300, interval=10):
    print("Waiting for Docker agent to connect...")
    start = time.time()
    while time.time() - start < timeout:
        r = session.get(f"{TC_URL}/app/rest/agents?locator=connected:true,authorized:any")
        if r.status_code == 200:
            data = r.json()
            if data.get("count", 0) > 0:
                agent = data["agent"][0]
                print(f"  Agent connected: id={agent['id']}, name={agent.get('name', 'unknown')}")
                return agent["id"]
        time.sleep(interval)
    print("ERROR: No agent connected within timeout.")
    return None


def authorize_agent(session, csrf, agent_id):
    print(f"Authorizing agent {agent_id}...")
    r = session.put(
        f"{TC_URL}/app/rest/agents/id:{agent_id}/authorized",
        headers={"Content-Type": "text/plain", "Accept": "text/plain",
                 "X-TC-CSRF-Token": csrf},
        data="true"
    )
    if r.status_code in (200, 204):
        print("  Agent authorized.")
        return True
    print(f"  ERROR: Authorize agent returned {r.status_code}: {r.text}")
    return False


# --------------- Main ---------------

def main():
    if not wait_for_server_http():
        sys.exit(1)

    if not complete_maintenance_wizard():
        sys.exit(1)

    if not wait_for_rest_api():
        sys.exit(1)

    token = get_super_user_token()
    if not token:
        print("ERROR: Could not find super user token in container logs.")
        sys.exit(1)
    print(f"Super user token: {token}")

    session = make_session(token)

    csrf = get_csrf_token(session)
    if not csrf:
        print("ERROR: Could not obtain CSRF token.")
        sys.exit(1)
    print(f"CSRF token: {csrf}")

    accept_license(session, csrf)

    if not create_admin_user(session, csrf):
        sys.exit(1)

    if not verify_plugin(session):
        sys.exit(1)

    if not verify_runners(session, csrf):
        sys.exit(1)

    agent_id = wait_for_agent(session)
    if agent_id is None:
        print("ERROR: Docker agent did not connect. Check docker compose logs.")
        sys.exit(1)

    if not authorize_agent(session, csrf, agent_id):
        sys.exit(1)

    print("\nSetup completed successfully.")


if __name__ == "__main__":
    main()
