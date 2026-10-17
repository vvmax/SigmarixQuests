import tempfile
import unittest
import uuid
from pathlib import Path
from nbt_codec import IntTag, FloatTag, TypedArray, read_nbt, write_nbt
from import_ftb112 import SNBT, snbt, convert, convert_task, convert_reward, seconds, weights, convert_map_image, item, click_action
from migrate_progress import migrate

class MigrationTests(unittest.TestCase):
    def test_custom_click_protocols_survive_import_and_reject_malformed_targets(self):
        for value in ('command:/help', 'custom:pack:open_panel', 'guide:botania:lexicon', 'guide:', 'https://example.org/page', 'curseforgepages:pack:guide', 'othermod:panel'):
            self.assertEqual(value, click_action(value))
        for value in ('command:', 'command:/help\n/op player', 'https://user:pass@example.org', 'custom:bad', 'curseforgepages:../secret'):
            with self.assertRaises(ValueError): click_action(value)

    def test_external_guide_reference_is_preserved_without_fetching_guide_content(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); chapter = root / 'chapters' / '01'; chapter.mkdir(parents=True)
            (chapter / 'chapter.snbt').write_text('{}'); (chapter / '02.snbt').write_text('{guide_page:"botania:lexicon" tasks:[{type:"checkmark"}]}')
            self.assertEqual('botania:lexicon', convert(root)['quests'][0]['guidePage'])

    def test_task_and_reward_labels_icons_and_task_toast_flags_survive_import(self):
        icon = {'id': 'ftbquests:custom_icon', 'tag': {'icon': 'minecraft:textures/items/diamond.png'}}
        raw = {'type': 'checkmark', 'title': 'Named task', 'description': ['First', 'Second'], 'icon': icon, 'disable_toast': True}
        converted = convert_task(raw, 0)
        self.assertEqual('Named task', converted['title'])
        self.assertEqual('First\nSecond', converted['description'])
        self.assertTrue(converted['disableToast'])
        self.assertEqual('sigmarixquests:custom_icon', converted['icon'])
        reward = convert_reward({'type': 'item', 'item': 'minecraft:diamond', 'title': 'Prize', 'icon': icon})
        self.assertEqual('Prize', reward['title'])
        self.assertEqual(converted['iconNbt'], reward['iconNbt'])

    def test_native_item_aliases_preserve_icon_nbt_and_leave_external_items_alone(self):
        self.assertEqual('sigmarixquests:quest_book', item('ftbquests:book')['item'])
        self.assertEqual('sigmarixquests:task_screen', item('ftbquests:screen')['item'])
        self.assertEqual('sigmarixquests:redstone_detector', item('ftbquests:detector')['item'])
        self.assertEqual('sigmarixquests:player_detector', item({'id': 'ftbquests:detector', 'Damage': 1})['item'])
        value = item({'id': 'ftbquests:custom_icon', 'Count': 2, 'tag': {'icon': 'minecraft:textures/items/diamond.png'}})
        self.assertEqual('sigmarixquests:custom_icon', value['item'])
        self.assertEqual(2, value['count'])
        self.assertIn('minecraft:textures/items/diamond.png', value['nbt'])
        self.assertEqual('Botania:manaResource', item('Botania:manaResource')['item'])
        self.assertEqual('sigmarixquests:loot_crate', item('ftbquests:lootcrate')['item'])

    def test_map_illustrations_preserve_geometry_hover_and_links(self):
        image = convert_map_image({'image': 'minecraft:textures/blocks/stone.png', 'x': 2, 'y': -1, 'width': 3, 'height': 2, 'rotation': 45, 'hover': ['Image'], 'click': 'https://example.org/guide'})
        self.assertEqual(image['rotation'], 45); self.assertEqual(image['hover'], ['Image']); self.assertEqual(image['uri'], 'https://example.org/guide')
        self.assertEqual('https://example.org/image.png', convert_map_image({'image': 'https://example.org/image.png'})['resource'])
        with self.assertRaises(ValueError): convert_map_image({'image': 'file:///tmp/image.png'})
    def test_emergency_durations_and_creature_weights(self):
        self.assertEqual(seconds('5m'), 300); self.assertEqual(seconds('1m20t'), 61); self.assertEqual(seconds('21t'), 2)
        with self.assertRaises(ValueError): seconds('tomorrow')
        self.assertEqual(weights({'#monster': 12, 'passive': 2}), {'monster': 12, 'passive': 2, 'boss': 0})
    def test_ray_matchers_keep_tile_identity_and_nbt(self):
        result = convert_task({'type': 'interaction', 'match_type': 'block_entity_id', 'match': 'Furnace', 'nbt': {'BurnTime': IntTag(7, 's')}}, 0)
        self.assertEqual(result['matchType'], 'block_entity_id'); self.assertEqual(result['nbtMode'], 'subset'); self.assertFalse(result['entity'])
        self.assertIn('7s', result['nbt'])
    def test_npc_and_native_marker_integrations(self):
        task = convert_task({'type': 'npc_quest', 'npc_quest': 17, 'check_active': True}, 0)
        self.assertEqual(task['target'], '17'); self.assertTrue(task['checkActive'])
        task = convert_task({'type': 'npc_faction', 'npc_faction': 3, 'required_points': -5, 'point_mode': '<='}, 0)
        self.assertEqual(task['points'], -5); self.assertEqual(task['comparison'], 'at_most')
        reward = convert_reward({'type': 'npc_faction', 'npc_faction': 3, 'points': 200, 'add': False})
        self.assertEqual(reward['points'], 200); self.assertFalse(reward['add'])
        mail = convert_reward({'type': 'npc_mail', 'mail': {'Subject': 'Test', 'Sender': 'Quests', 'Message': {'pages': ['Hello']}}})
        self.assertEqual(SNBT(mail['nbt']).parse()['Message']['pages'], ['Hello'])
        self.assertEqual(convert_task({'type': 'gamestage', 'stage': 'pack:bronze'}, 0)['target'], 'pack:bronze')
        self.assertTrue(convert_reward({'type': 'gamestage', 'stage': 'pack:bronze', 'remove': True})['remove'])
        self.assertEqual(convert_task({'type': 'reskillable', 'skill': 'reskillable:mining', 'skill_level': 12}, 0)['count'], 12)
    def test_binary_nbt_roundtrip_preserves_types_arrays_and_java_unicode(self):
        value = {'text': 'Привет\0🐲', 'byte': IntTag(-2, 'b'), 'short': IntTag(500, 's'), 'long': IntTag(2**48, 'L'), 'float': FloatTag(1.25, 'f'), 'intArray': TypedArray([1, 2], 'I'), 'byteArray': TypedArray([-1, 2], 'B'), 'longArray': TypedArray([2**40], 'L'), 'list': [{'a': 'b'}]}
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'test.nbt'; write_nbt(path, value); restored = read_nbt(path)
            self.assertEqual(value, restored); self.assertEqual(restored['long'].suffix, 'L'); self.assertEqual(restored['byteArray'].kind, 'B')
            with self.assertRaises(FileExistsError):
                write_nbt(path, value)
    def test_binary_reader_rejects_truncated_and_excess_depth(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'bad.nbt'; path.write_bytes(b'\x0a\0\0\x03\0\x01a')
            with self.assertRaises(ValueError):
                read_nbt(path)
            value = {}
            for _ in range(67):
                value = {'a': value}
            with self.assertRaises(ValueError):
                write_nbt(Path(folder) / 'deep.nbt', value)
    def test_typed_snbt_arrays_remain_typed(self):
        value = SNBT('{a:[I;1,2] b:[B;1b,-1b]}').parse()
        self.assertEqual(value['a'].kind, 'I'); self.assertIn('[I;1,2]', snbt(value)); self.assertIn('[B;1,-1]', snbt(value))
    def test_new_task_fields_preserve_confirmation_and_consumption(self):
        self.assertTrue(convert_task({'type': 'checkmark'}, 0)['manual'])
        result = convert_task({'type': 'item', 'items': ['minecraft:iron_ingot', 'minecraft:gold_ingot'], 'ignore_damage': 1, 'only_from_crafting': 'true', 'nbt_mode': 'ignore'}, 0)
        self.assertTrue(result['craftingOnly']); self.assertTrue(result['ignoreDamage']); self.assertEqual(result['nbtMode'], 'ignore'); self.assertEqual(len(result['targets']), 1)
        self.assertTrue(convert_task({'type': 'item', 'item': 'minecraft:iron_ingot'}, 0, repeat=True)['consume'])
    def test_alternative_item_filters_preserve_distinct_metadata(self):
        task = convert_task({'type': 'item', 'items': [{'id': 'minecraft:stone', 'Damage': 1}, {'id': 'minecraft:stone', 'Damage': 2}]}, 0)
        self.assertEqual([v['meta'] for v in task['filter']['children']], [1, 2])
    def test_location_uses_lower_corner_and_full_extent(self):
        result = convert_task({'type': 'location', 'location': [0, -2, 64, 10, 8, 4, 6]}, 0)
        self.assertEqual((result['x'], result['width'], result['height']), (-2, 8, 4))
    def test_rewards_preserve_scope_bonus_auto_and_player_permissions(self):
        r = convert_reward({'type': 'item', 'uid': 'a', 'item': 'minecraft:bread', 'only_one': 1, 'random_bonus': 3, 'team_reward': 'true', 'auto': 'enabled'})
        self.assertEqual(r['id'], 'ftb_0000000a'); self.assertEqual(r['mode'], 'team'); self.assertEqual(r['auto'], 'enabled'); self.assertEqual(r['randomBonus'], 3)
        self.assertTrue(convert_reward({'type': 'command', 'command': 'help', 'player_command': 1})['asPlayer'])
    def test_binary_book_task_and_chapter_dependencies_and_loot(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); chapter = root / 'chapters' / '01'; chapter.mkdir(parents=True)
            write_nbt(chapter / 'chapter.nbt', {'title': 'Binary chapter'})
            write_nbt(chapter / '02.nbt', {'title': 'First', 'tasks': [{'type': 'checkmark', 'uid': '03'}]})
            write_nbt(chapter / '04.nbt', {'title': 'Next', 'dependencies': ['03'], 'dependency_requirement': 'one_started', 'tasks': [{'type': 'checkmark', 'uid': '05'}], 'rewards': [{'type': 'loot', 'table': '06'}]})
            tables = root / 'reward_tables'; tables.mkdir(); write_nbt(tables / '06.nbt', {'loot_size': 2, 'empty_weight': 1, 'rewards': [{'type': 'xp', 'xp': 3, 'weight': 5}]})
            book = convert(root)
            self.assertEqual(book['quests'][1]['dependencies'], ['task:ftb_00000002/ftb_00000003']); self.assertEqual(book['quests'][1]['dependencyState'], 'started')
            self.assertEqual(book['lootSettings']['ftb_00000006']['rolls'], 2)
    def test_unknown_semantics_fail_with_source_path(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); chapter = root / 'chapters' / '01'; chapter.mkdir(parents=True)
            (chapter / 'chapter.snbt').write_text('{}'); (chapter / '02.snbt').write_text('{tasks:[{type:"unknown_mod_task"}]}')
            with self.assertRaisesRegex(ValueError, '02.snbt.*unknown_mod_task'):
                convert(root)
    def book(self):
        return {'quests': [{'id': 'ftb_00000002', 'rewardMode': 'individual', 'tasks': [{'id': 'ftb_00000003', 'type': 'item', 'count': 4}], 'rewards': [{'id': 'ftb_00000004', 'type': 'xp', 'mode': 'individual'}]}]}
    def test_progress_migrates_task_completion_and_uuid_personal_claims(self):
        member = str(uuid.uuid4())
        output = migrate(self.book(), {'Tasks': {'03': 4}, 'ClaimedPlayerRewards': {member.replace('-', ''): [4]}}, [member])['data']
        state = next(iter(output['players'].values())); self.assertIn('ftb_00000002', state['completed']); self.assertIn('ftb_00000002:ftb_00000004', output['personalRewards'][member])
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'sigmarixquests.dat'; write_nbt(path, {'data': output}); self.assertEqual(read_nbt(path)['data'], output)
    def test_progress_rejects_unknown_ids_and_changed_reward_scopes(self):
        member = str(uuid.uuid4())
        with self.assertRaisesRegex(ValueError, 'Unknown saved task'):
            migrate(self.book(), {'Tasks': {'99': 1}}, [member])
        with self.assertRaisesRegex(ValueError, 'became individual'):
            migrate(self.book(), {'ClaimedTeamRewards': [4]}, [member])
    def test_partial_progress_does_not_complete_quest(self):
        member = str(uuid.uuid4()); state = next(iter(migrate(self.book(), {'Tasks': {'03': 2}}, [member])['data']['players'].values()))
        self.assertFalse(state['completed']); self.assertEqual(state['tasks']['ftb_00000002:ftb_00000003'], 2)

    def test_xp_levels_and_unsupported_long_arrays(self):
        self.assertEqual(convert_reward({'type': 'xp_levels', 'xp_levels': 12})['count'], 12)
        with self.assertRaisesRegex(ValueError, 'Long-array'):
            snbt(SNBT('[L;1L]').parse())
    def test_corrupt_compression_has_a_clear_error(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'bad.nbt'; path.write_bytes(b'\x1f\x8b\x08')
            with self.assertRaisesRegex(ValueError, 'compressed NBT'):
                read_nbt(path)
    def test_table_cycles_are_rejected_before_expansion(self):
        from import_ftb112 import resolve_choices
        tables = {'a': [{'_optionsTable': 'b', 'type': 'random'}], 'b': [{'_optionsTable': 'a', 'type': 'random'}]}
        with self.assertRaisesRegex(ValueError, 'cyclic'):
            resolve_choices({'_optionsTable': 'a', 'type': 'choice'}, tables)

    def test_snbt_numeric_overflow_is_not_reinterpreted_as_text(self):
        for value in ('{x:1e999}', '{x:128b}', '{x:2147483648}', '{x:9223372036854775808L}'):
            with self.assertRaises(ValueError):
                SNBT(value).parse()

    def test_energy_integration_types_preserve_units_and_value(self):
        for kind in ('ic2_energy', 'botania_mana'):
            task = convert_task({'type': kind, 'value': 12345}, 0)
            self.assertEqual(task['type'], kind); self.assertEqual(task['count'], 12345)

    def test_emc_import_keeps_resource_consumption(self):
        task = convert_task({'type': 'emc', 'value': 8192}, 0)
        self.assertEqual(task['type'], 'emc'); self.assertEqual(task['count'], 8192); self.assertTrue(task['consume'])

if __name__ == '__main__':
    unittest.main()
