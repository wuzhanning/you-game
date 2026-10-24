# Android TV 版

安装包：[YouGame-TV-debug.apk](releases/YouGame-TV-debug.apk)。Android 8.0 及以上，需要支持现代 Canvas、dialog 的系统 Android System WebView。复制 APK 到电视，通过电视文件管理器安装；也可通过已授权的 ADB 执行：

```sh
adb install -r android-tv/releases/YouGame-TV-debug.apk
```

从电视应用列表打开「星球碰碰乐」。内置完整游戏，离线运行，不需要本机 HTTP 服务。横屏全屏，包含五种模式；分数、棋盘和道具保存在电视本机，退出后继续。电视与电脑浏览器存档独立；卸载或清除应用数据会删除电视存档。

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
node planet-game/check.cjs
node --check planet-game/dist/game.js
node --check planet-game/dist/tv.js
python3 android-tv/build.py
```

脚本直接打包 `planet-game/dist`，输出至 `android-tv/releases/YouGame-TV-debug.apk`。当前为便于侧载的开发签名 APK，未作为 Google Play 正式发行包。首次生成的 `.debug.keystore` 仅保存在本机，请保留以便后续覆盖安装；更换签名后无法覆盖旧包，不要通过卸载解决而丢失存档。

## 验证范围

自动检查覆盖字形轮廓、合成、存档和遥控器状态切换。Android 35 ARM64 普通模拟器已验证安装启动、遥控器菜单、字形数字投放和合成、覆盖安装存档保留、暂停恢复，以及 TV 启动入口。未在实体 Android TV 或实体蓝牙手柄上完成测试；音效听感和不同电视 WebView 兼容性仍需真机确认。
