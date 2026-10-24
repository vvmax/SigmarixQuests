"""Audit generated definitions against captured registries and optional installed classes."""
import argparse,collections,hashlib,json,pathlib,re,zipfile
ROOT=pathlib.Path(__file__).resolve().parent.parent
def read(path):return json.loads(path.read_text(encoding='utf8'))
def main():
 p=argparse.ArgumentParser();p.add_argument('--mods',type=pathlib.Path);args=p.parse_args()
 config=ROOT/'config/sigmarixquests';root=read(config/'quests.json')
 fragments=[(f,read(f)) for f in sorted((config/'chapters').glob('*.json'))]
 quests=[q for _,f in fragments for q in f['quests']];byid={q['id']:q for q in quests}
 assert len(byid)==len(quests)
 names={r['name'].lstrip('\x01\x02') for r in read(ROOT/'evidence/server-registry.json')}
 classes=set()
 if args.mods:
  for file in args.mods.glob('*.jar'):
   with zipfile.ZipFile(file) as z:classes.update(n[:-6].replace('/','.') for n in z.namelist() if n.endswith('.class'))
 expectedClasses=set();hooks=set();stackEvidence=collections.Counter();known={(r['name'],r['meta']) for r in read(ROOT/'evidence/item-index.json')}
 for q in quests:
  assert re.fullmatch('[a-zA-Z0-9_.-]{1,64}',q['id'])
  assert all(dep in byid for dep in q['dependencies'])
  for t in q['tasks']:
   assert t['type']!='check'
   if t['type']=='item':
    assert t['target'].startswith('ore:') or t['target'] in names,(q['id'],t['target'])
    stackEvidence['ordinary_recipe_stack' if (t['target'],t.get('meta',0)) in known else 'registry_name_only_or_special_recipe']+=1
   if t['type']=='observation' and t.get('matchType')=='block_entity_class':expectedClasses.add(t['target'])
   if t['type']=='custom':
    hooks.add(t['target'])
    if t['target']=='sigma.dimension':expectedClasses.add(t['criterion'])
  assert q['icon'] in names
 vanilla={'net.minecraft.world.WorldProviderHell','net.minecraft.world.WorldProviderEnd','net.minecraft.world.WorldProviderSurface'}
 if args.mods:assert expectedClasses<=classes|vanilla,sorted(expectedClasses-classes-vanilla)
 depths={};active=set()
 def depth(key):
  assert key not in active,('cycle',key)
  if key in depths:return depths[key]
  active.add(key);value=1+max([depth(dep) for dep in byid[key]['dependencies']] or [0]);active.remove(key);depths[key]=value;return value
 for key in byid:depth(key)
 for q in quests:
  if not q.get('optional'):assert all(not byid[d].get('optional') for d in q['dependencies']),(q['id'],'mandatory depends on optional')
 files=[config/'quests.json',*[f for f,_ in fragments],*sorted((config/'scripts').glob('*.js'))]
 report=dict(quests=len(quests),sections=len(fragments),rootGroups=len(root['chapters']),tasks=sum(len(q['tasks']) for q in quests),taskTypes=dict(collections.Counter(t['type'] for q in quests for t in q['tasks'])),customHandlers=sorted(hooks),dependencyDepth=max(depths.values()),definitionBytes=sum(f.stat().st_size for f in files if f.suffix=='.json'),largestCompactChapter=max((f.stat().st_size,f.name) for f,_ in fragments),classTargets=sorted(expectedClasses),installedClassTargetsVerified=bool(args.mods),stackEvidence=dict(stackEvidence),limitations=['Registry names and class presence do not prove every recipe or machine mode is obtainable in the running game.','Live client/server smoke test with this campaign has not been performed; native 0.27 persistence and transport tests pass.'])
 (ROOT/'docs/audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
 coverage=read(ROOT/'coverage.json');mods=sorted({m for row in coverage for m in row['mods']})
 lines=['# Покрытие кампании','',f'{len(quests)} квестов; {len(fragments)} разделов. В карте заявлено {len(mods)} модовых пространств/систем, включая общие ветки. Это карта объясняемых механик, а не утверждение о прямой проверке каждого режима машины.','','| Раздел | Квестов | Моды и системы |','|---|---:|---|']
 for _,fragment in fragments:
  c=fragment['chapters'][0];rows=[r for r in coverage if r['chapter']==c['id']]
  lines.append('| '+c['title']+' | '+str(len(fragment['quests']))+' | '+', '.join(sorted({m for r in rows for m in r['mods']}))+' |')
 lines+=['','## Значение проверок','','`direct`: состояние исследования/персонажа/измерения, наблюдаемый класс блока или приём ресурса. `result`: обнаружение или сдача результата. Предмет как результат не доказывает правильность всей построенной установки.','','В каждой главе описаны основные рабочие процессы и характерные семейства устройств. Варианты одинаковых семян, цветов, блоков, пчёл и инструментов не перечислены по одному без механического смысла.','','## Не превращено в обязательные задания','','- CustomNPCs: отсутствуют фактические серверные ID сюжетных заданий, диалогов и фракций.','- Hats и клиентские технические моды: нет производственных целей.','- Universal Singularities: компрессия описана в Avaritia; отдельные рецепты, отсутствующие в снимке обычных крафтов, не сделаны обязательными.','- Правильная структура каждого многоблока, устойчивый баланс энергии и все настройки машин не определяются одним наличием предмета.','- Для косметических наборов и некоторых небольших дополнений покрытие обзорное; все их скрытые режимы и взаимодействия не заявлены проверенными.','']
 (ROOT/'docs/COVERAGE.md').write_text('\n'.join(lines),encoding='utf8')
 manifest={f.relative_to(ROOT).as_posix():hashlib.sha256(f.read_bytes()).hexdigest() for f in files}
 (ROOT/'manifest.json').write_text(json.dumps(dict(version='1.0.0',transport='Sigmarix Quests 0.27.0-SNAPSHOT, a4a9d16, bidirectional full-book chunk transport',files=manifest),ensure_ascii=False,indent=2),encoding='utf8')
 print(json.dumps(report,ensure_ascii=True))
if __name__=='__main__':main()
