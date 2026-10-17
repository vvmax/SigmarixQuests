#!/usr/bin/env python3
"""Validate the distributed book, links, evidence and original image hashes (Python 3)."""
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

def main():
    root = Path(__file__).resolve().parent
    folder = root / 'config' / 'sigmarixquests'
    header = json.loads((folder / 'quests.json').read_text(encoding='utf-8'))
    chapters, quests = {}, {}
    for file in sorted((folder / header['chapterDirectory']).glob('*.json')):
        document = json.loads(file.read_text(encoding='utf-8'))
        local = {c['id'] for c in document['chapters']}
        for chapter in document['chapters']:
            assert chapter['id'] not in chapters, 'Duplicate chapter: ' + chapter['id']
            chapters[chapter['id']] = chapter
        for quest in document['quests']:
            assert quest['id'] not in quests, 'Duplicate quest: ' + quest['id']
            assert quest['chapter'] in local, 'Foreign chapter: ' + quest['id']
            quests[quest['id']] = quest
    visited, visiting = set(), set()
    def visit(identifier):
        assert identifier in quests, 'Missing dependency: ' + identifier
        assert identifier not in visiting, 'Dependency cycle: ' + identifier
        if identifier in visited:
            return
        visiting.add(identifier)
        for dependency in quests[identifier].get('dependencies', []):
            visit(dependency)
        visiting.remove(identifier)
        visited.add(identifier)
    for identifier in quests:
        visit(identifier)
    evidence = json.loads((root / 'registry-evidence.json').read_text(encoding='utf-8'))['targets']
    known = {(r['id'], r['meta'], r['ignore_damage']) for r in evidence}
    manual = automatic = 0
    for quest in quests.values():
        for task in quest['tasks']:
            assert not task.get('consume', False), 'Consumes items: ' + quest['id']
            if task['type'] == 'item':
                automatic += 1
                assert (task['target'], task['meta'], task.get('ignoreDamage', False)) in known, 'Unverified target: ' + quest['id']
            else:
                manual += 1
                assert task['type'] == 'check' and task['manual'], 'Unmarked manual task'
                assert 'Ручная проверка:' in quest['description'], 'Manual instructions missing'
    images = json.loads((root / 'illustration-sources.json').read_text(encoding='utf-8'))
    pack = root / 'resourcepacks' / 'Sigmarix-Quest-Illustrations-1.7.10.zip'
    with ZipFile(pack) as archive:
        assert archive.testzip() is None, 'Damaged resource pack'
        for image in images:
            assert hashlib.sha256(archive.read(image['pack_entry'])).hexdigest() == image['sha256'], image['pack_entry']
        references = [i['resource'] for c in chapters.values() for i in c.get('images', [])]
        references += [e['resource'] for q in quests.values() for p in q.get('richPages', []) for e in p['elements'] if e['type'] == 'image']
        for reference in references:
            namespace, path = reference.split(':', 1)
            assert 'assets/' + namespace + '/' + path in archive.namelist(), 'Missing image: ' + reference
    assert len(chapters) == 6 and len(quests) == 180
    assert automatic == 139 and manual == 41 and len(images) == 17
    print('PASS: 6 chapters, 180 quests, 139 item tasks, 41 explicit manual tasks, 17 original images; links and hashes valid.')

if __name__ == '__main__':
    main()
