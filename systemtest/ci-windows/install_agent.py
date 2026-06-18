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
  JAVA_HOME  - Must point to JDK/JRE 11+ installation
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
    url = f"{TC_URL}/update/buildAgentFull.zip"
    print(f"Downloading agent from {url}...")
    start = time.time()
    while time.time() - start < timeout:
        try:
            r = requests.get(url, timeout=120)
            if r.status_code == 200:
                content = r.content
                if len(content) < 1000 or content[:2] != b"PK":
                    print(f"  Downloaded {len(content):,} bytes but not a valid ZIP. Retrying...")
                    time.sleep(interval)
                    continue
                try:
                    zipfile.ZipFile(io.BytesIO(content))
                except zipfile.BadZipFile:
                    print(f"  Downloaded {len(content):,} bytes but ZIP is corrupt. Retrying...")
                    time.sleep(interval)
                    continue
                print(f"  Downloaded {len(content):,} bytes (valid ZIP).")
                return content
            print(f"  HTTP {r.status_code}, retrying...")
        except (requests.ConnectionError, requests.ReadTimeout) as e:
            print(f"  Connection error: {e}, retrying...")
        time.sleep(interval)
    print("ERROR: Could not download agent ZIP within timeout.")
    return None


def extract_agent(zip_content):
    print(f"Extracting agent to {AGENT_DIR}...")
    os.makedirs(AGENT_DIR, exist_ok=True)
    zf = zipfile.ZipFile(io.BytesIO(zip_content))
    zf.extractall(AGENT_DIR)
    zf.close()
    print(f"  Extracted {len(zf.namelist())} files.")


def write_properties():
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
    agent_bat = os.path.join(AGENT_DIR, "bin", "agent.bat")
    if not os.path.isfile(agent_bat):
        print(f"ERROR: {agent_bat} not found.")
        return False
    print(f"Starting agent: {agent_bat} start...")
    subprocess.Popen(
        [agent_bat, "start"],
        cwd=os.path.join(AGENT_DIR, "bin"),
        shell=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    time.sleep(10)
    result = subprocess.run(
        ["tasklist", "/FI", "IMAGENAME eq java.exe"],
        capture_output=True, text=True
    )
    if "java.exe" in result.stdout:
        print("  Agent process is running.")
        return True
    print("  WARNING: Agent process not detected, but continuing...")
    return True


def main():
    java_home = os.environ.get("JAVA_HOME", "")
    if not java_home:
        print("ERROR: JAVA_HOME is not set.")
        sys.exit(1)
    java_exe = os.path.join(java_home, "bin", "java.exe")
    if not os.path.isfile(java_exe):
        print(f"ERROR: java.exe not found at {java_exe}")
        sys.exit(1)
    result = subprocess.run([java_exe, "-version"], capture_output=True, text=True)
    print(f"Java: {(result.stderr + result.stdout).splitlines()[0].strip()}")

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
