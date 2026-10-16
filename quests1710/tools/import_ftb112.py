#!/usr/bin/env python3
"""Strict offline importer for a subset of FTB Quests 1.12 SNBT books.

Never edits the source book. Unsupported semantic fields fail the conversion
instead of silently weakening a quest. This is not an importer for modern FTB.
"""
import argparse
import json
import re
from pathlib import Path


class Number(int):
    def __new__(cls, value, suffix=""):
        result = super().__new__(cls, value)
        result.suffix = suffix
        return result


class Decimal(float):
    def __new__(cls, value, suffix=""):
        result = super().__new__(cls, value)
        result.suffix = suffix
        return result


class SNBT:
    def __init__(self, text):
        if len(text.encode("utf-8")) > 1024 * 1024:
            raise ValueError("SNBT file exceeds 1 MiB")
        self.text, self.at, self.nodes = text, 0, 0

    def skip(self):
        while self.at < len(self.text):
            if self.text[self.at].isspace() or self.text[self.at] == ',':
                self.at += 1
            elif self.text.startswith('//', self.at) or self.text[self.at] == '#':
                end = self.text.find('\n', self.at)
                self.at = len(self.text) if end < 0 else end + 1
            elif self.text.startswith('/*', self.at):
                end = self.text.find('*/', self.at + 2)
                if end < 0:
                    raise ValueError("Unterminated SNBT comment")
                self.at = end + 2
            else:
                return

    def quoted(self):
        quote = self.text[self.at]
        self.at += 1
        result = []
        while self.at < len(self.text):
            char = self.text[self.at]
            self.at += 1
            if char == quote:
                return ''.join(result)
            if char == '\\':
                if self.at >= len(self.text):
                    break
                char = self.text[self.at]
                self.at += 1
                if char == 'u':
                    digits = self.text[self.at:self.at + 4]
                    if not re.fullmatch('[0-9a-fA-F]{4}', digits):
                        raise ValueError("Invalid Unicode escape")
                    result.append(chr(int(digits, 16)))
                    self.at += 4
                    continue
                char = {'n': '\n', 'r': '\r', 't': '\t', 'b': '\b', 'f': '\f'}.get(char, char)
            result.append(char)
        raise ValueError("Unterminated SNBT string")

    def value(self, depth=0):
        self.nodes += 1
        if depth > 64 or self.nodes > 100000:
            raise ValueError("SNBT complexity limit")
        self.skip()
        if self.at >= len(self.text):
            raise ValueError("Unexpected end of SNBT")
        char = self.text[self.at]
        if char in '\"\'':
            return self.quoted()
        if char == '{':
            self.at += 1
            result = {}
            while True:
                self.skip()
                if self.at >= len(self.text):
                    raise ValueError("Unterminated compound")
                if self.text[self.at] == '}':
                    self.at += 1
                    return result
                if self.text[self.at] in '\"\'':
                    key = self.quoted()
                else:
                    start = self.at
                    while self.at < len(self.text) and self.text[self.at] not in ':\r\n{}[],':
                        self.at += 1
                    key = self.text[start:self.at].strip()
                self.skip()
                if not key or self.at >= len(self.text) or self.text[self.at] != ':':
                    raise ValueError("Expected key: value")
                self.at += 1
                if key in result:
                    raise ValueError("Duplicate SNBT key: " + key)
                result[key] = self.value(depth + 1)
        if char == '[':
            self.at += 1
            self.skip()
            if re.match('[BILbil];', self.text[self.at:self.at + 2]):
                self.at += 2
            result = []
            while True:
                self.skip()
                if self.at >= len(self.text):
                    raise ValueError("Unterminated list")
                if self.text[self.at] == ']':
                    self.at += 1
                    return result
                result.append(self.value(depth + 1))
        start = self.at
        while self.at < len(self.text) and not self.text[self.at].isspace() and self.text[self.at] not in ',]}':
            self.at += 1
        token = self.text[start:self.at]
        if not token:
            raise ValueError("Unexpected SNBT token")
        if token.lower() in ('true', 'false'):
            return Number(token.lower() == 'true', 'b')
        if re.fullmatch(r'[+-]?\d+[bBsSlL]?', token):
            suffix = token[-1] if token[-1].isalpha() else ''
            return Number(int(token[:-1] if suffix else token), suffix)
        if re.fullmatch(r'[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?[fFdD]?', token):
            suffix = token[-1] if token[-1] in 'fFdD' else ''
            return Decimal(float(token[:-1] if suffix else token), suffix)
        return token

    def parse(self):
        result = self.value()
        self.skip()
        if self.at != len(self.text):
            raise ValueError("Trailing SNBT input")
        return result


def snbt(value):
    if isinstance(value, (Number, Decimal)):
        return str(int(value) if isinstance(value, Number) else float(value)) + value.suffix
    if isinstance(value, dict):
        return '{' + ','.join(json.dumps(k) + ':' + snbt(v) for k, v in value.items()) + '}'
    if isinstance(value, list):
        return '[' + ','.join(snbt(v) for v in value) + ']'
    return json.dumps(value, ensure_ascii=False)


def code(value):
    if isinstance(value, str):
        value = int(value.removeprefix('#'), 16)
    return 'ftb_' + format(int(value) & 0xffffffff, '08x')


def item(value):
    if isinstance(value, str):
        # The supported compact form is a registry ID, with optional " count".
        parts = value.split()
        if not parts or len(parts) > 2:
            raise ValueError('Unsupported compact item: ' + value)
        return {'item': parts[0], 'count': int(parts[1]) if len(parts) == 2 else 1}
    if not isinstance(value, dict) or 'id' not in value:
        raise ValueError('Missing item registry ID')
    result = {'item': value['id'], 'count': int(value.get('Count', 1)), 'meta': int(value.get('Damage', 0))}
    if value.get('tag'):
        result['nbt'] = snbt(value['tag'])
    return result


def refuse_fields(raw, names):
    for name in names:
        if raw.get(name):
            raise ValueError('Unsupported semantic field: ' + name)


def convert_task(raw, index):
    kind = raw.get('type', '').removeprefix('ftbquests:')
    result = {'id': code(raw['uid']) if raw.get('uid') else 't' + str(index)}
    if kind == 'checkmark':
        result['type'] = 'check'
    elif kind == 'item':
        refuse_fields(raw, ('ignore_damage', 'only_from_crafting'))
        alternatives = raw.get('items', [])
        if len(alternatives) > 1:
            raise ValueError('Alternative item filters need manual conversion')
        value = item(alternatives[0] if alternatives else raw.get('item'))
        result.update(type='item', target=value.pop('item'), count=int(raw.get('count', 1)), consume=bool(raw.get('consume_items', 0)))
        for key in ('meta', 'nbt'):
            if key in value:
                result[key] = value[key]
        if raw.get('ignore_nbt'):
            raise ValueError('NBT matching mode needs manual conversion')
    elif kind == 'dimension':
        result.update(type='dimension', dimension=int(raw.get('dim', 0)))
    elif kind == 'xp':
        result.update(type='xp', count=int(raw.get('value', 1)), levels=not bool(raw.get('points', False)), consume=True)
    elif kind in ('forge_energy', 'fluid'):
        result.update(type='energy' if kind == 'forge_energy' else 'fluid', count=int(raw.get('value', raw.get('amount', 1000))))
        if kind == 'fluid':
            if raw.get('nbt'):
                raise ValueError('Fluid NBT needs manual conversion')
            result['target'] = raw.get('fluid', 'water')
            result['consume'] = True
    elif kind == 'stat':
        result.update(type='stat', target=raw.get('stat', ''), count=int(raw.get('value', 1)))
    elif kind == 'kill':
        entity = raw.get('entity', '')
        vanilla = {'minecraft:zombie': 'Zombie', 'minecraft:skeleton': 'Skeleton', 'minecraft:creeper': 'Creeper', 'minecraft:spider': 'Spider', 'minecraft:blaze': 'Blaze', 'minecraft:enderman': 'Enderman', 'minecraft:wither': 'WitherBoss', 'minecraft:ender_dragon': 'EnderDragon', 'minecraft:pig': 'Pig', 'minecraft:cow': 'Cow', 'minecraft:sheep': 'Sheep', 'minecraft:chicken': 'Chicken'}
        if entity not in vanilla:
            raise ValueError('Entity registry mapping needed: ' + entity)
        result.update(type='kill', target=vanilla[entity], count=int(raw.get('value', 1)))
    else:
        raise ValueError('Task requires manual conversion: ' + kind)
    return result


def convert_reward(raw):
    kind = raw.get('type', '').removeprefix('ftbquests:')
    refuse_fields(raw, ('only_one', 'random_bonus', 'player_command', 'team_reward', 'auto'))
    if kind == 'item':
        result = item(raw.get('item'))
        result.update(type='item', count=int(raw.get('count', result['count'])))
        return result
    if kind in ('xp', 'xp_levels'):
        return {'type': kind, 'count': int(raw.get('xp', 1))}
    if kind == 'command':
        command = raw.get('command', '')
        if '@' in command:
            raise ValueError('1.12 command selectors need manual review')
        return {'type': 'command', 'target': command}
    raise ValueError('Reward requires manual conversion: ' + kind)


def convert(folder):
    if list(folder.rglob('*.nbt')):
        raise ValueError('Binary .nbt books must be exported to SNBT by FTB Quests first')
    book = {'format': 2, 'title': 'Imported FTB 1.12 book', 'chapters': [], 'quests': []}
    if (folder / 'reward_tables').exists() and list((folder / 'reward_tables').glob('*.snbt')):
        raise ValueError('Reward tables need manual conversion; refusing to discard them')
    errors = []
    for chapter_dir in sorted((folder / 'chapters').iterdir()):
        if not chapter_dir.is_dir():
            continue
        chapter_path = chapter_dir / 'chapter.snbt'
        try:
            chapter = SNBT(chapter_path.read_text(encoding='utf-8')).parse()
            chapter_id = code(chapter_dir.name)
            book['chapters'].append({'id': chapter_id, 'title': chapter.get('title', chapter_id), 'description': ''})
        except (ValueError, OSError) as error:
            errors.append(str(chapter_path) + ': ' + str(error))
            continue
        for path in sorted(chapter_dir.glob('*.snbt')):
            if path == chapter_path:
                continue
            try:
                raw = SNBT(path.read_text(encoding='utf-8')).parse()
                refuse_fields(raw, ('custom_click', 'guide_page'))
                requirement = raw.get('dependency_requirement', 'all_completed')
                if requirement not in ('all_completed', 'one_completed'):
                    raise ValueError('Started-dependency semantics need manual conversion')
                quest = {'id': code(path.stem), 'chapter': chapter_id, 'title': raw.get('title', path.stem),
                         'description': '\n'.join([raw.get('description', '')] + raw.get('text', [])),
                         'x': float(raw.get('x', 0)), 'y': float(raw.get('y', 0)),
                         'hidden': bool(raw.get('hide', False)), 'optional': bool(raw.get('optional', False)),
                         'repeatable': bool(raw.get('can_repeat', False)), 'anyTask': bool(raw.get('or_tasks', False)),
                         'anyDependency': requirement == 'one_completed', 'minDependencies': int(raw.get('min_required_dependencies', 0)),
                         'dependencies': [code(v) for v in raw.get('dependencies', [])],
                         'tasks': [convert_task(t, i) for i, t in enumerate(raw.get('tasks', []))],
                         'rewards': [convert_reward(r) for r in raw.get('rewards', [])]}
                if not quest['tasks']:
                    raise ValueError('Empty quest needs manual conversion')
                book['quests'].append(quest)
            except (ValueError, TypeError, KeyError) as error:
                errors.append(str(path) + ': ' + str(error))
    ids = {q['id'] for q in book['quests']}
    for quest in book['quests']:
        for dependency in quest['dependencies']:
            if dependency not in ids:
                errors.append(quest['id'] + ': dependency is not an imported quest: ' + dependency)
    if not book['quests']:
        errors.append('No supported quests found')
    if errors:
        raise ValueError('\n'.join(errors))
    return book


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('folder', type=Path, help='config/ftbquests/normal from a 1.12.2 instance')
    parser.add_argument('output', type=Path, help='New destination JSON; existing files are never overwritten')
    args = parser.parse_args()
    try:
        book = convert(args.folder)
        with args.output.open('x', encoding='utf-8') as out:
            json.dump(book, out, ensure_ascii=False, indent=2)
    except (OSError, ValueError) as error:
        parser.exit(1, str(error) + '\n')
    print(f"Converted {len(book['quests'])} quests. Review IDs and commands, then validate by /quests reload.")


if __name__ == '__main__':
    main()
