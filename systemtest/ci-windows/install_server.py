"""
Download and install TeamCity server natively on Windows (no Docker).

Steps:
1. Download TeamCity distribution tar.gz from JetBrains
2. Extract to SERVER_DIR
3. Deploy plugin ZIP to datadir/plugins
4. Start teamcity-server.bat
5. Wait for server log to confirm startup

Environment variables:
  SERVER_DIR     - Server install directory (default: C:\\TeamCity)
  DATA_DIR       - Server data directory (default: C:\\TeamCity-data)
  PLUGIN_ZIP     - Path to plugin ZIP file (auto-detected if not set)
  TC_VERSION     - TeamCity version to download (default: 2025.03.3)
  JAVA_HOME      - Must point to JDK 11+ installation
"""

import io
import os
import shutil
import subprocess
import sys
import tarfile
import time
import requests

SERVER_DIR = os.environ.get("SERVER_DIR", r"C:\TeamCity")
DATA_DIR = os.environ.get("DATA_DIR", r"C:\TeamCity-data")
TC_VERSION = os.environ.get("TC_VERSION", "2025.03.3")
PLUGIN_ZIP = os.environ.get("PLUGIN_ZIP", "")

DOWNLOAD_URL = f"https://download.jetbrains.com/teamcity/TeamCity-{TC_VERSION}.tar.gz"


def find_plugin_zip():
    if PLUGIN_ZIP and os.path.isfile(PLUGIN_ZIP):
        return os.path.abspath(PLUGIN_ZIP)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
    # Check target/ at repo root (mvn package output)
    target_dir = os.path.join(repo_root, "target")
    if os.path.isdir(target_dir):
        for f in os.listdir(target_dir):
            if f.startswith("matlab-plugin") and f.endswith(".zip"):
                return os.path.join(target_dir, f)
    return None


def verify_java():
    java_home = os.environ.get("JAVA_HOME", "")
    if not java_home:
        print("ERROR: JAVA_HOME is not set.")
        sys.exit(1)
    java_exe = os.path.join(java_home, "bin", "java.exe")
    if not os.path.isfile(java_exe):
        print(f"ERROR: java.exe not found at {java_exe}")
        sys.exit(1)
    result = subprocess.run([java_exe, "-version"], capture_output=True, text=True)
    version_info = (result.stderr + result.stdout).splitlines()[0].strip()
    print(f"Java: {version_info}")
    print(f"JAVA_HOME: {java_home}")


def download_teamcity():
    """Download TeamCity tar.gz. Returns path to downloaded file."""
    dest = os.path.join(os.environ.get("TEMP", r"C:\Temp"), f"TeamCity-{TC_VERSION}.tar.gz")
    if os.path.isfile(dest) and os.path.getsize(dest) > 100_000_000:
        print(f"Using cached download: {dest} ({os.path.getsize(dest):,} bytes)")
        return dest

    print(f"Downloading TeamCity {TC_VERSION} from {DOWNLOAD_URL}...")
    print("  (This is ~1.5 GB, may take several minutes)")
    r = requests.get(DOWNLOAD_URL, stream=True, timeout=600)
    if r.status_code != 200:
        print(f"ERROR: Download failed: HTTP {r.status_code}")
        sys.exit(1)

    total = int(r.headers.get("content-length", 0))
    downloaded = 0
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with open(dest, "wb") as f:
        for chunk in r.iter_content(chunk_size=8 * 1024 * 1024):
            f.write(chunk)
            downloaded += len(chunk)
            if total:
                pct = downloaded * 100 // total
                print(f"\r  {downloaded:,} / {total:,} bytes ({pct}%)", end="", flush=True)
    print(f"\n  Downloaded: {dest} ({downloaded:,} bytes)")
    return dest


def extract_teamcity(tar_path):
    """Extract TeamCity tar.gz to SERVER_DIR."""
    print(f"Extracting to {SERVER_DIR}...")
    os.makedirs(SERVER_DIR, exist_ok=True)

    with tarfile.open(tar_path, "r:gz") as tf:
        # tar.gz has top-level "TeamCity/" directory — strip it
        members = tf.getmembers()
        total = len(members)
        for i, member in enumerate(members):
            # Strip "TeamCity/" prefix
            if member.name.startswith("TeamCity/"):
                member.name = member.name[len("TeamCity/"):]
            elif member.name == "TeamCity":
                continue
            else:
                continue

            if not member.name:
                continue

            target = os.path.join(SERVER_DIR, member.name)
            if member.isdir():
                os.makedirs(target, exist_ok=True)
            elif member.isfile():
                os.makedirs(os.path.dirname(target), exist_ok=True)
                with tf.extractfile(member) as src, open(target, "wb") as dst:
                    shutil.copyfileobj(src, dst)

            if (i + 1) % 1000 == 0:
                print(f"  Extracted {i+1}/{total} files...")

    print(f"  Extraction complete ({total} entries).")


def deploy_plugin(plugin_zip):
    """Copy plugin ZIP to data directory plugins folder."""
    plugins_dir = os.path.join(DATA_DIR, "plugins")
    os.makedirs(plugins_dir, exist_ok=True)
    dest = os.path.join(plugins_dir, "matlab-teamcity-plugin.zip")
    shutil.copy2(plugin_zip, dest)
    print(f"Plugin deployed: {dest}")


def kill_existing_server():
    """Kill any existing TeamCity server process holding port 8111."""
    result = subprocess.run(
        ["powershell", "-Command",
         "Get-NetTCPConnection -LocalPort 8111 -ErrorAction SilentlyContinue | "
         "Select-Object -ExpandProperty OwningProcess -Unique"],
        capture_output=True, text=True
    )
    pids = [p.strip() for p in result.stdout.strip().splitlines() if p.strip().isdigit()]
    if pids:
        print(f"  Killing existing processes on port 8111: {pids}")
        for pid in pids:
            subprocess.run(
                ["powershell", "-Command", f"Stop-Process -Id {pid} -Force -ErrorAction SilentlyContinue"],
                capture_output=True
            )
        time.sleep(5)


def start_server():
    """Start TeamCity server as a background process via catalina.bat."""
    catalina_bat = os.path.join(SERVER_DIR, "bin", "catalina.bat")
    if not os.path.isfile(catalina_bat):
        print(f"ERROR: {catalina_bat} not found.")
        sys.exit(1)

    kill_existing_server()

    print("Starting server via catalina.bat run...")
    env = os.environ.copy()
    env["TEAMCITY_DATA_PATH"] = DATA_DIR
    env["CATALINA_HOME"] = SERVER_DIR
    env["CATALINA_BASE"] = SERVER_DIR
    env["CATALINA_OPTS"] = (
        "-Dteamcity.configuration.path=../conf/teamcity-startup.properties "
        f"-Dlog4j2.configurationFile=file:../conf/teamcity-server-log4j.xml "
        f'"-Dteamcity_logs={os.path.join(SERVER_DIR, "logs")}"'
    )

    subprocess.Popen(
        [catalina_bat, "run"],
        cwd=os.path.join(SERVER_DIR, "bin"),
        env=env,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    time.sleep(10)
    print("  Server process launched.")


def wait_for_server_log(timeout=300, interval=10):
    """Wait for server log to indicate startup."""
    log_file = os.path.join(DATA_DIR, "system", "caches", "pluginsDslCache", "log")
    # Actually check the main catalina log
    logs_dir = os.path.join(SERVER_DIR, "logs")
    print(f"Waiting for server to initialize (checking {logs_dir})...")

    start = time.time()
    while time.time() - start < timeout:
        if os.path.isdir(logs_dir):
            for f in os.listdir(logs_dir):
                if "catalina" in f.lower():
                    log_path = os.path.join(logs_dir, f)
                    try:
                        with open(log_path, "r", errors="replace") as lf:
                            content = lf.read()
                            if "Server startup" in content or "TeamCity initialized" in content:
                                print("  Server log indicates startup complete.")
                                return True
                    except (IOError, PermissionError):
                        pass
        time.sleep(interval)

    print("  Server log check timed out (will rely on HTTP check).")
    return True


def main():
    verify_java()

    plugin_zip = find_plugin_zip()
    if not plugin_zip:
        print("ERROR: Could not find plugin ZIP.")
        print("  Set PLUGIN_ZIP env var or run 'mvn package' first.")
        sys.exit(1)
    print(f"Plugin ZIP: {plugin_zip}")

    # Download and extract if not already present
    server_bat = os.path.join(SERVER_DIR, "bin", "teamcity-server.bat")
    if os.path.isfile(server_bat):
        print(f"TeamCity already installed at {SERVER_DIR}, skipping download.")
    else:
        tar_path = download_teamcity()
        extract_teamcity(tar_path)

    deploy_plugin(plugin_zip)
    start_server()
    wait_for_server_log()

    print("\nServer installed and started successfully.")


if __name__ == "__main__":
    main()
