# External chapter files (0.26)

A book may set `"chapterDirectory": "chapters"`. The directory is relative to the
book JSON, including when using profiles. Only a single local directory name
containing letters, digits, underscores or hyphens is allowed.

Each `*.json` fragment contains `format`, `chapters`, and `quests`. Other top-level
fields are rejected; keep book defaults, themes and loot tables in `quests.json`.
Every quest in a fragment must belong to a chapter declared by that fragment.
References between files are validated after all files load. Files load in name
order. Since 0.27, the combined UTF-8 source limit is 16 MiB, with at most 64 fragments.
Single-file books have the same 16 MiB limit. Saves and snapshots in both
directions use 30,000-byte chunks, bounded to 32 MiB uncompressed / 16 MiB
compressed per assembled message. Both endpoints must run the same mod version.
Since 0.28, initial sync and forced refresh send a full snapshot; subsequent
updates use per-viewer field patches, including progress. Editor saves send book
patches when the editor baseline revision matches. Quests and chapters use stable
IDs, including separate order lists. Nested arrays are replaced as field values.
Stale saves are rejected before patch application; a missing client baseline
triggers full resynchronization. A full transfer is also used when the patch
would be larger or the bounded server cache has evicted that viewer.

The server validates the whole book and network envelope before installing it.
Editor saves partition the current chapters/quests back into their original
files. Moving a quest changes its destination to the file owning its new chapter.
New chapters without a file owner remain in the root book. Deleted fragments
remain empty documents. Each changed document receives a `.bak`, staged replacement
and rollback on an I/O failure. Individual replacements are atomic where supported;
a crash between multiple file replacements still requires checking backups.

Profiles created from a fragmented book are flattened into their own JSON and
clear `chapterDirectory`, so they remain independent of the original chapter files.
Ordinary single-file books keep their existing behavior.

The six-mod Russian book lives in `../../quest-packs/ru-six-mods-1.7.10` and is not
included as a default book or a resource in the production JAR. Its separate
resource pack contains unchanged, attributed original game illustrations.
