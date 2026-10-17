# Version 0.26: Russian UI and external quest book

The saved pixel comparisons below document 0.25. Version 0.26 deliberately fills
missing Russian translations, including the native English Submit label, field
values, legacy tooltips, command/status messages and desktop controls. English
behavior is retained. Unknown registry IDs and typed text remain literal.

The separate book in `../../quest-packs/ru-six-mods-1.7.10` contains six chapters
and 180 quests (139 non-consuming item checks and 41 explicit manual checks).
Its resource pack contains 17 unchanged original game illustrations, including
Blood Magic altar screenshots I–V and Botania Lexica images. None of the six
chapter documents or their illustration pack is included in the production JAR.

Verification passed: 68 JUnit, 154 Forge integration and 43 Python tests (265 total).
The combined integrated-world run passed in 2m 12s; the final build and command
component checks also passed. See `build-verification.json`.

New tests cover separate-file load/save, moving quests between documents,
rollback after a later file fails, unsafe directories, independent flattened
profiles, all native/port Russian keys and formatting arguments, and the complete
book's dependency graph/network size. The illustrated Russian client preview
passed, including resource lookup, translated nested error messages and literal
user input. Client captures accompany the external book under `previews`.

---

# FTB Quests 1.12.2 standard-theme parity

Reference: FTB Quests 1202.9.0.15, FTBLib 5.4.7.2, Minecraft 1.12.2 / Forge
14.23.5.2860. Target: Minecraft 1.7.10 / Forge 10.13.4.1614, Java 8, official
Botania r1.8-249. Snapshot 0.25.0-SNAPSHOT.

## Current measured coverage

Saved source/port images use 854×480, GUI scale 2. Each report records its exact
rectangle and exclusions; the desktop editor instead uses its entire 835×761 frame.
Zero differing pixels in the recorded comparisons:

| Report | Coverage |
| --- | --- |
| `selector-pixel-comparison.json` | Entire default map, book/image/table/crate forms; item/fluid selector controls; map hover and quest-card bounds |
| `map-pixel-comparison.json` | Entire chapter expansion and quest-card bounds |
| `object-form-pixel-comparison.json` | 26 complete chapter/quest/task/reward property frames |
| `value-pixel-comparison.json` | Nine complete item/NBT/scalar/invalid-value frames |
| `auxiliary-pixel-comparison.json` | Ten complete table directory/editor, choice, emergency and notification frames |
| `device-form-pixel-comparison.json` | Eight complete device property frames |
| `desktop-pixel-comparison.json` | Complete native Swing text editor |
| `context-hud-pixel-comparison.json` | Complete context menu and active/completed pinned HUD |
| `toast-valid-items-pixel-comparison.json` | Seven complete toast frames and four valid-item grids |
| `world-screen-pixel-comparison.json` | Eleven complete task/progress screen foreground frames |
| `optional-energy-pixel-comparison.json` | Mana/EU forms and seven optional-energy world screen frames |
| `map-geometry-pixel-comparison.json` | Fractional node sizes/zoom, unrotated/rotated illustrations, four complete frames |
| `catalog-pixel-comparison.json` | Complete catalog: all 13 inventory items, labels, nine node shapes and background |
| `russian-pixel-comparison.json` | Complete Russian book form, valid-item grid, map, team selector, member/disabled/leader action menus, creation and palette hover |

Recompute a report using
`python tools/compare_gui_pixels.py docs/REPORT.json` (Pillow required).
`item-pixel-comparison.json` and `pixel-comparison.json` retain older static-item
crops and team/scalar widget-mask evidence. The entire catalog report supersedes
the old catalog animation/label exclusions. Reports overlap; their frame counts
must not be interpreted as distinct windows.

The English selector controls exclude animated fluid slots, version-dependent
vanilla inventory contents and caret phase. All other listed whole-frame records
have no excluded regions. Toast age, barrier/input animation phase, emergency
countdown, text caret and canonical world-screen camera are controlled equally
in both clients. World records measure the original TESR foreground under a fixed
orthographic front view, not arbitrary terrain/screenshots.

Reference Mana/EU captures use the original FTB integration task/TESR methods with
official 1.12 Botania and IC2 APIs available to the reference loader. Those APIs
are not target dependencies and are not bundled. Actual target transfers are
checked separately using official 1.7.10 Botania/IC2 mods.

## Current verification and boundaries

Build: 62 JUnit, 153 Forge integration and 43 Python migration tests pass (258).
The combined game run checks official Botania mana bursts, IC2 EnergyNet,
ProjectE wallet, CustomNPCs, BuildCraft 7.1.23 and NEI together, including native
form saves, desktop Save, stale-edit rejection, offline automation and restart.
The latest combined run passed after the locale-font and chapter-state corrections
(`quests-v25-packaged-optional-client.log`, 2m 17s). All three native toast OGG resources
were registered and read successfully; actual desktop Save and server restart passed.

Matching these frames does not prove every resolution, hover, scroll, search or
editor selection state. A remote dedicated server has not been run. Version-specific
vanilla content, nonempty Forge capabilities and 1.12-only advancement criteria
need explicit 1.7 mappings; arbitrary FTBLib theme/rich-page DSL is not a supported
input format. See `../COMPATIBILITY.md`. No claim of universal binary/data parity.

## Implementation and verification history

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

Optional Mana/EU forms and world screens match nine full frames. The complete
13-item catalog, including the animated barrier at fixed frame zero, all labels
and nine shapes, matches a full frame without exclusions. Russian locale
verification additionally covers nine entire frames. Classic panel font state
is independent of the locale font, Unicode shadows retain the native half-pixel
offset, and tooltip rendering restores the locale font. Team Settings uses the
native translation key; FTBLib button icons retain full brightness when disabled.
The complete Russian team selector, member/disabled/leader menus, creation form
and palette tooltip all match. Empty/optional-only and always-invisible chapters
now have native started/complete states; a regression test covers nested empties.

The packaged build passed its final combined client run after the team translation,
disabled-icon and palette-tooltip corrections (2m 17s). The saved reports contain
114 direct records, including 108 complete 854×480 records; reports overlap.
The artifact hash, test counts, runtime details and class-namespace audit are
recorded in `build-verification.json`. No preview or optional API classes are bundled.
