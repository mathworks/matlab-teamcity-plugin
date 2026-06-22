"""
Download and install TeamCity build agent natively on Windows (no Docker).

Steps:
1. Download buildAgentFull.zip from the TeamCity server
2. Extract to AGENT_DIR
3. Write buildAgent.properties with serverUrl
4. Start agent.bat as background process

Environment variables:
  TC_URL     - TeamCity server URL (default: http://localhost:8111)
  AGENT_DIR  - Agent install directory (default: C:\\BuildAgent)
"""

import io
import os
import subprocess
import sys
import time
import zipfile
import requests

TC_URL = os.environ.get("TC_URL", "http://localhost:8111")
AGENT_DIR = os.environ.get("AGENT_DIR", r"C:\BuildAgent")


def download_agent_zip(timeout=180, interval=15):
    # Download agent ZIP from server, retrying until endpoint is available.
    url = f"{TC_URL}/update/buildAgentFull.zip"
    print(f"Downloading agent from {url}...")
    start = time.time()
    while time.time() - start < timeout:
        try:
            r = requests.get(url, timeout=120)
            if r.status_code == 200 and len(r.content) > 1000:
                print(f"  Downloaded {len(r.content):,} bytes.")
                return r.content
            print(f"  HTTP {r.status_code}, retrying...")
        except (requests.ConnectionError, requests.ReadTimeout) as e:
            print(f"  Connection error: {e}, retrying...")
        time.sleep(interval)
    print("ERROR: Could not download agent ZIP within timeout.")
    return None


def extract_agent(zip_content):
    # Extract agent ZIP contents to AGENT_DIR.
    print(f"Extracting agent to {AGENT_DIR}...")
    os.makedirs(AGENT_DIR, exist_ok=True)
    zf = zipfile.ZipFile(io.BytesIO(zip_content))
    file_count = len(zf.namelist())
    zf.extractall(AGENT_DIR)
    zf.close()
    print(f"  Extracted {file_count} files.")


def write_properties():
    # Write buildAgent.properties with server URL and directory paths.
    conf_dir = os.path.join(AGENT_DIR, "conf")
    os.makedirs(conf_dir, exist_ok=True)
    props_file = os.path.join(conf_dir, "buildAgent.properties")
    print(f"Writing {props_file}...")
    with open(props_file, "w") as f:
        f.write(f"serverUrl={TC_URL}\n")
        f.write(f"name=NativeWindowsAgent\n")
        f.write(f"workDir=../work\n")
        f.write(f"tempDir=../temp\n")
        f.write(f"systemDir=../system\n")
    print("  buildAgent.properties written.")


def start_agent():
    # Start agent.bat as a background process, logging output to file.
    agent_bat = os.path.join(AGENT_DIR, "bin", "agent.bat")
    if not os.path.isfile(agent_bat):
        print(f"ERROR: {agent_bat} not found.")
        return False
    print(f"Starting agent: {agent_bat} start...")
    log_dir = os.path.join(AGENT_DIR, "logs")
    os.makedirs(log_dir, exist_ok=True)
    log_file = open(os.path.join(log_dir, "agent-stdout.log"), "w")
    subprocess.Popen(
        [agent_bat, "start"],
        cwd=os.path.join(AGENT_DIR, "bin"),
        shell=True,
        stdout=log_file,
        stderr=log_file,
    )
    time.sleep(10)
    print("  Agent process launched.")
    return True


def main():
    zip_content = download_agent_zip()
    if zip_content is None:
        sys.exit(1)

    extract_agent(zip_content)
    write_properties()

    if not start_agent():
        sys.exit(1)

    print("\nAgent installed and started successfully.")


if __name__ == "__main__":
    main()
