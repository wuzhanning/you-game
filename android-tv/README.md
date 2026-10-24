# Android TV 版

当前 APK 的启动入口是「YouGame 数字游乐场」，包含 `native-games/` 中六款非棋盘原生 Java / Canvas 街机游戏，面向 Android 9 与 2GB 内存电视。原生游戏不依赖 WebView；大厅保留「星球碰碰乐」按钮，该游戏继续使用下面说明的 WebView 方案。覆盖安装保留两类游戏各自的存档，包名和签名保持一致。

原生游戏：方向键选游戏、确认进入，游戏内返回 / 菜单打开暂停菜单。可继续存档、选择已解锁关卡、查看玩法、重玩、回大厅及开关音效。首次进入游戏先确认出发。切到后台会停止计时与绘制，恢复后确认继续。六款游戏的具体按键及关卡见 [原生游戏说明](../native-games/README.md)，设备验证范围见 [测试记录](../native-games/QA.md)。

## 原有星球游戏

安装包：[YouGame-TV-debug.apk](releases/YouGame-TV-debug.apk)。Android 8.0 及以上，需要支持 Canvas、dialog 的系统 Android System WebView；已适配小米电视内置的 WebView 66。复制 APK 到电视，通过电视文件管理器安装；也可通过已授权的 ADB 执行：

```sh
adb install -r android-tv/releases/YouGame-TV-debug.apk
```

从数字游乐场大厅打开「星球碰碰乐」。内置完整游戏，离线运行，不需要本机 HTTP 服务。横屏全屏，包含五种模式；分数、棋盘和道具保存在电视本机，退出后继续。电视与电脑浏览器存档独立；卸载或清除应用数据会删除电视存档。

电视遥控器和蓝牙手柄均可使用。游戏主界面优先显示遥控器提示，手柄快捷键见「玩法与图鉴」。手柄需先在电视系统的蓝牙设置中配对并连接。

| 输入 | 操作 |
| --- | --- |
| 遥控器左右 / 手柄方向键、左摇杆 | 移动投放位置 |
| 确认 / A | 投放；暂停时继续；炸弹状态确认目标 |
| 上下 / 菜单 / Select | 打开操作菜单 |
| 菜单内方向键、确认 | 选择和执行按钮，可切换模式、使用道具 |
| B / X | 炸弹 / 摇一摇 |
| Start | 暂停或继续 |
| 返回 | 关闭弹窗；游戏中暂停；已暂停时退出 |

## 构建

使用系统 SDK、Java 和 Python 标准库，无 Gradle 或在线依赖。需要 JDK 17+、Android SDK Platform 34、Build Tools 35.0.0；脚本默认使用 macOS Android Studio 自带 JDK 和 `~/Library/Android/sdk`，其他路径通过 `JAVA_HOME`、`ANDROID_HOME` 指定。

```sh
python3 native-games/check.py
node planet-game/check.cjs
node --check planet-game/dist/game.js
node --check planet-game/dist/tv.js
python3 android-tv/build.py
```

脚本编译 `android-tv/src` 和 `native-games/src` 并打包 `planet-game/dist` 和 `native-games/assets`，输出至 `android-tv/releases/YouGame-TV-debug.apk`。当前为便于侧载的开发签名 APK，未作为 Google Play 正式发行包。首次生成的 `.debug.keystore` 仅保存在本机，请保留以便后续覆盖安装；更换签名后无法覆盖旧包，不要通过卸载解决而丢失存档。

## 验证范围

自动检查覆盖字形轮廓、合成、存档和遥控器状态切换。Android 35 ARM64 普通模拟器已验证安装启动、遥控器菜单、字形数字投放和合成、覆盖安装存档保留、暂停恢复，以及 TV 启动入口。

2026-10-24 在小米 MiTV-ASTP0（Android 9 / WebView 66.0.3359.158）安装验证：修复旧 WebView 的脚本语法和网页接口兼容问题，确认横屏棋盘、字形字母模式、分数和遥控器提示正常显示，并发送确认键及手柄 A 键事件。该电视当时只检测到小米蓝牙遥控器，没有连接的游戏手柄；实体手柄的蓝牙连接、摇杆和音效听感尚未完成实测。随后 ADB 连接离线，未继续验证遥控器菜单完整流程和真机重启恢复。
