import tempfile
import unittest
from pathlib import Path
from import_ftb112 import SNBT, snbt, convert, convert_task, convert_reward, mapping_rules

class ImportTests(unittest.TestCase):
    def test_native_book_defaults_metadata_and_tick_precision_remain_editable(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'file.snbt').write_text('{title:"" tags:["book"] default_consume_items:true default_reward_team:true default_autoclaim_rewards:"no_toast" disable_toast:true emergency_items_cooldown:"21t"}')
            chapter = root / 'chapters' / '00000001'
            chapter.mkdir(parents=True)
            (chapter / 'chapter.snbt').write_text('{subtitle:["Chapter detail"] tags:["chapter"] disable_toast:true icon:"ftbquests:book"}')
            (chapter / '00000002.snbt').write_text('{tasks:[{uid:3 type:"item" item:"minecraft:stone" tags:["task"]}] rewards:[{uid:4 type:"xp" xp:7 tags:["reward"]}]}')
            book = convert(root)
            self.assertEqual(book['title'], '')
            self.assertEqual(book['defaultQuestShape'], 'circle')
            self.assertEqual(book['defaultAutoClaim'], 'no_toast')
            self.assertEqual(book['emergencyCooldownTicks'], 21)
            self.assertTrue(book['defaultConsumeItems'])
            self.assertTrue(book['defaultRewardTeam'])
            self.assertTrue(book['disableToast'])
            self.assertEqual(book['chapters'][0]['description'], 'Chapter detail')
            self.assertEqual(book['chapters'][0]['icon'], 'sigmarixquests:quest_book')
            self.assertTrue(book['chapters'][0]['disableToast'])
            quest = book['quests'][0]
            self.assertEqual(quest['title'], '')
            self.assertEqual(quest['rewardMode'], 'inherit')
            self.assertEqual(quest['tasks'][0]['consumeMode'], 'inherit')
            self.assertEqual(quest['tasks'][0]['tags'], ['task'])
            self.assertEqual(quest['rewards'][0]['mode'], 'inherit')
            self.assertEqual(quest['rewards'][0]['auto'], 'inherit')
            self.assertEqual(quest['rewards'][0]['tags'], ['reward'])

    def test_table_title_and_crate_item_name_are_independent(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'chapters' / '00000002').mkdir(parents=True)
            (root / 'chapters' / '00000002' / 'chapter.snbt').write_text('{title:"Chapter"}')
            (root / 'chapters' / '00000002' / '00000003.snbt').write_text('{tasks:[{type:"checkmark" uid:4}]}')
            (root / 'reward_tables').mkdir()
            (root / 'reward_tables' / '00000001.snbt').write_text('{title:"Table label" tags:["tier_one"] hide_tooltip:true use_title:true rewards:[{type:"xp" xp:10}] loot_crate:{string_id:"common" item_name:"Physical crate"}}')
            book = convert(root)
            settings = book['lootSettings']['ftb_00000001']
            self.assertEqual(settings['name'], 'Table label')
            self.assertEqual(settings['itemName'], 'Physical crate')
            self.assertEqual(settings['tags'], ['tier_one'])
            self.assertTrue(settings['hideTooltip'])
            self.assertTrue(settings['useTitle'])

    def test_silent_auto_claim_mode_is_preserved(self):
        self.assertEqual(convert_reward({"type": "xp", "xp": 12, "auto": "no_toast"})["auto"], "no_toast")

    def test_snbt_comments_suffixes_and_omitted_commas(self):
        value = SNBT('{ // comment\nname:"Привет"\nlist:[I;1,2,3]\ntag:{on:1b}\n}').parse()
        self.assertEqual(value['name'], 'Привет')
        self.assertEqual(value['list'], [1, 2, 3])
        self.assertIn('1b', snbt(value['tag']))

    def test_parser_rejects_duplicate_keys_and_excess_depth(self):
        for text in ('{a:1 a:2}', '[' * 66 + '0' + ']' * 66, '{a:"broken}'):
            with self.assertRaises(ValueError):
                SNBT(text).parse()

    def test_imported_repeat_quests_enable_automatic_reset(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'chapters' / '00000001').mkdir(parents=True)
            (root / 'chapters' / '00000001' / 'chapter.snbt').write_text('{title:"Repeat"}')
            (root / 'chapters' / '00000001' / '00000002.snbt').write_text('{title:"Loop" can_repeat:true tasks:[{type:"checkmark" uid:3}] rewards:[]}')
            book = convert(root)
            self.assertTrue(book['quests'][0]['repeatable'])
            self.assertTrue(book['quests'][0]['autoRepeat'])

    def test_custom_counter_is_preserved_for_detectors_and_native_callbacks(self):
        task = convert_task({'type': 'ftbquests:custom', 'uid': 7}, 0)
        self.assertEqual(task['type'], 'custom')
        self.assertEqual(task['target'], '')
        self.assertEqual(task['count'], 1)
        self.assertFalse(task['manual'])
        self.assertTrue(convert_task({'type': 'custom', 'enable_button': True}, 0)['manual'])

    def test_native_command_variables_are_preserved(self):
        command = '/say @p @team @quest @chapter @x @y @z'
        self.assertEqual(convert_reward({'type': 'command', 'command': command})['target'], command)
        for command in ('give @s diamond', 'give @p[type=Player] diamond'):
            with self.assertRaises(ValueError):
                convert_reward({'type': 'command', 'command': command})

    def test_unsupported_types_fail_instead_of_becoming_checkmarks(self):
        with self.assertRaises(ValueError):
            convert_task({'type': 'ftbquests:missing_integration'}, 0)
        with self.assertRaises(ValueError):
            convert_reward({'type': 'ftbquests:command', 'command': 'give @s diamond'})

    def test_directory_and_dependency_conversion(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            chapter = root / 'chapters' / '00000001'
            chapter.mkdir(parents=True)
            (chapter / 'chapter.snbt').write_text('{title:"Chapter"}')
            (chapter / '00000002.snbt').write_text('{title:"Start" tasks:[{type:"checkmark" uid:3}] rewards:[]}')
            (chapter / '00000004.snbt').write_text('{title:"Next" dependencies:[I;2] tasks:[{type:"dimension" dim:-1}] rewards:[]}')
            book = convert(root)
            self.assertEqual(book['quests'][1]['dependencies'], ['ftb_00000002'])
            self.assertEqual(book['quests'][1]['tasks'][0]['dimension'], -1)
            self.assertEqual(book['quests'][0]['tasks'][0]['id'], 'ftb_00000003')

    def test_failed_conversion_does_not_silently_drop_quests(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            chapter = root / 'chapters' / '01'
            chapter.mkdir(parents=True)
            (chapter / 'chapter.snbt').write_text('{title:"Chapter"}')
            (chapter / '02.snbt').write_text('{tasks:[{type:"unknown"}]}')
            with self.assertRaisesRegex(ValueError, 'manual conversion'):
                convert(root)


class VersionMappingTests(unittest.TestCase):
    def test_explicit_identity_maps_preserve_mod_mobs_biomes_and_classes(self):
        rules = mapping_rules({'entities': {'pack:sentinel': 'PackSentinel'}, 'biomes': {'pack:grove': '82'}, 'classes': {'new.Tile': 'old.Tile'}, 'tileEntities': {'pack:new': 'OldTile'}, 'stats': {'stat.new': 'stat.old'}})
        self.assertEqual(convert_task({'type': 'kill', 'entity': 'pack:sentinel'}, 0, mappings=rules)['target'], 'PackSentinel')
        self.assertEqual(convert_task({'type': 'biome', 'biome': 'pack:grove'}, 0, mappings=rules)['target'], '82')
        self.assertEqual(convert_task({'type': 'stat', 'stat': 'stat.new'}, 0, mappings=rules)['target'], 'stat.old')
        for kind, source, expected in [('entity_class', 'new.Tile', 'old.Tile'), ('block_entity_id', 'pack:new', 'OldTile')]:
            self.assertEqual(convert_task({'type': 'observation', 'match_type': kind, 'match': source}, 0, mappings=rules)['target'], expected)

    def test_block_states_require_exact_explicit_metadata_mapping(self):
        raw = {'type': 'interaction', 'match': 'minecraft:log', 'properties': 'variant = spruce, axis = x'}
        with self.assertRaises(ValueError):
            convert_task(raw, 0)
        rules = mapping_rules({'blocks': {'minecraft:log[axis=x,variant=spruce]': {'id': 'minecraft:log', 'meta': 5}}})
        task = convert_task(raw, 0, mappings=rules)
        self.assertEqual((task['target'], task['meta'], task['ignoreDamage']), ('minecraft:log', 5, False))
        raw['properties'] = {'axis': 'x', 'variant': 'spruce'}
        self.assertEqual(convert_task(raw, 0, mappings=rules)['meta'], 5)
        raw['properties'] = 'axis=y,variant=spruce'
        with self.assertRaises(ValueError):
            convert_task(raw, 0, mappings=rules)

    def test_achievement_maps_distinguish_whole_advancement_from_criterion(self):
        rules = mapping_rules({'advancements': {'pack:trial': 'achievement.openInventory', 'pack:trial#ore': 'achievement.mineWood'}})
        task = convert_task({'type': 'advancement', 'advancement': 'pack:trial'}, 0, mappings=rules)
        self.assertEqual((task['type'], task['target']), ('achievement', 'achievement.openInventory'))
        reward = convert_reward({'type': 'choice', 'rewards': [{'type': 'advancement', 'advancement': 'pack:trial', 'criterion': 'ore'}]}, mappings=rules)
        self.assertEqual(reward['options'][0]['target'], 'achievement.mineWood')
        with self.assertRaises(ValueError):
            convert_task({'type': 'advancement', 'advancement': 'pack:trial', 'criterion': 'unmapped'}, 0, mappings=rules)

    def test_mapping_schema_rejects_unknown_sections_bad_types_and_metadata(self):
        for bad in [[], {'typo': {}}, {'entities': {'x': 3}}, {'blocks': {'x': {'id': 'minecraft:stone', 'meta': 16}}}, {'blocks': {'x': {'id': 'minecraft:stone', 'meta': True}}}]:
            with self.assertRaises(ValueError):
                mapping_rules(bad)
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); chapter = root / 'chapters' / '01'; chapter.mkdir(parents=True)
            (chapter / 'chapter.snbt').write_text('{title:"Map" icon:{id:"pack:icon" Damage:2}}')
            (chapter / '02.snbt').write_text('{tasks:[{type:"kill" entity:"pack:new"}] rewards:[{type:"advancement" advancement:"pack:trial"}]}')
            book = convert(root, {'items': {'pack:icon': {'id': 'minecraft:diamond', 'meta': 3}}, 'entities': {'pack:new': 'OldMob'}, 'advancements': {'pack:trial': 'achievement.mineWood'}})
            self.assertEqual((book['chapters'][0]['icon'], book['chapters'][0]['iconMeta']), ('minecraft:diamond', 3))
            self.assertEqual(book['quests'][0]['tasks'][0]['target'], 'OldMob')
            self.assertEqual(book['quests'][0]['rewards'][0]['target'], 'achievement.mineWood')

    def test_item_and_fluid_maps_apply_before_alternative_filters_and_nested_rewards(self):
        rules = mapping_rules({'items': {'botania:manaresource': {'id': 'Botania:manaResource', 'meta': 2}, 'pack:other': {'id': 'Botania:manaResource', 'meta': 3}}, 'fluids': {'pack:new': 'oldfluid'}})
        task = convert_task({'type': 'item', 'items': [{'id': 'botania:manaresource'}, {'id': 'pack:other'}], 'icon': {'id': 'botania:manaresource'}}, 0, mappings=rules)
        self.assertEqual(task['icon'], 'Botania:manaResource')
        self.assertEqual(task['iconMeta'], 2)
        self.assertEqual([child['meta'] for child in task['filter']['children']], [2, 3])
        reward = convert_reward({'type': 'choice', 'rewards': [{'type': 'item', 'item': {'id': 'botania:manaresource', 'Count': 4}}]}, mappings=rules)
        self.assertEqual((reward['options'][0]['item'], reward['options'][0]['meta'], reward['options'][0]['count']), ('Botania:manaResource', 2, 4))
        self.assertEqual(convert_task({'type': 'fluid', 'fluid': 'pack:new'}, 0, mappings=rules)['target'], 'oldfluid')
        for bad in [{'items': {'x': {'id': 'y', 'meta': -1}}}, {'items': {'x': {'id': 'y', 'meta': True}}}]:
            with self.assertRaises(ValueError):
                mapping_rules(bad)


if __name__ == '__main__':
    unittest.main()
