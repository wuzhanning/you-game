# YouGame · 电视数字游乐场

Android TV 应用包含六款非棋盘原生 Java / Canvas 动作游戏，并保留星球碰碰乐入口。目标 Android 9、2GB 内存电视，支持遥控器、离线游玩、独立存档与关卡解锁。

安装包：[YouGame-TV-debug.apk](android-tv/releases/YouGame-TV-debug.apk) · [电视安装说明](android-tv/README.md) · [原生游戏与扩展目录](native-games/README.md)。

## 星球碰碰乐 H5

适合儿童的 H5 合成游戏，可选择星球、数字、字母以及两种非球形字形模式，支持炸弹、摇一摇、独立存档，以及触屏、鼠标、键盘和标准映射手柄。

直接打开 `planet-game/dist/index.html`，或运行：

```sh
python3 -m http.server 5173 --directory planet-game/dist
```

访问 http://localhost:5173 。玩法和手柄说明见 [游戏说明](planet-game/README.md)。

## 验证

```sh
python3 native-games/check.py
node planet-game/check.cjs
node --check planet-game/dist/game.js
node --check planet-game/dist/tv.js
python3 android-tv/build.py
```

后续游戏修改检查通过后自动提交并推送到本仓库。完整 H5 文件包位于 `planet-game/星球碰碰乐-H5.zip`。

Android TV 离线安装版：[APK](android-tv/releases/YouGame-TV-debug.apk) · [安装、遥控器和构建说明](android-tv/README.md)。
