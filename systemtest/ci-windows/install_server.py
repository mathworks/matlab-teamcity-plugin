"""
Download and install TeamCity server natively on Windows (no Docker).

Steps:
1. Download TeamCity distribution tar.gz from JetBrains
2. Extract to SERVER_DIR
3. Deploy plugin ZIP to datadir/plugins
4. Start server via catalina.bat

Environment variables:
  SERVER_DIR     - Server install directory (default: C:\\TeamCity)
  DATA_DIR       - Server data directory (default: C:\\TeamCity-data)
  TC_VERSION     - TeamCity version to download (default: 2025.03.3)
  JAVA_HOME      - Must point to JDK 11+ installation
"""

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

DOWNLOAD_URL = f"https://download.jetbrains.com/teamcity/TeamCity-{TC_VERSION}.tar.gz"


def find_plugin_zip():
    # Locate the plugin ZIP from PLUGIN_ZIP env var.
    env_zip = os.environ.get("PLUGIN_ZIP", "")
    if env_zip and os.path.isfile(env_zip):
        return os.path.abspath(env_zip)
    return None


def verify_java():
    # Validate that JAVA_HOME is set and points to a valid JDK installation.
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
    # Download TeamCity tar.gz to %TEMP%, using cache if already present.
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
    # Extract tar.gz to SERVER_DIR, stripping the top-level "TeamCity/" prefix.
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
    # Copy plugin ZIP into the server's data/plugins directory.
    plugins_dir = os.path.join(DATA_DIR, "plugins")
    os.makedirs(plugins_dir, exist_ok=True)
    dest = os.path.join(plugins_dir, "matlab-teamcity-plugin.zip")
    shutil.copy2(plugin_zip, dest)
    print(f"Plugin deployed: {dest}")


def start_server():
    # Start TeamCity server as a background process via catalina.bat.
    catalina_bat = os.path.join(SERVER_DIR, "bin", "catalina.bat")
    if not os.path.isfile(catalina_bat):
        print(f"ERROR: {catalina_bat} not found.")
        sys.exit(1)

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

    log_dir = os.path.join(SERVER_DIR, "logs")
    os.makedirs(log_dir, exist_ok=True)
    log_file = open(os.path.join(log_dir, "catalina-stdout.log"), "w")
    subprocess.Popen(
        [catalina_bat, "run"],
        cwd=os.path.join(SERVER_DIR, "bin"),
        env=env,
        stdout=log_file,
        stderr=log_file,
    )
    time.sleep(10)
    print("  Server process launched.")


def main():
    verify_java()

    plugin_zip = find_plugin_zip()
    if not plugin_zip:
        print("ERROR: Could not find plugin ZIP in matlab-plugin-build/target/.")
        print("  Run 'mvn package -DskipTests' first.")
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

    print("\nServer installed and started successfully.")


if __name__ == "__main__":
    main()
