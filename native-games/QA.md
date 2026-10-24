# Android TV 验证记录

版本：3.0（versionCode 3），2026-10-24。这是六款全部改为非棋盘动作玩法后的版本；不沿用旧棋盘原型的测试结论。

## 规则与构建

- `python3 native-games/check.py`：19,191 次断言通过，覆盖六玩法、多难度连续模拟、碰撞与动作窗口、连锁、切边、AI 接触与绕柱、障碍物外生成、二段跳、存档逐字节恢复和 CRC 损坏拒绝、对象池上限。
- 跑酷、企鹅使用操作序列分别验证第 1、10、30、60 关存在可完成路线。它验证规则可行性，不代替儿童对难度的体验测试。
- 既有 `node planet-game/check.cjs`、`node --check planet-game/dist/game.js`、`node --check planet-game/dist/tv.js` 通过。
- `python3 android-tv/build.py` 通过；APK 使用原有密钥，v2 / v3 签名验证通过。APK 约 136KB，包含原 H5 资源与约 55KB 的原生短音效。

## 设备与操作

真机：小米 MiTV-ASTP0 / mulan，Android 9，1920×1080、density 320；`/proc/meminfo` 的 MemTotal 为 2,058,020KB。经用户授权，通过 Wi-Fi ADB 使用 `install -r` 覆盖安装，包管理器读回 versionCode 3、versionName 3.0。

模拟器：Android 35 ARM64，配置 2048MB 内存，覆盖 1080p / density 320 的电视布局。六场景均检查过截图与方向键、确认、返回操作；实际 Android 9 电视另外验证启动、确认出发、跑酷换道与跳跃、企鹅跳跃与二段跳、自由移动、冲刺、暂停和后台恢复。操作由 ADB 注入标准按键，不能替代实体遥控器长按重复速率和实际音响听感的验证。

新旧两类原生玩法使用不同 SharedPreferences，覆盖安装不删除原棋盘实验存档；原星球游戏 WebView 目录及包名、签名保持不变。模拟器验证了原星球入口及返回大厅。新街机版六模式局面在纯 Java 检查中逐字节保存恢复；电视确认了后台回来后保持暂停。

## 真机性能：数字疾跑

流程：启动跑酷、换到右跑道；重置 gfxinfo 后，约 10 秒连续绘制，采集 `dumpsys gfxinfo` / `framestats` / `meminfo`。使用 Choreographer，在 60Hz 电视上隔帧绘制；这是一次短采样。

| 指标 | 结果 |
| --- | --- |
| 本轮绘制帧数 | 305 |
| framestats 有效样本 | 120（系统保留的尾部窗口） |
| 相邻 IntendedVsync 间隔中位数 | 33.333ms |
| 间隔 P95 | 33.334ms，约 30fps |
| IntendedVsync → FrameCompleted 中位 / P95 | 19.797 / 25.451ms |
| DrawStart → SyncQueued 中位 / P95 | 1.629 / 3.202ms |
| 进程 TOTAL PSS | 34,076KB，约 33.3MiB |
| WebViews / Activities | 0 / 1 |
| 系统 Janky frames（60Hz 统计） | 261 / 305，85.57% |
| Missed Vsync / Slow UI thread | 0 / 3 |

稳定的 33.3ms 帧间隔支持本轮达到了 30fps 目标。系统按屏幕刷新周期统计的超时比例仍然高，不能写成“零卡顿”或“稳定 60fps”；这些数据也不证明六种场景都可以长期保持相同表现。未进行长时热稳定性、全关卡人工通关、蓝牙手柄和电视系统后台压力测试。

## 证据位置

本次会话原始报告与截图保存在本机 `/tmp/yougame-native-qa/`（临时目录，系统可能清理）：

- `tv/arcade-gfx-paced.txt`、`tv/arcade-framestats-paced.txt`、`tv/arcade-memory-paced.txt`
- `tv/arcade-runner-paced.png`、`tv/arcade-penguin.png`、`tv/arcade-arena.png`
- 模拟器的 `arcade-blaster.png`、`arcade-runner.png`、`arcade-penguin.png`、`arcade-1.png`、`arcade-3.png`、`arcade-4.png`

项目中的 `check.py` 与 `GameCheck.java` 可重复运行，不依赖这些临时报告。
