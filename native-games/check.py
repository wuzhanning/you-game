#!/usr/bin/env python3
"""Run deterministic rules checks with the JDK only; no Android runtime needed."""
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent
JAVA = Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home')) / 'bin'
with tempfile.TemporaryDirectory(prefix='yougame-check-') as output:
    subprocess.run([str(JAVA / 'javac'), '--release', '11', '-encoding', 'UTF-8', '-d', output,
                    str(ROOT / 'src/com/wuzhanning/yougame/nativegames/Game.java'),
                    str(ROOT / 'test/com/wuzhanning/yougame/nativegames/GameCheck.java')], check=True)
    subprocess.run([str(JAVA / 'java'), '-cp', output, 'com.wuzhanning.yougame.nativegames.GameCheck'], check=True)
