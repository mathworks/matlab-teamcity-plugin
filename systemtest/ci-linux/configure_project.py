"""
Create project, VCS root, and build configurations via REST API for Linux.

MATLAB_PATH: /opt/matlab/R2026a (mount point inside the Docker agent container).

Build configurations:
  1. RunCommand_Disp       - MATLAB Command with disp('hello')
  2. RunBuild_DefaultTask  - MATLAB Build with "test" task
  3. RunTests_Basic        - MATLAB Tests with JUnit artifact
"""

import os
import sys
import requests

TC_URL = os.environ.get("TC_URL", "http://localhost:8111")
ADMIN_AUTH = ("admin", "admin")
MATLAB_PATH = "/opt/matlab/R2026a"
VCS_REPO_URL = "https://github.com/ymannem-MW/ci-configuration-examples"
PROJECT_ID = "MatlabSystemTests"
PROJECT_NAME = "MATLAB System Tests"


def make_session():
    session = requests.Session()
    session.auth = ADMIN_AUTH
    session.headers.update({"Accept": "application/json"})
    r = session.get(f"{TC_URL}/authenticationTest.html?csrf")
    csrf = r.text.strip()
    if r.status_code != 200 or len(csrf) > 100 or "\n" in csrf:
        print(f"ERROR: Authentication failed. Response: {csrf[:200]}")
        sys.exit(1)
    session.headers.update({"X-TC-CSRF-Token": csrf})
    return session


def create_project(session):
    print(f"Creating project '{PROJECT_NAME}'...")
    r = session.post(
        f"{TC_URL}/app/rest/projects",
        headers={"Content-Type": "application/json"},
        json={
            "name": PROJECT_NAME,
            "id": PROJECT_ID,
            "parentProject": {"locator": "_Root"}
        }
    )
    if r.status_code in (200, 201):
        print(f"  Project created: {PROJECT_ID}")
        return True
    if "already exists" in r.text.lower():
        print(f"  Project already exists: {PROJECT_ID}")
        return True
    print(f"  ERROR: {r.status_code}: {r.text}")
    return False


def create_vcs_root(session):
    vcs_id = "MatlabSystemTests_CiConfigExamples"
    print(f"Creating VCS root '{vcs_id}'...")
    r = session.post(
        f"{TC_URL}/app/rest/vcs-roots",
        headers={"Content-Type": "application/json"},
        json={
            "id": vcs_id,
            "name": "ci-configuration-examples",
            "vcsName": "jetbrains.git",
            "project": {"id": PROJECT_ID},
            "properties": {
                "property": [
                    {"name": "url", "value": VCS_REPO_URL},
                    {"name": "branch", "value": "refs/heads/main"},
                    {"name": "authMethod", "value": "ANONYMOUS"}
                ]
            }
        }
    )
    if r.status_code in (200, 201):
        print(f"  VCS root created: {vcs_id}")
        return vcs_id
    if "already exists" in r.text.lower():
        print(f"  VCS root already exists: {vcs_id}")
        return vcs_id
    print(f"  ERROR: {r.status_code}: {r.text}")
    return None


def attach_vcs_root(session, config_id, vcs_id):
    r = session.post(
        f"{TC_URL}/app/rest/buildTypes/id:{config_id}/vcs-root-entries",
        headers={"Content-Type": "application/json"},
        json={
            "vcs-root": {"id": vcs_id},
            "checkout-rules": ""
        }
    )
    if r.status_code in (200, 201):
        return True
    if "already exists" in r.text.lower():
        return True
    print(f"  ERROR attaching VCS root to {config_id}: {r.status_code}: {r.text}")
    return False


def create_build_config(session, config_id, config_name):
    r = session.post(
        f"{TC_URL}/app/rest/buildTypes",
        headers={"Content-Type": "application/json"},
        json={
            "name": config_name,
            "id": config_id,
            "project": {"id": PROJECT_ID}
        }
    )
    if r.status_code in (200, 201):
        return True
    if "already exists" in r.text.lower():
        return True
    print(f"  ERROR creating build config {config_id}: {r.status_code}: {r.text}")
    return False


def add_build_step(session, config_id, step_name, runner_type, properties):
    prop_list = [{"name": k, "value": v} for k, v in properties.items()]
    r = session.post(
        f"{TC_URL}/app/rest/buildTypes/id:{config_id}/steps",
        headers={"Content-Type": "application/json"},
        json={
            "name": step_name,
            "type": runner_type,
            "properties": {"property": prop_list}
        }
    )
    if r.status_code in (200, 201):
        return True
    print(f"  ERROR adding step to {config_id}: {r.status_code}: {r.text}")
    return False


def set_artifact_rules(session, config_id, rules):
    r = session.put(
        f"{TC_URL}/app/rest/buildTypes/id:{config_id}/settings/artifactRules",
        headers={"Content-Type": "text/plain", "Accept": "text/plain"},
        data=rules
    )
    if r.status_code in (200, 204):
        return True
    print(f"  ERROR setting artifact rules on {config_id}: {r.status_code}: {r.text}")
    return False


# ---------- Build config definitions ----------

BUILD_CONFIGS = [
    {
        "id": "RunCommand_Disp",
        "name": "MATLAB Command",
        "runner": "matlabCommandRunner",
        "step_name": "Run MATLAB Command",
        "properties": {
            "MatlabPathKey": MATLAB_PATH,
            "matlabCommand": "disp('hello from MATLAB')",
        },
        "artifact_rules": None
    },
    {
        "id": "RunBuild_DefaultTask",
        "name": "MATLAB Build",
        "runner": "matlabBuildRunner",
        "step_name": "Run MATLAB Build",
        "properties": {
            "MatlabPathKey": MATLAB_PATH,
            "matlabTasks": "test",
        },
        "artifact_rules": None
    },
    {
        "id": "RunTests_Basic",
        "name": "MATLAB Tests",
        "runner": "matlabTestRunner",
        "step_name": "Run MATLAB Tests",
        "properties": {
            "MatlabPathKey": MATLAB_PATH,
            "sourceFolders": "code",
            "junitArtifact": "matlabTestArtifacts/results.xml",
            "logOutputDetail": "Default",
            "logLoggingLevel": "Default",
        },
        "artifact_rules": "matlabTestArtifacts/**"
    },
]


def main():
    print(f"MATLAB_PATH = {MATLAB_PATH}")
    session = make_session()

    if not create_project(session):
        sys.exit(1)

    vcs_id = create_vcs_root(session)
    if not vcs_id:
        sys.exit(1)

    for cfg in BUILD_CONFIGS:
        config_id = cfg["id"]
        print(f"\nConfiguring '{cfg['name']}' ({config_id})...")

        if not create_build_config(session, config_id, cfg["name"]):
            sys.exit(1)
        print(f"  Build config ready.")

        if not attach_vcs_root(session, config_id, vcs_id):
            sys.exit(1)
        print(f"  VCS root attached.")

        if not add_build_step(session, config_id, cfg["step_name"],
                              cfg["runner"], cfg["properties"]):
            sys.exit(1)
        print(f"  Build step added: {cfg['runner']}")

        if cfg["artifact_rules"]:
            if not set_artifact_rules(session, config_id, cfg["artifact_rules"]):
                sys.exit(1)
            print(f"  Artifact rules set: {cfg['artifact_rules']}")

    print("\n" + "=" * 60)
    print("Build configurations created:")
    print("=" * 60)
    for cfg in BUILD_CONFIGS:
        print(f"  {cfg['id']:30s} -> {cfg['runner']}")
    print(f"\nMATLAB path:  {MATLAB_PATH}")
    print(f"VCS root:     {VCS_REPO_URL}")
    print(f"Project:      {TC_URL}/project/{PROJECT_ID}")


if __name__ == "__main__":
    main()
