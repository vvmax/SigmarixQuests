import tempfile
import unittest
from pathlib import Path
from import_ftb112 import SNBT, snbt, convert, convert_task, convert_reward

class ImportTests(unittest.TestCase):
    def test_snbt_comments_suffixes_and_omitted_commas(self):
        value = SNBT('{ // comment\nname:"Привет"\nlist:[I;1,2,3]\ntag:{on:1b}\n}').parse()
        self.assertEqual(value['name'], 'Привет')
        self.assertEqual(value['list'], [1, 2, 3])
        self.assertIn('1b', snbt(value['tag']))

    def test_parser_rejects_duplicate_keys_and_excess_depth(self):
        for text in ('{a:1 a:2}', '[' * 66 + '0' + ']' * 66, '{a:"broken}'):
            with self.assertRaises(ValueError):
                SNBT(text).parse()

    def test_unsupported_types_fail_instead_of_becoming_checkmarks(self):
        with self.assertRaises(ValueError):
            convert_task({'type': 'ftbquests:npc_quest'}, 0)
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

if __name__ == '__main__':
    unittest.main()
