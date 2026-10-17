# Reference graphics

The Java implementation in this module is separate from FTB's implementation.
The current implementation includes the following original graphical resources, unchanged:

- `assets/ftbquests/textures`: [FTB Quests, 1.12](https://github.com/FTBTeam/FTB-Quests/tree/85b7652bd69f3bc4c03ba9ba24909ddea0605a53/src/main/resources/assets/ftbquests/textures), commit `85b7652bd69f3bc4c03ba9ba24909ddea0605a53`.
- `assets/ftblib/textures`: [FTB Library legacy, 1.12](https://github.com/FTBTeam/FTB-Library-legacy/tree/47586e9a1bb60e6229ce05d75b3d88ae82ef963a/src/main/resources/assets/ftblib/textures), commit `47586e9a1bb60e6229ce05d75b3d88ae82ef963a`.
- `assets/ftbquests/lang` and `assets/ftblib/lang`: original English and Russian language files from the same commits. Language filename capitalization is adapted for Minecraft 1.7.10.
- `assets/minecraft/textures/font/ascii.png` and `unicode_page_01.png`: the two font sheets with visible pixel differences from 1.7.10, copied from the same official 1.12.2 client. All other 222 font sheets and glyph widths already match visually.
- `assets/minecraft/textures/gui/presets/isles.png`: default chapter image from the same official Minecraft 1.12.2 client.
- `assets/sigmarixquests/textures/reference`: GUI sheets, toast sheet and default player skins extracted from the official Minecraft 1.12.2 client, SHA-1 `0f275bc1547d01fa5f56ba34bdc87d981ee12daf`. Filenames retain the original basename with directory separators replaced by underscores.

FTB graphics belong to their upstream authors; Minecraft graphics belong to Mojang/Microsoft.
These resources are not relicensed as this project's own work. The FTB Quests
project page states All Rights Reserved. Inclusion here does not assert a new
license or permission grant from the upstream authors.

[The resource manifest](docs/reference-assets.json) records SHA-256 hashes of every
included reference file. Resource identity does not establish full GUI pixel parity.

The GUI icon layout in `assets/sigmarixquests/gui_icon_layout.json` records the
FTBLib sprite rectangles observed in the reference 1.12.2 runtime. The port
builds this atlas from the attributed original icon PNGs at resource reload,
preserving resource-pack overrides and stable sampling when optional mods
change the Minecraft block atlas. It adds no Minecraft block textures.

The three toast sounds in `assets/sigmarixquests/sounds/reference/toast/` are
unchanged Minecraft 1.12 asset-index objects (`ui/toast/in`, `out`, and
`challenge_complete`). Mojang retains rights to these sounds. The reference
manifest records their source keys, object SHA-1s and included-file SHA-256s.
