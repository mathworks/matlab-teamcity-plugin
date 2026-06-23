"""
Create project, VCS root, and build configurations via REST API.

Environment variables:
  TC_URL       - TeamCity server URL (default: http://localhost:8111)
  MATLAB_PATH  - MATLAB installation root (auto-detected from PATH)
"""

import os
import shutil
import sys
import requests

TC_URL = os.environ.get("TC_URL", "http://localhost:8111")
ADMIN_AUTH = ("admin", "admin")
VCS_REPO_URL = "https://github.com/ymannem-MW/ci-configuration-examples"
PROJECT_ID = "MatlabSystemTests"
PROJECT_NAME = "MATLAB System Tests"


def detect_matlab_path():
    # Find MATLAB installation from PATH (setup-matlab adds it).
    matlab_exe = shutil.which("matlab")
    if matlab_exe:
        bin_dir = os.path.dirname(os.path.abspath(matlab_exe))
        return os.path.dirname(bin_dir)
    print("ERROR: MATLAB not found on PATH. Ensure setup-matlab has run.")
    sys.exit(1)


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


def create_project(session):
    # Create the top-level test project.
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
    # Create VCS root pointing to the test repository.
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
    # Attach VCS root to a build configuration.
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
    # Create a build configuration in the project.
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
    # Add a build step with the specified runner and properties.
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
    # Set artifact publishing rules on a build configuration.
    r = session.put(
        f"{TC_URL}/app/rest/buildTypes/id:{config_id}/settings/artifactRules",
        headers={"Content-Type": "text/plain", "Accept": "text/plain"},
        data=rules
    )
    if r.status_code in (200, 204):
        return True
    print(f"  ERROR setting artifact rules on {config_id}: {r.status_code}: {r.text}")
    return False


def get_build_configs(matlab_path):
    return [
        {
            "id": "RunCommand_Disp",
            "name": "Run MATLAB Command - disp",
            "runner": "matlabCommandRunner",
            "step_name": "Run MATLAB Command",
            "properties": {
                "MatlabPathKey": matlab_path,
                "matlabCommand": "disp('hello from MATLAB')",
            },
            "artifact_rules": None
        },
        {
            "id": "RunBuild_DefaultTask",
            "name": "Run MATLAB Build - Test Task",
            "runner": "matlabBuildRunner",
            "step_name": "Run MATLAB Build",
            "properties": {
                "MatlabPathKey": matlab_path,
                "matlabTasks": "test",
            },
            "artifact_rules": None
        },
        {
            "id": "RunTests_Basic",
            "name": "Run MATLAB Tests - Basic",
            "runner": "matlabTestRunner",
            "step_name": "Run MATLAB Tests",
            "properties": {
                "MatlabPathKey": matlab_path,
                "sourceFolders": "code",
                "junitArtifact": "matlabTestArtifacts/results.xml",
                "logOutputDetail": "Default",
                "logLoggingLevel": "Default",
            },
            "artifact_rules": "matlabTestArtifacts/**"
        },
    ]


def main():
    matlab_path = detect_matlab_path()
    print(f"MATLAB_PATH = {matlab_path}")
    build_configs = get_build_configs(matlab_path)
    session = make_session()

    if not create_project(session):
        sys.exit(1)

    vcs_id = create_vcs_root(session)
    if not vcs_id:
        sys.exit(1)

    for cfg in build_configs:
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
    for cfg in build_configs:
        print(f"  {cfg['id']:30s} -> {cfg['runner']}")
    print(f"\nMATLAB path:  {matlab_path}")
    print(f"VCS root:     {VCS_REPO_URL}")
    print(f"Project:      {TC_URL}/project/{PROJECT_ID}")


if __name__ == "__main__":
    main()
