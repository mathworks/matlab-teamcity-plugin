"""
Single-command end-to-end system test: fully native (no Docker).

Architecture:
  - TeamCity Server: Native Java process (catalina.bat)
  - TeamCity Agent: Native Windows process (agent.bat)
  - MATLAB: Installed on host via setup-matlab
  - Docker: NOT REQUIRED

Usage:
    python run_all.py

Environment variables:
    TC_URL         - TeamCity server URL (default: http://localhost:8111)
    SERVER_DIR     - Server install dir (default: C:\\TeamCity)
    DATA_DIR       - Server data dir (default: C:\\TeamCity-data)
    AGENT_DIR      - Agent install dir (default: C:\\BuildAgent)
    TC_VERSION     - TeamCity version (default: 2025.03.3)
    JAVA_HOME      - JDK 11+ installation
    PLUGIN_ZIP     - Path to plugin ZIP file (required)

Pipeline:
  1. Install   - Download TeamCity, extract, deploy plugin, start server
  2. Setup     - Maintenance wizard, admin user, plugin verify
  3. Agent     - Download agent from server, configure, start
  4. Authorize - Wait for agent connection, authorize
  5. Configure - Create project, VCS root, 5 build configurations
  6. Test      - Trigger all builds, validate status + logs + artifacts
"""

import os
import subprocess
import sys
import time

os.environ["PYTHONUNBUFFERED"] = "1"

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))


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


def main():
    start = time.time()

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
