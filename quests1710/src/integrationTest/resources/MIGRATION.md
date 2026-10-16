`imported-quests.json` was produced by tools/import_ftb112.py from a synthetic FTB
SNBT directory containing a manual checkmark, a fluid/NBT task, task-started
reference, weighted random table, personal/automatic rewards and styled nodes.
The Forge integration test loads it through the production QuestService validator.

`migrated-progress.dat` was produced by tools/migrate_progress.py and nbt_codec.py:
member UUID 11111111-1111-1111-1111-111111111111, quest ftb_00000002,
item task ftb_00000003 at 4/4, personal reward ftb_00000004 claimed.
This is generated test data, not a player save. Forge reads the actual compressed
NBT file and checks its native quest state and UUID reward ledger.
