# Project workflow

- GitHub repository: git@github.com:wuzhanning/you-game.git.
- The user authorizes automatic Git commits and pushes after each completed project change. Run the relevant checks first, then commit and push to origin. No repeated confirmation is needed.
- Preserve unrelated work, never force-push, and report authentication or remote conflicts rather than claiming synchronization succeeded.
- Game source is in planet-game/dist. Run `node planet-game/check.cjs` and `node --check planet-game/dist/game.js` before pushing game changes.
- Refresh planet-game/星球碰碰乐-H5.zip from the files in planet-game/dist when game assets change.
