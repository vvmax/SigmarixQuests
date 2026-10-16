#!/usr/bin/env python3
"""Migrate one FTB 1.12 team progress file to a NEW Sigmarix world data file.

Run with the destination server stopped. Never edits source or overwrites output.
Members/leader must be supplied because FTB stores team membership in FTBLib,
separately from the ftbquests progress document.
"""
import argparse
import json
import uuid
from pathlib import Path
from import_ftb112 import code, read_document
from nbt_codec import IntTag, write_nbt


def stamp(value=1):
    return IntTag(value, 'L')


def migrate(book, source, members, leader=None, name='Imported FTB team'):
    members = list(dict.fromkeys(str(uuid.UUID(str(v))) for v in members))
    if not members or len(members) > 256:
        raise ValueError('Supply 1–256 explicit member UUIDs')
    leader = str(uuid.UUID(str(leader))) if leader else members[0]
    if leader not in members:
        raise ValueError('Leader must belong to members')
    if not 1 <= len(name) <= 32:
        raise ValueError('Team name must contain 1–32 characters')
    task_ids, reward_ids = {}, {}
    for q in book['quests']:
        for task in q['tasks']:
            if task['id'] in task_ids:
                raise ValueError('Ambiguous task ID: ' + task['id'])
            task_ids[task['id']] = (q, task)
        for i, reward in enumerate(q['rewards']):
            key = reward.get('id', 'r' + str(i))
            if key in reward_ids:
                raise ValueError('Ambiguous reward ID: ' + key)
            reward_ids[key] = (q, reward, key)
    state = {'tasks': {}, 'taskDone': {}, 'started': {}, 'rewardClaims': {}, 'completed': []}
    for old_id, raw_progress in source.get('Tasks', {}).items():
        key = code(old_id)
        if key not in task_ids:
            raise ValueError('Unknown saved task: ' + str(old_id))
        q, task = task_ids[key]
        value = int(raw_progress)
        if value < 0:
            raise ValueError('Negative saved progress')
        maximum = int(task.get('count', 1))
        if task['type'] == 'observation' and task.get('lookTicks', 0):
            state['tasks'][q['id'] + ':' + key + '_look'] = stamp(min(value, task['lookTicks']))
            value = maximum if value >= task['lookTicks'] else 0
        value = min(value, maximum)
        state['tasks'][q['id'] + ':' + key] = stamp(value)
        if value:
            state['started'][q['id']] = IntTag(1, 'b')
        if value >= maximum:
            state['taskDone'][q['id'] + ':' + key] = stamp()
    for q in book['quests']:
        complete = [q['id'] + ':' + t['id'] in state['taskDone'] for t in q['tasks']]
        if complete and (any(complete) if q.get('anyTask') else all(complete)):
            state['completed'].append(q['id'])
    def reward(old_id):
        key = code(old_id)
        if key not in reward_ids:
            raise ValueError('Unknown claimed reward: ' + str(old_id))
        return reward_ids[key]
    for old_id in source.get('ClaimedTeamRewards', []):
        q, r, key = reward(old_id)
        if (q.get('rewardMode', 'team') if r.get('mode', 'inherit') == 'inherit' else r['mode']) == 'individual':
            raise ValueError('Claimed team reward became individual: ' + str(old_id))
        state['rewardClaims'][q['id'] + ':' + key] = stamp()
    personal = {}
    for old_uuid, rewards in source.get('ClaimedPlayerRewards', {}).items():
        player = str(uuid.UUID(old_uuid)); ledger = personal.setdefault(player, {})
        for old_id in rewards:
            q, r, key = reward(old_id)
            scope = r.get('mode', 'inherit')
            if (q.get('rewardMode', 'team') if scope == 'inherit' else scope) != 'individual':
                raise ValueError('Claimed individual reward became shared: ' + str(old_id))
            ledger[q['id'] + ':' + key] = stamp()
    # Claims for unknown IDs or mismatched scopes fail; no rewards are issued during migration.
    team = str(uuid.uuid4())
    return {'data': {'players': {'team-' + team: state}, 'personalRewards': personal,
                     'teams': {team: {'leader': leader, 'name': name}},
                     'membership': {member: team for member in members}}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('book', type=Path, help='Converted quests.json with original ftb_ IDs')
    parser.add_argument('progress', type=Path, help='One FTBLib team ftbquests.dat/.nbt/.snbt')
    parser.add_argument('output', type=Path, help='NEW sigmarixquests.dat; never overwrite an existing world file')
    parser.add_argument('--members', nargs='+', required=True, help='Explicit destination player UUIDs')
    parser.add_argument('--leader')
    parser.add_argument('--team-name', default='Imported FTB team')
    args = parser.parse_args()
    try:
        book = json.loads(args.book.read_text(encoding='utf-8'))
        result = migrate(book, read_document(args.progress), args.members, args.leader, args.team_name)
        write_nbt(args.output, result)
    except (OSError, ValueError, KeyError, TypeError) as error:
        parser.exit(1, str(error) + '\n')
    print('Wrote a NEW world data file. Back up the destination world, stop the server, then install it as data/sigmarixquests.dat.')

if __name__ == '__main__':
    main()
