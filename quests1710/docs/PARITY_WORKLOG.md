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

- Build: 60 JUnit and 150 Forge integration tests, plus 43 Python migration tests.
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

All 26 base task/reward/chapter/quest property forms now match the complete
854×480 reference screenshot, with zero exclusions. Recompute with
`python tools/compare_gui_pixels.py docs/object-form-pixel-comparison.json`.
Energy input limits, command variables and 1.7 achievement criterion semantics
have dedicated backend regression coverage. Native editor actions, desktop
text editing and remaining interaction states are being completed.

The native Swing text editor now matches the complete 835×761 reference frame
(635,435 pixels, zero differences). Its Save button was also exercised through
an integrated server and the resulting quest text was verified in the server book.
Map editing now supports source-style placement/cancel, group moves, and batch
reward creation/deletion, with dependency cleanup validated before submission.

Nine complete scalar/item/NBT editor frames now match at 854×480: null and populated
NBT, item SNBT, plain item text, empty item, count, metadata, name, and an invalid
count with disabled Accept. Native 1.12 SNBT formatting is generated without
changing the underlying 1.7 NBT. Item text supports string registry IDs and legacy
numeric NBT. Nonempty ForgeCaps still require a compatibility mapping because
Forge 1.7 has no capability framework.

Ten complete auxiliary frames also match at 854×480: empty/populated reward-table
directory, weighted reward editor, choice dialog, emergency items, and reward
notifications. Countdown fixtures fix the same timer second; text fixtures hide
carets to fix blink phase. No pixel regions are excluded. Reports are
`value-pixel-comparison.json`, `auxiliary-pixel-comparison.json`, and
`desktop-pixel-comparison.json`. Device property forms and world rendering remain
under active comparison; this checkpoint does not claim complete state coverage.

Eight native device property forms now match full reference screenshots at
854×480: task/progress screens, barrier, collector, redstone/player/progress
detectors, and chest. The reference forms were obtained from actual native
server config messages, not a reconstruction. See `device-form-pixel-comparison.json`.
The missing required-redstone-level property has been added. Detector input now
reacts to power changes and persists the previous power, avoiding repeat
completion under a steady signal; the Forge threshold test also covers reset
under steady power and a subsequent above-threshold change. Protected screen
fields now use native read-only state for non-editors.

Checkpoint build passed: 60 JUnit, 150 Forge integration tests and 43 Python tests.
A new combined optional-mod client run is in progress after these changes.

Context-menu geometry, disabled/info entries and original FTBLib icons now match
one complete 854×480 frame. Active and completed pinned HUD also match full frames;
fluid units and formatted counters now follow native task progress strings.
See `context-hud-pixel-comparison.json`. Chapter up/down actions keep their menu
open and rebuild against a new server revision; a native reward-table edit action
is now available directly from random/choice/loot reward context menus.

Seven toast frames (custom, long-title, long-subtitle, chapter, task, quest,
reward) and four valid-items grids (0/1/5/20 entries) match full frames, without
exclusions. See `toast-valid-items-pixel-comparison.json`. Chapter completion uses
its native pink title and actual chapter name. Task notices preserve counts and
custom icons; reward notices preserve live table title references. Native toast
in/out/chapter-completion OGG files are attributed and hashed. The client fixture
checks all three registered sound resources.

Eleven world-screen foreground frames now match the native TESRs under the same
orthographic front view: check task empty/complete, energy 0/50/100%, item, input
mode, and chapter map normal/fullscreen/with icons/empty. Input animation phase is
fixed at frame zero in both fixtures; no image regions are excluded. The runtime
uses the animated block-atlas sprite rather than compressing its entire frame
strip. Energy texture boundaries and hidden progress numbers follow the original.
See `world-screen-pixel-comparison.json`. Chapter map values now include the
selected chapter directly rather than silently merging its descendants.

Server snapshots now send native integer display progress for all exposed quests,
not only pinned ones. The display calculation floors task percentages, preserves
positive progress at 1%, treats OR-task quests as 0/100%, and averages child chapter
groups. Dedicated backend tests cover these semantics. The independent completion
and task counter model remains authoritative.

Latest build passed 61 JUnit and 153 Forge integration tests; 43 Python migration
tests pass. The combined optional-mod client passed at commit 2fa64f1 (including
native desktop Save and restart). A fresh combined run is checking the subsequent
world-display, toast, sound, progress and context-action refinements. Remaining
visual work includes fractional map sizes/zoom and further interaction states;
this is still not a claim of complete parity.

The combined client passed again after world/toast/progress changes, including all
six optional integrations, native text/device saves, restart, and three registered
native toast OGG assets. Two failed attempts exposed eager TESR type verification
inside the development @Mod fixture; deferring canonical render fixtures fixed
that loader timing issue. The production JAR contains none of these fixture classes.

Fractional quest sizes (.125/1.3/2.1/.6) now match complete map frames at native zoom
16 and 20. Widget positions and widths are quantized separately, hit masks and
edges use the resulting centers, and icon/badge sizes retain fractional values.
The standard map was rechecked and still matches its full reference frame.
Chapter illustrations now use the native button size, integer center, float
rotation and per-vertex color; unrotated and 27-degree frames both match completely.
See `map-geometry-pixel-comparison.json` (four complete frames, zero exclusions).
The latest render changes are undergoing the final build/game checks.
