# 星球碰碰乐 · You Game

适合儿童的 H5 星球合成游戏，支持触屏、鼠标、键盘和标准映射手柄。

直接打开 `planet-game/dist/index.html`，或运行：

```sh
python3 -m http.server 5173 --directory planet-game/dist
```

访问 http://localhost:5173 。玩法和手柄说明见 [游戏说明](planet-game/README.md)。

## 验证

```sh
node planet-game/check.cjs
node --check planet-game/dist/game.js
```

后续游戏修改检查通过后自动提交并推送到本仓库。完整 H5 文件包位于 `planet-game/星球碰碰乐-H5.zip`。
