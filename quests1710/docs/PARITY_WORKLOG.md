# Reference parity work in progress

Target: FTB Quests 1202.9.0.15, FTBLib 5.4.7.2, Minecraft 1.12.2,
standard theme, 854×480, GUI scale 2. Port runtime: Forge 1.7.10,
official Botania r1.8-249. This ledger is not a claim of complete parity.

## Measured

- All 12 static inventory item/block renders match all 4096 pixels in each
  64×64 crop, including the background. See item-pixel-comparison.json.
- Native create-team frame/palette and scalar editor widget masks matched the
  original runtime in the preceding comparison; see pixel-comparison.json.
- Original quest card with two tasks, two rewards and editor add buttons:
  logical bounds (113,69), 201×101; text block height 33, text top 64.

## Implemented, require further visual coverage

- Team selector, team menu, roles, requests, ownership transfer, settings,
  player lists, native confirmation dialogs, scalar editor.
- Task/reward compact grid, add buttons, dependency context list, source icons,
  numeric badges, default task icon animation.
- Settings context menu, progress reset/complete, exports, reward table directory.
- Reward toasts, collection notification screen, emergency item timer, pinned HUD.
- Barrier interpolation and atlas-backed inventory animation.

## Remaining work before declaring the requested full copy complete

- Finish and measure object editing and all object context actions, type creation,
  item/fluid/NBT selectors, reward-table editor, and choice dialog.
- Replace primary device configuration with original config/selectors and check
  task/progress screens, crate storage/opener and chest windows in the game.
- Compare main-map chapter panels, hover, search, scrolling, backgrounds, tooltip,
  pinned HUD, toasts, support confirmation and all editor state combinations.
- Run the complete optional-mod game suite after the final UI changes.

The existing backend extensions remain available, but do not substitute for
matching the original windows. Static texture identity alone is insufficient.
