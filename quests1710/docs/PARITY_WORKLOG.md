# Reference parity work in progress

Target: FTB Quests 1202.9.0.15, FTBLib 5.4.7.2, Minecraft 1.12.2,
standard theme, 854×480, GUI scale 2. Port runtime: Forge 1.7.10,
official Botania r1.8-249. This ledger is not a claim of complete parity.

## Measured

- All 12 static inventory item/block renders match all 4096 pixels in each
  64×64 crop, including the background. See item-pixel-comparison.json.
- Native create-team frame/palette and scalar editor widget masks matched the
  original runtime in the preceding comparison; see pixel-comparison.json.
- Original quest card with two tasks, two rewards and editor add buttons (remaining pixel differences under investigation):
  logical bounds (113,69), 201×101; text block height 33, text top 64.
- Item selector frame, buttons and scrollbar: 49,032 pixels, zero differences.
- Fluid selector controls: 92,432 pixels, zero differences. Animated vanilla
  fluids excluded. See selector-pixel-comparison.json and paired screenshots.

- Complete default book, chapter image, reward table and reward-table/crate/drop
  property windows each match all 409,920 pixels. See selector-pixel-comparison.json.
- Recompute all saved selector/form results with
  `python tools/compare_gui_pixels.py` (Pillow required).
- Main map fixture: 2 chapters, 5 quests, 3 dependency lines. Latest completed
  comparison matches all 409,920 pixels, with no exclusions. Single-chapter hover
  panel also matches. Expanded parent/child chapters match the full screen. The complete editor
  quest card matches all 81,204 pixels in its window, without exclusions.

## Implemented, require further visual coverage

- Team selector, team menu, roles, requests, ownership transfer, settings,
  player lists, native confirmation dialogs, scalar editor.
- Task/reward compact grid, add buttons, dependency context list, source icons,
  numeric badges, default task icon animation.
- Settings context menu, progress reset/complete, exports, reward table directory.
- Reward toasts, collection notification screen, emergency item timer, pinned HUD.
- Barrier interpolation and atlas-backed inventory animation.
- Typed book/chapter/quest/task/reward property forms, transactional list editors,
  original type menus, item stack and fluid selectors, weighted reward editor.
- Atomic device configuration after closing the GUI, native object selectors,
  source task/progress screen layout, world screen body render-pass correction.
- Operator task/reward scoped progress actions and inherited book defaults.
- Actual item quantities in reward notifications, grouping and descending sort.
- Original timer notation with tick precision and legacy seconds compatibility.

- Native crate opener/storage activation uses status/chat; advanced inventory
  extensions remain accessible by sneaking. Chest uses native team/protection form.
- Tick-precision timers, item reward range 8192 + 8192, no-toast auto-claim,
  quest/chapter/book completion notices and official Minecraft font corrections.
- Chapter image edit/move/delete and server-validated click actions.
- Common 1.7 block metadata to native block-state property bridge for observation
  matching and device skins. Unsupported 1.12 block states still need mapping.
- Named choice/random reward tables remain live references after table edits;
  native table properties and crate presets, creature drops, delete/copy-ID actions.
- Native custom reward Forge event without a mandatory named callback.
- Ctrl multi-selection, select all/deselect, fractional keyboard movement,
  validated batch dependency edits, chapter shortcuts and double-Shift search.

## Runtime verification (current checkpoint)

- Build: 58 JUnit and 147 Forge integration tests, plus 42 Python migration tests.
  Current backend checks passed; the final small render corrections are being measured.
- Full vanilla client/integrated-world fixture passed with native property form
  save, native device accept/close/save and restart persistence.
- Combined optional-mod fixture passed with official Botania r1.8-249, IC2,
  ProjectE, CustomNPCs, BuildCraft 7.1.23 and NEI. The combined optional-mod fixture passed again after the native forms,
  block-state bridge, completion notices, reward tables and stable icon atlas.
  A remote icon warning found in that run has been fixed; final runtime
  coverage is being extended alongside the remaining forms. No claim of complete window/state coverage.

## Remaining work before declaring the requested full copy complete

- Finish and measure object editing and all object context actions, type creation,
  item/fluid/NBT selectors, reward-table editor, and choice dialog.
- Measure device config/task/progress screens against the reference world; measure
  the chest configuration and advanced inventory extension windows. Primary device config has been replaced.
- Compare main-map chapter panels, hover, search, scrolling, backgrounds, tooltip,
  pinned HUD, toasts, support confirmation and all editor state combinations.
- Run the complete optional-mod game suite after the final UI changes.

The existing backend extensions remain available, but do not substitute for
matching the original windows. Static texture identity alone is insufficient.

The 26 native base task/reward/chapter/quest property forms have now been
captured from the original runtime. Their comparison is in progress; this
checkpoint does not claim that all 26 forms already match.
