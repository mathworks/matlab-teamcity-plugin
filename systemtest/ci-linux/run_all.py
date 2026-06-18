"""
Single-command end-to-end system test for CI (Docker server + Docker agent).

Usage:
    python run_all.py                          # Full run from scratch
    python run_all.py --skip-teardown          # Reuse existing containers
    MATLAB_PATH=/opt/matlab python run_all.py  # Custom MATLAB path

Environment variables:
    MATLAB_PATH  - Path to MATLAB inside the agent container (default: /opt/matlab)
    TC_URL       - TeamCity server URL (default: http://localhost:8111)

Pipeline:
  1. Teardown  - docker compose down -v
  2. Start     - docker compose up -d (server + agent)
  3. Setup     - Maintenance wizard, admin user, plugin verify, agent authorize
  4. Configure - Create project, VCS root, 5 build configurations
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
    print(f"# START SERVER + AGENT")
    print(f"{'#'*60}\n")
    sys.stdout.flush()

    result = subprocess.run(
        ["docker", "compose", "up", "-d"],
        cwd=SCRIPT_DIR,
    )
    if result.returncode != 0:
        print("FAILED: docker compose up")
        sys.exit(1)
    print("Server and agent containers started.")


def main():
    import argparse
    parser = argparse.ArgumentParser(description="CI end-to-end system test runner")
    parser.add_argument("--skip-teardown", action="store_true",
                        help="Skip teardown, reuse existing containers")
    args = parser.parse_args()

    start = time.time()

    if not args.skip_teardown:
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
