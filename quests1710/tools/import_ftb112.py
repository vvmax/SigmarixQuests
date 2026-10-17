#!/usr/bin/env python3
"""Strict offline importer for FTB Quests 1.12 SNBT/binary NBT books.

Never edits the source book. Unsupported semantic fields fail the conversion
instead of silently weakening a quest. This is not an importer for modern FTB.
"""
import argparse
import json
import re
from pathlib import Path
from urllib.parse import urlsplit


from nbt_codec import IntTag as Number, FloatTag as Decimal, TypedArray, read_nbt


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
            kind = None
            if re.match('[BILbil];', self.text[self.at:self.at + 2]):
                kind = self.text[self.at].upper()
                self.at += 2
            result = TypedArray(kind=kind) if kind else []
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
    if isinstance(value, TypedArray):
        if value.kind == 'L':
            raise ValueError('Long-array NBT does not exist in Minecraft 1.7.10')
        limit = 128 if value.kind == 'B' else 2**31
        if any(not isinstance(v, int) or not -limit <= v < limit for v in value):
            raise ValueError('Typed NBT array value outside range')
        return '[' + value.kind + ';' + ','.join(str(int(v)) for v in value) + ']'
    if isinstance(value, list):
        return '[' + ','.join(snbt(v) for v in value) + ']'
    return json.dumps(value, ensure_ascii=False)


def code(value):
    if isinstance(value, str):
        value = int(value.removeprefix('#'), 16)
    return 'ftb_' + format(int(value) & 0xffffffff, '08x')


# Independent equivalents. Native crate resolution preserves legacy type aliases.
ITEM_ALIASES = {
    'book': 'quest_book', 'lootcrate': 'loot_crate', 'custom_icon': 'custom_icon', 'screen': 'task_screen',
    'progress_screen': 'progress_screen', 'progress_detector': 'progress_detector',
    'detector': 'redstone_detector', 'chest': 'quest_chest', 'barrier': 'quest_barrier',
    'reward_collector': 'reward_collector', 'loot_crate_storage': 'loot_crate_storage',
    'loot_crate_opener': 'loot_crate_opener',
}

def item_id(value):
    if value.startswith('ftbquests:') and value[10:] in ITEM_ALIASES:
        return 'sigmarixquests:' + ITEM_ALIASES[value[10:]]
    return value


def _item(value):
    if isinstance(value, str):
        # The supported compact form is a registry ID, with optional " count".
        parts = value.split()
        if not parts or len(parts) > 2:
            raise ValueError('Unsupported compact item: ' + value)
        return {'item': item_id(parts[0]), 'count': int(parts[1]) if len(parts) == 2 else 1}
    if not isinstance(value, dict) or 'id' not in value:
        raise ValueError('Missing item registry ID')
    result = {'item': item_id(value['id']), 'count': int(value.get('Count', 1)), 'meta': int(value.get('Damage', 0))}
    if value['id'] == 'ftbquests:detector':
        if result['meta'] not in (0, 1): raise ValueError('Unknown FTB detector variant')
        result['item'] = 'sigmarixquests:' + ('player_detector' if result['meta'] == 1 else 'redstone_detector')
        result['meta'] = 0
    if value.get('tag'):
        result['nbt'] = snbt(value['tag'])
    return result


def item(value, mappings=None):
    result = _item(value)
    mapped = (mappings or {}).get('items', {}).get(result['item'])
    if isinstance(mapped, str):
        result['item'] = mapped
    elif mapped is not None:
        result['item'] = mapped['id']
        if 'meta' in mapped:
            result['meta'] = mapped['meta']
    return result


def refuse_fields(raw, names):
    for name in names:
        if raw.get(name):
            raise ValueError('Unsupported semantic field: ' + name)


def tristate(value, default=False):
    if value is None or value == 'default':
        return default
    if value in (True, 1, 'true'):
        return True
    if value in (False, 0, 'false'):
        return False
    raise ValueError('Unsupported tristate: ' + str(value))

VANILLA_ENTITIES = {'minecraft:zombie': 'Zombie', 'minecraft:skeleton': 'Skeleton', 'minecraft:creeper': 'Creeper', 'minecraft:spider': 'Spider', 'minecraft:cave_spider': 'CaveSpider', 'minecraft:blaze': 'Blaze', 'minecraft:enderman': 'Enderman', 'minecraft:wither': 'WitherBoss', 'minecraft:ender_dragon': 'EnderDragon', 'minecraft:pig': 'Pig', 'minecraft:cow': 'Cow', 'minecraft:sheep': 'Sheep', 'minecraft:chicken': 'Chicken', 'minecraft:ghast': 'Ghast', 'minecraft:slime': 'Slime', 'minecraft:magma_cube': 'LavaSlime', 'minecraft:villager': 'Villager', 'minecraft:zombie_pigman': 'PigZombie', 'minecraft:wolf': 'Wolf', 'minecraft:ocelot': 'Ozelot', 'minecraft:horse': 'EntityHorse', 'minecraft:squid': 'Squid', 'minecraft:witch': 'Witch', 'minecraft:bat': 'Bat', 'minecraft:silverfish': 'Silverfish', 'minecraft:iron_golem': 'VillagerGolem', 'minecraft:snowman': 'SnowMan'}
VANILLA_BIOMES = {'minecraft:ocean': 0, 'minecraft:plains': 1, 'minecraft:desert': 2, 'minecraft:extreme_hills': 3, 'minecraft:forest': 4, 'minecraft:taiga': 5, 'minecraft:swampland': 6, 'minecraft:river': 7, 'minecraft:hell': 8, 'minecraft:sky': 9, 'minecraft:frozen_ocean': 10, 'minecraft:frozen_river': 11, 'minecraft:ice_flats': 12, 'minecraft:ice_mountains': 13, 'minecraft:mushroom_island': 14, 'minecraft:mushroom_island_shore': 15, 'minecraft:beaches': 16, 'minecraft:jungle': 21, 'minecraft:jungle_hills': 22, 'minecraft:deep_ocean': 24, 'minecraft:birch_forest': 27, 'minecraft:roofed_forest': 29, 'minecraft:cold_taiga': 30, 'minecraft:mega_taiga': 32, 'minecraft:savanna': 35, 'minecraft:mesa': 37}

def mapping_rules(value):
    """Mappings are explicit pack-author decisions, never guessed version conversions."""
    value = {} if value is None else value
    if not isinstance(value, dict) or set(value) - {'items', 'fluids', 'entities', 'biomes', 'blocks', 'tileEntities', 'classes', 'advancements', 'stats'}:
        raise ValueError('Unknown mapping section')
    for section, entries in value.items():
        if not isinstance(entries, dict) or len(entries) > 4096:
            raise ValueError('Invalid mapping section: ' + section)
        for source, target in entries.items():
            if not isinstance(source, str) or not source or len(source) > 1024:
                raise ValueError('Invalid mapping key')
            if section == 'blocks':
                if not isinstance(target, dict) or set(target) - {'id', 'meta'} or not isinstance(target.get('id'), str) or not target['id'] or type(target.get('meta')) is not int or not 0 <= target['meta'] <= 15:
                    raise ValueError('Block mapping needs id and metadata 0..15: ' + source)
            elif section == 'items' and isinstance(target, dict):
                if set(target) - {'id', 'meta'} or not isinstance(target.get('id'), str) or not target['id'] or 'meta' in target and (type(target['meta']) is not int or not 0 <= target['meta'] <= 32767):
                    raise ValueError('Item mapping needs id and optional metadata 0..32767: ' + source)
            elif not isinstance(target, str) or not target or len(target) > 1024:
                raise ValueError('Mapping target must be a nonempty string: ' + source)
    return value


def entity_id(value, mappings=None):
    target = (mappings or {}).get('entities', {}).get(value, VANILLA_ENTITIES.get(value))
    if target is None:
        raise ValueError('Entity registry mapping needed: ' + str(value))
    return target


def achievement_id(raw, mappings):
    source = raw.get('advancement', '')
    if raw.get('criterion'):
        source += '#' + raw['criterion']
    target = (mappings or {}).get('advancements', {}).get(source)
    if not target:
        raise ValueError('Achievement mapping needed: ' + source)
    return target


def block_state_key(block, properties):
    if not properties:
        return block
    if isinstance(properties, str):
        pairs = [[value.strip() for value in part.split('=', 1)] for part in properties.split(',')]
        if any(len(pair) != 2 or not pair[0] or not pair[1] for pair in pairs) or len({pair[0] for pair in pairs}) != len(pairs):
            raise ValueError('Invalid block properties')
        properties = dict(pairs)
    if not isinstance(properties, dict) or any(not isinstance(k, str) or not isinstance(v, str) for k, v in properties.items()):
        raise ValueError('Invalid block properties')
    return block + '[' + ','.join(k + '=' + properties[k] for k in sorted(properties)) + ']'


def object_presentation(raw, mappings=None):
    result = {'title': text(raw.get('title', '')), 'description': text(raw.get('description', ''))}
    if raw.get('icon'):
        value = item(raw['icon'], mappings)
        result.update(icon=value['item'], iconMeta=value.get('meta', 0), iconNbt=value.get('nbt', ''))
    return result


def convert_task(raw, index, defaults=None, repeat=False, mappings=None):
    defaults = defaults or {}
    kind = raw.get('type', '').removeprefix('ftbquests:')
    result = {'id': code(raw['uid']) if raw.get('uid') else 't' + str(index)}
    result.update(object_presentation(raw, mappings))
    result['disableToast'] = bool(raw.get('disable_toast', False))
    if kind == 'checkmark':
        result.update(type='check', manual=True)
    elif kind == 'item':
        alternatives = raw.get('items') or [raw.get('item')]
        values = [item(v, mappings) for v in alternatives]
        value = values[0]
        # Metadata/NBT filters are shared by our alternatives. Refuse alternatives with distinct filters.
        distinct_filters = any((v.get('meta', 0), v.get('nbt', '')) != (value.get('meta', 0), value.get('nbt', '')) for v in values)
        result.update(type='item', target=value['item'], targets=[v['item'] for v in values[1:]], count=max(1, int(raw.get('count', 1))),
                      consume=repeat or tristate(raw.get('consume_items'), bool(defaults.get('default_consume_items', False))),
                      craftingOnly=tristate(raw.get('only_from_crafting'), False), ignoreDamage=bool(raw.get('ignore_damage', False)))
        for key in ('meta', 'nbt'):
            if key in value:
                result[key] = value[key]
        mode = raw.get('nbt_mode', raw.get('ignore_nbt', 0))
        modes = {0: 'exact', 1: 'ignore', 2: 'subset', 'match': 'exact', 'ignore': 'ignore', 'contain': 'subset', 'exact': 'exact', 'subset': 'subset'}
        if mode not in modes:
            raise ValueError('Unknown NBT mode: ' + str(mode))
        result['nbtMode'] = modes[mode]
        if distinct_filters:
            result.pop('target', None); result['targets'] = []; result.pop('meta', None); result.pop('nbt', None)
            result['filter'] = {'type': 'any', 'children': [{'type': 'item', 'value': v['item'], 'meta': -1 if result['ignoreDamage'] else v.get('meta', 0), 'nbt': v.get('nbt', ''), 'nbtMode': modes[mode]} for v in values]}
    elif kind == 'dimension':
        result.update(type='dimension', dimension=int(raw.get('dim', 0)))
    elif kind == 'xp':
        result.update(type='xp', count=int(raw.get('value', 1)), levels=not bool(raw.get('points', False)), consume=True)
    elif kind in ('forge_energy', 'ic2_energy', 'botania_mana', 'buildcraft_mj', 'fluid'):
        result.update(type='energy' if kind == 'forge_energy' else kind, count=int(raw.get('value', raw.get('amount', 1000))))
        if kind == 'fluid':
            result.update(target=(mappings or {}).get('fluids', {}).get(raw.get('fluid', 'water'), raw.get('fluid', 'water')), consume=True)
            if raw.get('nbt'):
                result['nbt'] = snbt(raw['nbt']) if isinstance(raw['nbt'], dict) else raw['nbt']
    elif kind == 'emc':
        result.update(type='emc', count=int(raw.get('value', 8192)), consume=True)
    elif kind == 'stat':
        result.update(type='stat', target=(mappings or {}).get('stats', {}).get(raw.get('stat', ''), raw.get('stat', '')), count=int(raw.get('value', 1)))
    elif kind == 'kill':
        result.update(type='kill', target=entity_id(raw.get('entity', ''), mappings), count=int(raw.get('value', 1)))
    elif kind == 'biome':
        biome = raw.get('biome')
        target = (mappings or {}).get('biomes', {}).get(biome, VANILLA_BIOMES.get(biome))
        if target is None:
            raise ValueError('Biome registry mapping needed: ' + str(biome))
        result.update(type='biome', target=str(target))
    elif kind == 'location':
        area = raw.get('location', [])
        if len(area) != 7 or any(int(v) < 0 for v in area[4:]):
            raise ValueError('Invalid location box')
        # Both versions use lower-corner coordinates and exclusive upper bounds.
        dim, x, y, z, w, h, d = map(int, area)
        result.update(type='location', box=True, dimension=dim, x=x, y=y, z=z,
                      width=w, height=h, depth=d, ignoreDimension=bool(raw.get('ignore_dim', False)))
    elif kind in ('observation', 'interaction'):
        match_type = raw.get('match_type', 'block_id')
        if match_type not in ('block_id', 'entity_id', 'block_entity_id', 'block_entity_class', 'entity_class'):
            raise ValueError('Ray matcher requires manual conversion: ' + str(match_type))
        target = raw.get('match', '')
        result.update(type=kind, target=entity_id(target, mappings) if match_type == 'entity_id' else (mappings or {}).get('classes' if match_type.endswith('_class') else 'tileEntities' if match_type == 'block_entity_id' else '_', {}).get(target, target),
                      entity=match_type.startswith('entity_'), matchType=match_type, ignoreDamage=True)
        if match_type == 'block_id':
            key = block_state_key(target, raw.get('properties'))
            block = (mappings or {}).get('blocks', {}).get(key)
            if raw.get('properties') and block is None:
                raise ValueError('Block state mapping needed: ' + key)
            if block is not None:
                result.update(target=block['id'], meta=block['meta'], ignoreDamage=False)
        elif raw.get('properties'):
            raise ValueError('Properties require a block matcher')
        if raw.get('nbt'): result.update(nbt=snbt(raw['nbt']) if isinstance(raw['nbt'], dict) else raw['nbt'], nbtMode='subset')
        if kind == 'observation':
            result['lookTicks'] = int(raw.get('timer', 0))
    elif kind == 'advancement':
        result.update(type='achievement', target=achievement_id(raw, mappings))
    elif kind in ('npc_quest', 'npc_dialog', 'npc_faction'):
        result.update(type=kind, target=str(int(raw.get(kind, 0))))
        if kind == 'npc_quest': result['checkActive'] = bool(raw.get('check_active', False))
        if kind == 'npc_faction':
            comparison = {'>=': 'at_least', '<=': 'at_most', '==': 'equals', '!=': 'not_equals', '>': 'greater', '<': 'less', 'greater_than_or_equal': 'at_least', 'less_than_or_equal': 'at_most', 'greater_than': 'greater', 'less_than': 'less', 'not': 'not_equals', 'more_or_equal': 'at_least', 'less_or_equal': 'at_most', 'equal': 'equals', 'at_least': 'at_least', 'at_most': 'at_most', 'equals': 'equals'}.get(raw.get('point_mode', 'more_or_equal'))
            if comparison is None: raise ValueError('Unknown faction comparison: ' + str(raw.get('point_mode')))
            result.update(points=int(raw.get('required_points', 1)), comparison=comparison)
    elif kind == 'gamestage':
        result.update(type='gamestage', target=raw.get('stage', ''))
    elif kind == 'reskillable':
        result.update(type='skill', target=raw.get('skill', ''), count=int(raw.get('skill_level', 1)))
    elif kind == 'custom':
        result.update(type='custom', target='', count=1, manual=bool(raw.get('enable_button', False)))
    else:
        raise ValueError('Task requires manual conversion: ' + kind)
    return result

def convert_reward(raw, defaults=None, repeat=False, nested=False, mappings=None):
    defaults = defaults or {}
    kind = raw.get('type', '').removeprefix('ftbquests:')
    result = object_presentation(raw, mappings)
    if not nested:
        if raw.get('uid'):
            result['id'] = code(raw['uid'])
        result['mode'] = 'team' if repeat or tristate(raw.get('team_reward'), bool(defaults.get('default_reward_team', False))) else 'individual'
        auto = raw.get('auto', 'invisible' if raw.get('invisible') else 'enabled' if raw.get('autoclaim') else 'disabled')
        if auto not in ('enabled', 'disabled', 'invisible', 'default'):
            raise ValueError('Unknown auto reward mode: ' + str(auto))
        result['auto'] = 'inherit' if auto == 'default' else auto
    elif raw.get('team_reward') not in (None, 'default') or raw.get('auto') not in (None, 'default', 'disabled'):
        raise ValueError('Nested reward scope requires manual conversion')
    if kind == 'item':
        value = item(raw.get('item'), mappings); result.update(value)
        result.update(type='item', count=int(raw.get('count', value['count'])), onlyOne=bool(raw.get('only_one', False)), randomBonus=int(raw.get('random_bonus', 0)))
    elif kind in ('xp', 'xp_levels'):
        result.update(type=kind, count=int(raw.get('xp_levels' if kind == 'xp_levels' else 'xp', raw.get('levels', 1))))
    elif kind == 'command':
        command = raw.get('command', '')
        if '@' in command:
            raise ValueError('1.12 command selectors need manual review')
        result.update(type='command', target=command, asPlayer=bool(raw.get('player_command', False)))
    elif kind in ('random', 'choice', 'loot'):
        table = raw.get('table')
        if table is not None and code(table) != 'ftb_ffffffff':
            result.update(type='loot' if kind == 'loot' else kind, target=code(table))
            if kind in ('choice', 'random'):
                result['_optionsTable'] = code(table)
        else:
            result.update(type='choice' if kind == 'choice' else 'random', options=[])
            for option in raw.get('rewards', []):
                value = convert_reward(option, defaults, repeat, True, mappings); value['weight'] = int(option.get('weight', 1)); result['options'].append(value)
            if not result['options']:
                raise ValueError('Missing reward table/options')
    elif kind in ('npc_quest', 'npc_faction'):
        result.update(type=kind, target=str(int(raw.get(kind, 0))))
        if kind == 'npc_faction': result.update(points=int(raw.get('points', raw.get('required_points', 1))), add=bool(raw.get('add', True)))
    elif kind == 'npc_mail':
        mail = raw.get('mail')
        if not isinstance(mail, dict): raise ValueError('Missing NPC mail NBT')
        mail = dict(mail)
        attachments = []
        for index, stack in enumerate(mail.pop('MailItems', [])):
            value = item(stack, mappings)
            if not isinstance(value['item'], str): raise ValueError('NPC mail items require registry IDs')
            value['slot'] = int(stack.get('Slot', index)); attachments.append(value)
        result.update(type='npc_mail', nbt=snbt(mail), attachments=attachments)
    elif kind == 'advancement':
        result.update(type='achievement', target=achievement_id(raw, mappings))
    elif kind == 'gamestage':
        result.update(type='gamestage', target=raw.get('stage', ''), remove=bool(raw.get('remove', False)))
    elif kind == 'toast':
        result.update(type='toast', target=raw.get('title') or 'Quest reward', text=raw.get('description', ''))
        if raw.get('icon'):
            result['item'] = item(raw['icon'], mappings)['item']
    else:
        raise ValueError('Reward requires manual conversion: ' + kind)
    return result

def read_document(path):
    value = read_nbt(path) if path.suffix in ('.nbt', '.dat') else SNBT(path.read_text(encoding='utf-8')).parse()
    if not isinstance(value, dict):
        raise ValueError('Expected compound document: ' + str(path))
    return value

def document(folder, name, optional=False):
    files = [p for p in (folder / (name + '.snbt'), folder / (name + '.nbt')) if p.exists()]
    if not files and optional:
        return {}
    if len(files) != 1:
        raise ValueError('Expected exactly one SNBT/NBT document: ' + str(folder / name))
    return read_document(files[0])

def documents(folder, exclude=()):
    files = sorted(p for p in folder.iterdir() if p.is_file() and p.suffix in ('.snbt', '.nbt') and p.stem not in set(exclude) | {'index'}) if folder.exists() else []
    if len({p.stem for p in files}) != len(files):
        raise ValueError('Ambiguous SNBT/NBT duplicates in ' + str(folder))
    index = document(folder, 'index', True).get('index', [])
    if index:
        order = {code(v): i for i, v in enumerate(index)}
        if len(order) != len(index):
            raise ValueError('Duplicate index IDs')
        files.sort(key=lambda p: order.get(code(p.stem), len(order)))
    return files

def resolve_choices(reward, tables, path=(), depth=0):
    if depth > 16:
        raise ValueError('Reward nesting limit')
    if '_optionsTable' in reward:
        table = reward.pop('_optionsTable')
        if table not in tables or table in path:
            raise ValueError('Unknown or cyclic choice/random table: ' + table)
        reward.pop('target', None)
        reward['options'] = json.loads(json.dumps(tables[table]))
        path = path + (table,)
    for option in reward.get('options', []):
        resolve_choices(option, tables, path, depth + 1)


def weights(raw, defaults=None):
    defaults = defaults or {'passive': 0, 'monster': 0, 'boss': 0}
    return {key: int(raw.get(key, raw.get('#' + key, fallback))) for key, fallback in defaults.items()}

def seconds(value):
    if value is None: return 300
    text = str(value).strip().lower()
    chunks = re.findall(r'(\d+)([tsmhd])', text)
    if not chunks or ''.join(number + unit for number, unit in chunks) != text: raise ValueError('Unsupported cooldown duration: ' + text)
    ticks = sum(int(number) * {'t': 1, 's': 20, 'm': 1200, 'h': 72000, 'd': 1728000}[unit] for number, unit in chunks)
    return (ticks + 19) // 20

def text(value):
    return value if isinstance(value, str) else '\n'.join(str(line) for line in (value or []))

NATIVE_SHAPES = {'square', 'rsquare', 'circle', 'diamond', 'pentagon', 'hexagon', 'octagon', 'heart', 'gear'}

def quest_shape(value, inherit=False):
    if not value or value == 'default': return '' if inherit else 'rsquare'
    if not isinstance(value, str): raise ValueError('Invalid quest shape')
    if value in NATIVE_SHAPES or http_uri(value): return value
    if re.fullmatch(r'[a-z0-9_.-]+:[a-zA-Z0-9_./-]+', value) and '..' not in value and '://' not in value: return value
    if re.fullmatch(r'[a-z0-9_-]+', value): return 'ftbquests:textures/shapes/' + value + '/background.png'
    raise ValueError('Invalid custom shape resource')


def http_uri(value):
    if not isinstance(value, str) or len(value) > 2048 or re.search(r'\s', value): return False
    try:
        parsed = urlsplit(value)
        return parsed.scheme in ('http', 'https') and bool(parsed.hostname) and parsed.username is None and parsed.password is None
    except ValueError:
        return False


def click_action(value):
    if not isinstance(value, str) or len(value) > 8192 or any(c in value for c in '\x00\r\n'): raise ValueError('Invalid custom_click')
    if not value: return value
    kind, separator, target = value.partition(':')
    if not separator: kind, target = '', value
    if not target and kind != 'guide' or kind and not re.fullmatch(r'[a-z][a-z0-9_.-]{0,63}', kind): raise ValueError('Invalid custom_click protocol')
    if kind in ('http', 'https') and not http_uri(value): raise ValueError('Invalid custom_click URL')
    if kind == 'guide' and len(target) > 512: raise ValueError('Invalid guide click')
    if kind == 'custom' and (not re.fullmatch(r'[a-z0-9_.-]+:[a-zA-Z0-9_./-]+', target) or '..' in target or '://' in target): raise ValueError('Invalid custom click ID')
    if kind == 'curseforgepages' and not re.fullmatch(r'[a-zA-Z0-9_-]+[:/][a-zA-Z0-9_/-]+', target): raise ValueError('Invalid CurseForge page click')
    return value


def convert_map_image(raw):
    resource = raw.get('image', '')
    if not isinstance(resource, str) or not ((re.fullmatch(r'[a-z0-9_.-]+:[a-zA-Z0-9_./-]+', resource) and '..' not in resource and '://' not in resource) or http_uri(resource)):
        raise ValueError('Map illustration needs a texture or http/https image: ' + str(resource))
    click = raw.get('click', '')
    if click and not http_uri(click): raise ValueError('Map illustration click needs a supported http/https URI')
    return dict(resource=resource, x=float(raw.get('x', 0)), y=float(raw.get('y', 0)), width=float(raw.get('width', 1)), height=float(raw.get('height', 1)), rotation=float(raw.get('rotation', 0)), hover=[raw['hover']] if isinstance(raw.get('hover'), str) else raw.get('hover', []), uri=click, dev=bool(raw.get('dev', False)))

def convert(folder, mappings=None):
    mappings = mapping_rules(mappings)
    defaults = document(folder, 'file', True)
    book = {'format': 2, 'title': defaults.get('title') or 'Imported FTB 1.12 book', 'chapters': [], 'quests': [], 'lootTables': {}, 'lootSettings': {}}
    book.update(disableGui=bool(defaults.get('disable_gui', False)), gridScale=float(defaults.get('grid_scale', .5) or .5), defaultQuestShape=quest_shape(defaults.get('default_quest_shape')), hideFromItemBrowser=bool(defaults.get('default_quest_disable_jei', False)))
    book['emergencyItems'] = [dict(item(stack, mappings), type='item') for stack in defaults.get('emergency_items', [])]
    book['emergencyCooldownSeconds'] = seconds(defaults.get('emergency_items_cooldown'))
    book['dropLootCrates'] = bool(defaults.get('drop_loot_crates', False))
    book['noDrop'] = weights(defaults.get('loot_crate_no_drop', {}), {'passive': 4000, 'monster': 600, 'boss': 0})
    errors, references, raw_dependencies = [], {}, {}
    for path in documents(folder / 'reward_tables'):
        try:
            raw = read_document(path); name = code(path.stem)
            entries = []
            for reward in raw.get('rewards', []):
                entry = convert_reward(reward, defaults, nested=True, mappings=mappings); entry['weight'] = int(reward.get('weight', 1)); entries.append(entry)
            book['lootTables'][name] = entries
            book['lootSettings'][name] = {'rolls': max(1, int(raw.get('loot_size', 1))), 'emptyWeight': int(raw.get('empty_weight', 0))}
            if raw.get('loot_crate'):
                crate = raw['loot_crate']
                book['lootSettings'][name].update(alias=crate.get('string_id', ''), name=crate.get('item_name', ''), color=format(int(crate.get('color', 0xffffff)) & 0xffffff, '06X'), glow=bool(crate.get('glow', False)), drops=weights(crate.get('drops', {})))
        except (ValueError, OSError, TypeError, KeyError) as error:
            errors.append(str(path) + ': ' + str(error))
    chapter_dirs = sorted(p for p in (folder / 'chapters').iterdir() if p.is_dir())
    chapter_index = document(folder / 'chapters', 'index', True).get('index', [])
    if chapter_index:
        order = {code(v): i for i, v in enumerate(chapter_index)}
        chapter_dirs.sort(key=lambda p: order.get(code(p.name), len(order)))
    def register(source, target):
        if source in references:
            raise ValueError('Duplicate global object ID: ' + source)
        references[source] = target
    for chapter_dir in chapter_dirs:
        try:
            chapter = document(chapter_dir, 'chapter'); chapter_id = code(chapter_dir.name)
            book['chapters'].append({'id': chapter_id, 'title': chapter.get('title') or chapter_id, 'description': text(chapter.get('description', [])), 'parent': code(chapter['group']) if chapter.get('group') else '', 'defaultQuestShape': quest_shape(chapter.get('default_quest_shape'), True), 'alwaysInvisible': bool(chapter.get('always_invisible', False)), 'images': [convert_map_image(image) for image in chapter.get('images', [])]})
            if chapter.get('icon'):
                icon = item(chapter['icon'], mappings)
                book['chapters'][-1].update(icon=icon['item'], iconMeta=icon.get('meta', 0), iconNbt=icon.get('nbt', ''))
            register(chapter_id, 'chapter:' + chapter_id)
        except (ValueError, OSError, TypeError) as error:
            errors.append(str(chapter_dir) + ': ' + str(error)); continue
        for path in documents(chapter_dir, ('chapter',)):
            try:
                raw = read_document(path)


                requirement = raw.get('dependency_requirement', 'all_completed')
                if requirement not in ('all_completed', 'one_completed', 'all_started', 'one_started'):
                    raise ValueError('Unknown dependency requirement: ' + str(requirement))
                repeat = bool(raw.get('can_repeat', False))
                title = raw.get('title') or path.stem
                quest = {'id': code(path.stem), 'chapter': chapter_id, 'title': title,
                         'description': '\n'.join([text(raw.get('description', '')), text(raw.get('text', []))]), 'subtitle': raw.get('subtitle', ''), 'guidePage': raw.get('guide_page', ''), 'minWidth': int(raw.get('min_width', 0)), 'uri': '', 'customClick': click_action(raw.get('custom_click', '')),
                         'itemBrowser': 'inherit' if raw.get('disable_jei') in (None, 'default') else 'disabled' if tristate(raw['disable_jei']) else 'enabled',
                         'x': float(raw.get('x', 0)), 'y': float(raw.get('y', 0)), 'tags': raw.get('tags', []),
                         'shape': quest_shape(raw.get('shape'), True), 'size': float(raw.get('size', 1)),
                         'hideDependencyLines': bool(raw.get('hide_dependency_lines', False)), 'hideTextUntilComplete': bool(raw.get('hide_text_until_complete', False)), 'disableToast': bool(raw.get('disable_toast', False)),
                         'rewardMode': 'team' if repeat or defaults.get('default_reward_team') else 'individual',
                         'hidden': bool(raw.get('hide', False)), 'optional': bool(raw.get('optional', False)),
                         'repeatable': repeat, 'autoRepeat': repeat, 'anyTask': bool(raw.get('or_tasks', False)),
                         'anyDependency': requirement.startswith('one_'), 'dependencyState': 'started' if requirement.endswith('started') else 'completed',
                         'minDependencies': int(raw.get('min_required_dependencies', 0)), 'dependencies': [],
                         'tasks': [convert_task(t, i, defaults, repeat, mappings) for i, t in enumerate(raw.get('tasks', []))],
                         'rewards': [convert_reward(r, defaults, repeat, mappings=mappings) for r in raw.get('rewards', [])]}
                if raw.get('icon'):
                    icon = item(raw['icon'], mappings); quest['icon'] = icon['item']; quest['iconMeta'] = icon.get('meta', 0); quest['iconNbt'] = icon.get('nbt', '')
                register(quest['id'], quest['id'])
                for task in quest['tasks']:
                    if task['id'].startswith('ftb_'):
                        register(task['id'], 'task:' + quest['id'] + '/' + task['id'])
                raw_dependencies[quest['id']] = raw.get('dependencies', [raw['dependency']] if raw.get('dependency') else [])
                book['quests'].append(quest)
            except (ValueError, TypeError, KeyError, OSError) as error:
                errors.append(str(path) + ': ' + str(error))
    for quest in book['quests']:
        for dependency in raw_dependencies[quest['id']]:
            source = code(dependency.get('id') if isinstance(dependency, dict) else dependency)
            if source not in references:
                errors.append(quest['id'] + ': unknown dependency: ' + source)
            else:
                quest['dependencies'].append(references[source])
        for reward in quest['rewards']:
            try:
                resolve_choices(reward, book['lootTables'])
            except ValueError as error:
                errors.append(quest['id'] + ': ' + str(error))
    for name, entries in list(book['lootTables'].items()):
        resolved = json.loads(json.dumps(entries))
        try:
            for entry in resolved:
                resolve_choices(entry, book['lootTables'], (name,))
            book['lootTables'][name] = resolved
        except ValueError as error:
            errors.append(name + ': ' + str(error))
    if not book['quests']:
        errors.append('No supported quests found')
    if errors:
        raise ValueError('\n'.join(errors))
    return book

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('folder', type=Path, help='config/ftbquests/normal from a 1.12.2 instance')
    parser.add_argument('output', type=Path, help='New destination JSON; existing files are never overwritten')
    parser.add_argument('--mappings', type=Path, help='Explicit 1.12-to-1.7 identity, block metadata and achievement mapping JSON')
    args = parser.parse_args()
    try:
        if args.mappings and args.mappings.stat().st_size > 1024 * 1024:
            raise ValueError('Mappings file exceeds 1 MiB')
        mappings = json.loads(args.mappings.read_text(encoding='utf-8')) if args.mappings else None
        book = convert(args.folder, mappings)
        encoded = json.dumps(book, ensure_ascii=False, indent=2).encode('utf-8')
        if len(encoded) > 900000:
            raise ValueError('Converted book exceeds the 900 KB server limit')
        with args.output.open('xb') as out:
            out.write(encoded)
    except (OSError, ValueError) as error:
        parser.exit(1, str(error) + '\n')
    print(f"Converted {len(book['quests'])} quests. Review IDs and commands, then validate by /quests reload.")


if __name__ == '__main__':
    main()
