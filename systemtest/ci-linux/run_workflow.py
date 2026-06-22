"""
End-to-end system test orchestrator for Linux.

Architecture:
  - TeamCity Server: Docker container
  - TeamCity Agent: Docker container
  - MATLAB: Installed in agent container at /opt/matlab/R2026a

Pipeline:
  1. Teardown  - docker compose down -v
  2. Start     - docker compose up -d (server + agent)
  3. Setup     - Maintenance wizard, admin user, plugin verify, agent authorize
  4. Configure - Create project, VCS root, 3 build configurations
  5. Test      - Trigger all builds, validate status + logs + artifacts
"""

import os
import subprocess
import sys
import time

os.environ["PYTHONUNBUFFERED"] = "1"

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))


def run_script(name, script):
    path = os.path.join(SCRIPT_DIR, script)
    print(f"\n{'#'*60}")
    print(f"# {name}")
    print(f"{'#'*60}\n")
    sys.stdout.flush()
    result = subprocess.run(
        [sys.executable, "-u", path], cwd=SCRIPT_DIR
    )
    if result.returncode != 0:
        print(f"\nFAILED: {name} (exit code {result.returncode})")
        sys.exit(result.returncode)


def teardown():
    print(f"\n{'#'*60}")
    print(f"# TEARDOWN")
    print(f"{'#'*60}\n")
    sys.stdout.flush()

    subprocess.run(
        ["docker", "compose", "down", "-v"],
        cwd=SCRIPT_DIR,
    )
    print("Teardown complete.")


def start_services():
    print(f"\n{'#'*60}")
    print(f"# BUILD + START SERVER + AGENT")
    print(f"{'#'*60}\n")
    sys.stdout.flush()

    result = subprocess.run(
        ["docker", "compose", "build"],
        cwd=SCRIPT_DIR,
    )
    if result.returncode != 0:
        print("FAILED: docker compose build")
        sys.exit(1)

    result = subprocess.run(
        ["docker", "compose", "up", "-d"],
        cwd=SCRIPT_DIR,
    )
    if result.returncode != 0:
        print("FAILED: docker compose up")
        sys.exit(1)
    print("Server and agent containers started.")


def main():
    start = time.time()

    teardown()
    start_services()
    run_script("SETUP (wizard + admin + plugin + agent authorize)", "setup_teamcity.py")
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
