"""
Automated first-run setup for TeamCity on Windows.

Super user token is read from the server log file.

Supports --mode flag:
  server-only: wizard + admin + plugin/runner verification (no agent wait)
  agent-only:  wait for agent connection + authorize
  full:        both (default)

Environment variables:
  TC_URL      - TeamCity server URL (default: http://localhost:8111)
  SERVER_DIR  - TeamCity server install directory (default: C:\\TeamCity)
  DATA_DIR    - TeamCity data directory (default: C:\\TeamCity-data)
"""

import argparse
import os
import re
import sys
import time
import requests

if sys.stdout.encoding != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

TC_URL = os.environ.get("TC_URL", "http://localhost:8111")
SERVER_DIR = os.environ.get("SERVER_DIR", r"C:\TeamCity")
DATA_DIR = os.environ.get("DATA_DIR", r"C:\TeamCity-data")
ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "admin"

SERVER_HTTP_TIMEOUT = 420
WIZARD_TIMEOUT = 600
REST_API_TIMEOUT = 300
AGENT_TIMEOUT = 300
POLL_INTERVAL = 10


def wait_for_server_http(timeout=SERVER_HTTP_TIMEOUT, interval=POLL_INTERVAL):
    # Poll server until it responds to HTTP requests.
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
    # Return the current maintenance wizard stage name.
    r = requests.get(f"{TC_URL}/mnt", timeout=10, allow_redirects=False)
    if r.status_code == 302:
        return None
    match = re.search(r"Stage:\s*(\S+)", r.text)
    if match:
        return match.group(1)
    return "UNKNOWN"


def get_maintenance_session():
    # Get session cookie and CSRF token from the maintenance page.
    r = requests.get(f"{TC_URL}/mnt", timeout=10, allow_redirects=False)
    session_id = r.cookies.get("TCSESSIONID")
    cookies = {"TCSESSIONID": session_id} if session_id else {}
    csrf = None
    match = re.search(r'<meta\s+name="tc-csrf-token"\s+content="([^"]+)"', r.text)
    if match:
        csrf = match.group(1)
    return cookies, csrf


def maintenance_post(command, data=None):
    # POST a command to the maintenance wizard.
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


def complete_maintenance_wizard(timeout=WIZARD_TIMEOUT, interval=POLL_INTERVAL):
    # Step through the maintenance wizard until server is ready.
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


def wait_for_rest_api(timeout=REST_API_TIMEOUT, interval=POLL_INTERVAL):
    # Poll until the REST API responds (200 or 401).
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
    # Read the most recent super user token from server log files.
    log_dirs = [
        os.path.join(SERVER_DIR, "logs"),
        os.path.join(DATA_DIR, "system", "logs"),
    ]

    latest_token = None
    found_in = None

    for logs_dir in log_dirs:
        if not os.path.isdir(logs_dir):
            continue
        for filename in os.listdir(logs_dir):
            log_path = os.path.join(logs_dir, filename)
            if not os.path.isfile(log_path):
                continue
            try:
                with open(log_path, "r", errors="replace") as f:
                    content = f.read()
                matches = re.findall(
                    r"Super user authentication token:\s+(\d+)", content
                )
                if matches:
                    latest_token = matches[-1]
                    found_in = log_path
            except (IOError, PermissionError):
                pass

    if latest_token:
        print(f"  Token found in {found_in}")
        return latest_token

    checked = [d for d in log_dirs if os.path.isdir(d)]
    print(f"  Token not found. Checked: {checked}")
    return None


def make_session(token):
    # Create a requests session authenticated with the super user token.
    session = requests.Session()
    session.auth = ("", token)
    session.headers.update({"Accept": "application/json"})
    return session


def accept_license_form(session, csrf):
    # Accept the license agreement via the HTML form.
    r = session.get(f"{TC_URL}/showAgreement.html", allow_redirects=False)
    if r.status_code != 200:
        return False
    match = re.search(r'name="tc-csrf-token"\s+content="([^"]+)"', r.text)
    page_csrf = match.group(1) if match else csrf
    r2 = session.post(
        f"{TC_URL}/showAgreement.html",
        data={"accept": "on", "Continue": "Continue"},
        headers={"X-TC-CSRF-Token": page_csrf},
        allow_redirects=False
    )
    return r2.status_code in (200, 302)


def get_csrf_token(session):
    # Obtain a CSRF token, accepting license agreement if prompted.
    for attempt in range(3):
        r = session.get(f"{TC_URL}/authenticationTest.html?csrf", allow_redirects=False)
        if r.status_code == 200:
            token = r.text.strip()
            if len(token) < 200:
                return token
            match = re.search(r'[a-zA-Z0-9_\-]{20,}', token)
            if match:
                return match.group(0)
            return token
        if r.status_code in (302, 401):
            accept_license_form(session, "")
            continue
        break
    return None


def accept_license(session, csrf):
    # Accept license agreement via REST API or HTML form fallback.
    print("Accepting license agreement...")
    r = session.put(
        f"{TC_URL}/app/rest/server/licensingData/licenseAgreementAccepted",
        headers={"Content-Type": "text/plain", "X-TC-CSRF-Token": csrf},
        data="true"
    )
    if r.status_code in (200, 204):
        print("  License accepted (REST API).")
        return True
    if r.status_code == 404:
        print("  License endpoint not available (already accepted).")
        return True

    print("  REST API method didn't work, trying web form...")
    if accept_license_form(session, csrf):
        print("  License accepted (web form).")
        return True

    print("  ERROR: Could not accept license agreement.")
    return False


def create_admin_user(session, csrf):
    # Create the admin user via REST API.
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


def verify_plugin(session, csrf, retries=5, delay=10):
    # Verify that the MATLAB plugin is loaded on the server.
    print("Verifying MATLAB plugin is loaded...")
    for attempt in range(retries):
        r = session.get(f"{TC_URL}/app/rest/server/plugins",
                        headers={"Accept": "application/json"},
                        allow_redirects=False)
        if r.status_code == 302:
            location = r.headers.get("Location", "")
            if "Agreement" in location or "agreement" in location:
                print(f"  Attempt {attempt+1}: Redirected to license page, accepting...")
                accept_license_form(session, csrf)
                time.sleep(delay)
                continue
            print(f"  Attempt {attempt+1}: Redirect to {location}, retrying...")
            time.sleep(delay)
            continue
        if r.status_code != 200:
            print(f"  Attempt {attempt+1}: HTTP {r.status_code}, retrying...")
            time.sleep(delay)
            continue
        try:
            data = r.json()
        except Exception:
            print(f"  Attempt {attempt+1}: Non-JSON response, retrying...")
            time.sleep(delay)
            continue
        plugins = data.get("plugin", [])
        matlab_plugins = [p for p in plugins if "matlab" in p.get("name", "").lower()]
        if matlab_plugins:
            p = matlab_plugins[0]
            print(f"  MATLAB plugin found: name={p['name']}, version={p.get('version', '?')}")
            return True
        if attempt < retries - 1:
            print(f"  Attempt {attempt+1}: Plugin not yet loaded, retrying...")
            time.sleep(delay)
        else:
            print("  ERROR: MATLAB plugin not found in loaded plugins.")
    return False



def wait_for_agent(session, timeout=AGENT_TIMEOUT, interval=POLL_INTERVAL):
    # Wait for an agent to connect to the server.
    print("Waiting for agent to connect...")
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
    # Authorize a connected agent so it can run builds.
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


def main():
    parser = argparse.ArgumentParser(description="TeamCity setup (fully native)")
    parser.add_argument("--mode", choices=["full", "server-only", "agent-only"],
                        default="full",
                        help="server-only: wizard+admin+plugin. "
                             "agent-only: wait+authorize agent. "
                             "full: both (default).")
    args = parser.parse_args()

    if args.mode in ("full", "server-only"):
        if not wait_for_server_http():
            sys.exit(1)

        if not complete_maintenance_wizard():
            sys.exit(1)

        if not wait_for_rest_api():
            sys.exit(1)

        print("Looking for super user token in logs...")
        token = None
        for attempt in range(12):
            token = get_super_user_token()
            if token:
                break
            time.sleep(10)
        if not token:
            print("ERROR: Could not find super user token in server logs.")
            sys.exit(1)
        print("  Super user token acquired.")

        session = make_session(token)

        csrf = get_csrf_token(session)
        if not csrf:
            print("ERROR: Could not obtain CSRF token.")
            sys.exit(1)
        print("  CSRF token acquired.")

        if not accept_license(session, csrf):
            sys.exit(1)

        if not create_admin_user(session, csrf):
            sys.exit(1)

        if not verify_plugin(session, csrf):
            sys.exit(1)

        if args.mode == "server-only":
            print("\nServer setup completed successfully (agent step skipped).")
            return

    if args.mode == "agent-only":
        token = get_super_user_token()
        if not token:
            print("ERROR: Could not find super user token in server logs.")
            sys.exit(1)
        session = make_session(token)
        csrf = get_csrf_token(session)
        if not csrf:
            print("ERROR: Could not obtain CSRF token.")
            sys.exit(1)

    agent_id = wait_for_agent(session)
    if agent_id is None:
        print("ERROR: Agent did not connect. Check agent logs.")
        sys.exit(1)

    if not authorize_agent(session, csrf, agent_id):
        sys.exit(1)

    print("\nSetup completed successfully.")


if __name__ == "__main__":
    main()
