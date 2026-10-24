#!/usr/bin/env python3
"""Build an offline Android TV APK using the installed SDK and JDK, no Gradle downloads."""
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
SDK = Path(os.environ.get('ANDROID_HOME', Path.home() / 'Library/Android/sdk'))
JAVA = Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home'))
TOOLS = SDK / 'build-tools/35.0.0'
ANDROID = SDK / 'platforms/android-34/android.jar'
BUILD = ROOT / 'build'
OUT = ROOT / 'releases/YouGame-TV-debug.apk'
ENV = dict(os.environ, JAVA_HOME=str(JAVA), PATH=str(JAVA / 'bin') + os.pathsep + os.environ.get('PATH', ''))

def run(*args):
    subprocess.run([str(a) for a in args], check=True, env=ENV, cwd=ROOT)

for path in [JAVA / 'bin/javac', TOOLS / 'aapt', ANDROID]:
    if not path.exists():
        raise SystemExit(f'Missing prerequisite: {path}')
if BUILD.exists():
    shutil.rmtree(BUILD)  # Generated files only, never sources or signing keys.
(BUILD / 'classes').mkdir(parents=True)
(BUILD / 'dex').mkdir()
OUT.parent.mkdir(exist_ok=True)
run(TOOLS / 'aapt', 'package', '-f', '-M', ROOT / 'AndroidManifest.xml', '-S', ROOT / 'res', '-A', ROOT.parent / 'planet-game/dist', '-I', ANDROID, '-F', BUILD / 'base.apk')
run(JAVA / 'bin/javac', '--release', '8', '-g', '-encoding', 'UTF-8', '-classpath', ANDROID, '-d', BUILD / 'classes', *sorted((ROOT / 'src').rglob('*.java')))
run(TOOLS / 'd8', '--lib', ANDROID, '--min-api', '26', '--output', BUILD / 'dex', *sorted((BUILD / 'classes').rglob('*.class')))
with zipfile.ZipFile(BUILD / 'base.apk', 'a', zipfile.ZIP_DEFLATED) as apk:
    for dex in (BUILD / 'dex').glob('*.dex'):
        apk.write(dex, dex.name)
run(TOOLS / 'zipalign', '-f', '4', BUILD / 'base.apk', BUILD / 'aligned.apk')
key = ROOT / '.debug.keystore'
if not key.exists():
    run(JAVA / 'bin/keytool', '-genkeypair', '-keystore', key, '-storepass', 'android', '-keypass', 'android', '-alias', 'androiddebugkey', '-dname', 'CN=Android Debug,O=Android,C=US', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000')
run(TOOLS / 'apksigner', 'sign', '--ks', key, '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', OUT, BUILD / 'aligned.apk')
run(TOOLS / 'apksigner', 'verify', '--verbose', OUT)
print(f'Built: {OUT}')
