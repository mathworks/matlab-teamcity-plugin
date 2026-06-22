"""
Single-command end-to-end system test: fully native (no Docker).

Architecture:
  - TeamCity Server: Native Java process (teamcity-server.bat)
  - TeamCity Agent: Native Windows process (agent.bat)
  - MATLAB: Installed on host via setup-matlab or pre-installed
  - Docker: NOT REQUIRED

Usage:
    python run_all.py                    # Full run from scratch
    python run_all.py --skip-teardown    # Reuse existing server

Environment variables:
    TC_URL         - TeamCity server URL (default: http://localhost:8111)
    SERVER_DIR     - Server install dir (default: C:\\TeamCity)
    DATA_DIR       - Server data dir (default: C:\\TeamCity-data)
    AGENT_DIR      - Agent install dir (default: C:\\BuildAgent)
    TC_VERSION     - TeamCity version (default: 2025.03.3)
    JAVA_HOME      - JDK 11+ installation
    MATLAB_PATH    - MATLAB root (auto-detected from PATH)

Pipeline:
  1. Teardown  - Stop server/agent processes, delete directories
  2. Install   - Download TeamCity, extract, deploy plugin, start server
  3. Setup     - Maintenance wizard, admin user, plugin verify
  4. Agent     - Download agent from server, configure, start
  5. Authorize - Wait for agent connection, authorize
  6. Configure - Create project, VCS root, 5 build configurations
  7. Test      - Trigger all builds, validate status + logs + artifacts
"""

import argparse
import os
import shutil
import subprocess
import sys
import time

os.environ["PYTHONUNBUFFERED"] = "1"

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SERVER_DIR = os.environ.get("SERVER_DIR", r"C:\TeamCity")
DATA_DIR = os.environ.get("DATA_DIR", r"C:\TeamCity-data")
AGENT_DIR = os.environ.get("AGENT_DIR", r"C:\BuildAgent")


def run_script(name, script, extra_args=None):
    # Run a sub-script by name, exit on failure.
    path = os.path.join(SCRIPT_DIR, script)
    print(f"\n{'#'*60}")
    print(f"# {name}")
    print(f"{'#'*60}\n")
    sys.stdout.flush()
    result = subprocess.run(
        [sys.executable, "-u", path] + (extra_args or []), cwd=SCRIPT_DIR
    )
    if result.returncode != 0:
        print(f"\nFAILED: {name} (exit code {result.returncode})")
        sys.exit(result.returncode)


def teardown():
    # Stop all processes and remove data directories for a clean slate.
    print(f"\n{'#'*60}")
    print(f"# TEARDOWN")
    print(f"{'#'*60}\n")
    sys.stdout.flush()

    agent_bat = os.path.join(AGENT_DIR, "bin", "agent.bat")
    if os.path.isfile(agent_bat):
        print("Stopping agent...")
        subprocess.run(
            [agent_bat, "stop"],
            cwd=os.path.join(AGENT_DIR, "bin"),
            capture_output=True, shell=True
        )
        time.sleep(3)

    server_bat = os.path.join(SERVER_DIR, "bin", "teamcity-server.bat")
    if os.path.isfile(server_bat):
        print("Stopping server...")
        env = os.environ.copy()
        env["TEAMCITY_DATA_PATH"] = DATA_DIR
        subprocess.run(
            [server_bat, "stop"],
            cwd=os.path.join(SERVER_DIR, "bin"),
            capture_output=True, shell=True,
            env=env
        )
        time.sleep(5)

    print("Killing lingering Java processes...")
    subprocess.run(
        ["powershell", "-Command",
         "Get-CimInstance Win32_Process -Filter \"name='java.exe'\" | "
         "Where-Object { $_.CommandLine -like '*TeamCity*' -or $_.CommandLine -like '*BuildAgent*' } | "
         "ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"],
        capture_output=True
    )
    time.sleep(2)

    if os.path.isdir(AGENT_DIR):
        print(f"Removing {AGENT_DIR}...")
        shutil.rmtree(AGENT_DIR, ignore_errors=True)

    if os.path.isdir(DATA_DIR):
        print(f"Removing {DATA_DIR}...")
        shutil.rmtree(DATA_DIR, ignore_errors=True)

    logs_dir = os.path.join(SERVER_DIR, "logs")
    if os.path.isdir(logs_dir):
        print(f"Clearing {logs_dir}...")
        shutil.rmtree(logs_dir, ignore_errors=True)

    print("Teardown complete.")


def main():
    parser = argparse.ArgumentParser(
        description="Fully native system test: no Docker, server + agent as Java processes"
    )
    parser.add_argument("--skip-teardown", action="store_true",
                        help="Skip teardown, reuse existing server")
    parser.add_argument("--clean-install", action="store_true",
                        help="Also delete SERVER_DIR (forces re-download of 1.5GB)")
    args = parser.parse_args()

    start = time.time()

    if not args.skip_teardown:
        teardown()
        if args.clean_install and os.path.isdir(SERVER_DIR):
            print(f"Removing {SERVER_DIR} (clean install)...")
            shutil.rmtree(SERVER_DIR, ignore_errors=True)

    run_script("INSTALL SERVER (download + extract + deploy plugin + start)", "install_server.py")
    run_script("SETUP SERVER (wizard + admin + plugin verify)",
               "setup_teamcity.py", ["--mode", "server-only"])
    run_script("INSTALL AGENT (download + configure + start)", "install_agent.py")

    print("\nWaiting 15s for agent to register with server...")
    time.sleep(15)

    run_script("AUTHORIZE AGENT (wait + authorize)",
               "setup_teamcity.py", ["--mode", "agent-only"])

    print("\nWaiting 30s for agent to be fully ready...")
    time.sleep(30)

    run_script("CONFIGURE (project + build configs)", "configure_project.py")
    run_script("TEST (trigger builds + validate results)", "run_builds.py")

    elapsed = time.time() - start
    minutes = int(elapsed // 60)
    seconds = int(elapsed % 60)
    print(f"\n{'#'*60}")
    print(f"# ALL DONE -- {minutes}m {seconds}s total")
    print(f"{'#'*60}")


if __name__ == "__main__":
    main()
