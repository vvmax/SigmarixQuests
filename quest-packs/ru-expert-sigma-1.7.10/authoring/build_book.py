"""Author the Russian Sigma campaign; no installed instance or world is modified."""
import json,pathlib,collections,re,csv,hashlib
ROOT=pathlib.Path(__file__).resolve().parent.parent
REG=json.loads((ROOT/'evidence/server-registry.json').read_text(encoding='utf8'))
INDEX=json.loads((ROOT/'evidence/item-index.json').read_text(encoding='utf8'))
NAMES={x['name'].lstrip('\x01\x02') for x in REG}
BLOCKS={x['name'][1:] for x in REG if x['name'].startswith('\x01')}
LABELS={(x['name'],x['meta']):x['display'] for x in INDEX}
chapters=[];quests=[];coverage=[]; current=None;last=None;section_root=None

def ref(value):
 m=re.fullmatch(r'(.+):(\d+)',value)
 return (m[1],int(m[2])) if m else (value,0)
def item(value,count=1,consume=False,**kw):
 name,meta=ref(value)
 return dict(type='item',target=name,meta=meta,count=count,consume=consume,**kw)
def observe(value,**kw):
 name,meta=ref(value)
 return dict(type='observation',target=name,meta=meta,lookTicks=40,radius=6,sticky=True,**kw)
def custom(handler,criterion='',count=1,**kw):
 return dict(type='custom',target=handler,criterion=criterion,count=count,**kw)
def research(key):return custom('sigma.tc_research',key)
def dimension(provider):return custom('sigma.dimension',provider)
def energy(kind,count):return dict(type=kind,count=count)
def delivered(value,count):return item(value,count,consume=True)
def chapter(id,title,mods,icon='minecraft:book',parent='technology',intro=''):
 global current,last,section_root
 current=dict(id=id,title=title,description=intro,mods=mods,parent=parent,icon=icon)
 chapters.append(current);last=None;section_root=None
def branch(root=None):
 global last,section_root
 last=root;section_root=root
def q(key,title,text,proof,deps=None,guide='',optional=False,verify='result',tags=None):
 global last
 assert current and not any(x['id']==key for x in quests),key
 if isinstance(proof,str):proof=item(proof)
 if isinstance(proof,dict):proof=[proof]
 tasks=[]
 for n,t in enumerate(proof):
  t=dict(t);t.setdefault('id','goal' if len(proof)==1 else 'goal_'+str(n+1));tasks.append(t)
 dependencies=([last] if last else []) if deps is None else ([deps] if isinstance(deps,str) else list(deps))
 icon=next((t['target'] for t in tasks if t['type']=='item' and not t['target'].startswith('ore:')),current['icon'])
 meta=next((t.get('meta',0) for t in tasks if t.get('target')==icon),0)
 node=dict(id=key,title=title,description=text,chapter=current['id'],icon=icon,iconMeta=meta,dependencies=dependencies,tasks=tasks,rewards=[],autoComplete=True,rewardMode='individual',optional=optional)
 if guide:node['pages']=[guide]
 if tags:node['tags']=tags
 quests.append(node);last=key
 coverage.append(dict(quest=key,chapter=current['id'],mods=current['mods'],mechanic=title,verification=verify,taskTypes=[t['type'] for t in tasks]))
 return key

def proof(spec):
 result=[]
 for raw in spec.split(';'):
  raw=raw.strip();m=re.fullmatch(r'(.*)\*(\d+)',raw)
  value,count=(m[1],int(m[2])) if m else (raw,1)
  if value.startswith('@'):result.append(observe(value[1:]))
  elif value.startswith('tile:'):result.append(dict(type='observation',target=value[5:],matchType='block_entity_class',ignoreDamage=True,lookTicks=40,radius=6))
  elif value.startswith('research:'):result.append(research(value[9:]))
  elif value.startswith('visit:'):result.append(dimension(value[6:]))
  elif value.startswith('kill:'):result.append(dict(type='kill',target=value[5:],count=count))
  elif value.startswith('rf:'):result.append(energy('energy',int(value[3:])))
  elif value.startswith('eu:'):result.append(dict(type='ic2_energy',count=int(value[3:]),energyTier=1))
  elif value.startswith('mana:'):result.append(energy('botania_mana',int(value[5:])))
  elif value.startswith('fluid:'):result.append(dict(type='fluid',target=value[6:],count=count,consume=True))
  elif value.startswith('!'):result.append(delivered(value[1:],count))
  else:result.append(item(value,count))
 return result
def seq(prefix,data,entry=None,optional=False):
 branch(entry)
 for line in data.strip().splitlines():
  if not line.strip() or line.lstrip().startswith('#'):continue
  parts=re.split(r'\|(?!Core:|Transport:|Factory:|Silicon:|Robotics:|Energy:|Builders:)',line);assert len(parts) in [4,5],line
  key,title,goal,text=parts[:4];guide=parts[4] if len(parts)==5 else ''
  q(prefix+'_'+key,title,text,proof(goal),guide=guide,optional=optional)

for file in sorted((ROOT/'authoring').glob('chapter_*.py')):exec(compile(file.read_text(encoding='utf8'),str(file),'exec'))

groups=[dict(id='foundations',title='Начало и устройство базы',icon='minecraft:crafting_table'),dict(id='technology',title='Промышленность и энергия',icon='IC2:itemPartCircuit'),dict(id='networks',title='Сети, логистика и автоматизация',icon='appliedenergistics2:item.ItemMultiMaterial',iconMeta=24),dict(id='magic',title='Магия и исследования',icon='Thaumcraft:ItemThaumonomicon'),dict(id='nature',title='Природа, селекция и ресурсы',icon='Forestry:apiculture'),dict(id='exploration',title='Приключения и измерения',icon='minecraft:compass'),dict(id='integration',title='Общие проекты и финал',icon='Avaritia:Resource',iconMeta=5)]
ids={q['id'] for q in quests};chapterids={c['id'] for c in chapters}|{g['id'] for g in groups}
assert len(chapterids)==len(chapters)+len(groups)
missing=[]
for node in quests:
 for dep in node['dependencies']:
  if dep not in ids:missing.append((node['id'],dep))
 for t in node['tasks']:
  if t['type'] in ['item','observation','interaction'] and t.get('matchType','block_id')=='block_id':
   name=t['target']
   if not name.startswith('ore:') and name not in NAMES:missing.append((node['id'],'item '+name))
   if t['type'] in ['observation','interaction'] and name not in BLOCKS:missing.append((node['id'],'not block '+name))
 for name in [node['icon']]:
  if name not in NAMES:missing.append((node['id'],'icon '+name))
assert not missing,missing

byid={q['id']:q for q in quests};done=set();visiting=set()
def visit(id):
 assert id not in visiting,('cycle',id,sorted(visiting))
 if id in done:return
 visiting.add(id)
 for dep in byid[id]['dependencies']:visit(dep)
 visiting.remove(id);done.add(id)
for key in ids:visit(key)

# Native UI defaults expose internal IDs. Give every task a readable Russian label.
for node in quests:
 for task in node['tasks']:
  kind=task['type'];count=task.get('count',1)
  if kind=='item':
   label=LABELS.get((task['target'],task.get('meta',0)),'')
   if not re.search('[А-Яа-яЁё]',label):label=node['title']
   task['title']=('Сдать: ' if task.get('consume') else 'Получить: ')+label+((' × '+str(count)) if count>1 else '')
  elif kind in ['energy','ic2_energy','botania_mana']:
   task['title']='Передать '+str(count)+' '+dict(energy='RF',ic2_energy='EU',botania_mana='маны')[kind]
  elif kind=='fluid':task['title']='Передать жидкость: '+str(count)+' mB'
  elif kind=='observation':task['title']='Осмотреть установку';task['description']='Смотрите на блок нужного механизма с расстояния до шести блоков две секунды. Проверяется тип блока, а не его текущая производительность.'
  elif kind=='custom':task['title']='Подтвердить: '+node['title']
  elif kind=='kill':task['title']='Победить: '+node['title']
  if task.get('nbt') and task.get('target')==node['icon']:node['iconNbt']=task['nbt']

# Lay out the actual DAG, rather than drawing an unrelated serpentine list.
# Cross-section prerequisites remain real gates but do not create empty columns.
for c in chapters:
 nodes=[q for q in quests if q['chapter']==c['id']]
 local={n['id']:n for n in nodes};ranks={};lanes={}
 def rank(key):
  if key not in ranks:ranks[key]=max([rank(d)+1 for d in local[key]['dependencies'] if d in local] or [0])
  return ranks[key]
 for n in nodes:rank(n['id'])
 column_offset=0
 for depth in sorted(set(ranks.values())):
  layer=[n for n in nodes if ranks[n['id']]==depth]
  def barycentre(n):
   p=[lanes[d] for d in n['dependencies'] if d in lanes]
   return sum(p)/len(p) if p else nodes.index(n)
  layer.sort(key=barycentre)
  # Broad independent branches wrap into parallel columns. They remain siblings:
  # no invented dependencies are added just to produce a compact drawing.
  for lane,n in enumerate(layer):
   column,row=divmod(lane,6);rows=min(6,len(layer)-column*6)
   lanes[n['id']]=row-(rows-1)/2
   n.update(x=(column_offset+column)*3.2,y=lanes[n['id']]*2.5)
  column_offset+=(len(layer)+5)//6
 count=sum(q['chapter']==c['id'] for q in quests)
 c['description']+=(('\n\n' if c['description'] else '')+str(count)+' заданий. Ключевые предметы только обнаруживаются; производственные испытания принимают указанные расходуемые образцы. Награды не выдают материалы следующих ступеней.')
 c.pop('mods')

config=ROOT/'config/sigmarixquests';folder=config/'chapters'
book=dict(format=2,title='Sigmarix — от первой мастерской до бесконечности',chapterDirectory='chapters',defaultConsumeItems=False,defaultAutoClaim='disabled',gridScale=.5,theme=dict(background='EE151D29',panel='EE263347',text='FFF0EBDD',available='FF498BB8',complete='FFE4B75C',claimed='FF55A779',locked='FF566174',questSpacing=1.15),styles=[dict(selector='within:magic',color='FF9677C1'),dict(selector='within:nature',color='FF74A66A'),dict(selector='within:integration',shape='hexagon')],chapters=groups,quests=[])
(config/'quests.json').write_text(json.dumps(book,ensure_ascii=False,separators=(',',':')),encoding='utf8')
expected={str(n+1).zfill(2)+'_'+c['id']+'.json' for n,c in enumerate(chapters)}
for file in folder.glob('*.json'):
 if re.fullmatch(r'\d{2}_[a-zA-Z0-9_.-]+\.json',file.name) and file.name not in expected:file.unlink()
for n,c in enumerate(chapters):
 part=dict(format=2,chapters=[c],quests=[q for q in quests if q['chapter']==c['id']])
 (folder/(str(n+1).zfill(2)+'_'+c['id']+'.json')).write_text(json.dumps(part,ensure_ascii=False,separators=(',',':')),encoding='utf8')
(ROOT/'coverage.json').write_text(json.dumps(coverage,ensure_ascii=False,indent=2),encoding='utf8')
with (ROOT/'quest-index.csv').open('w',encoding='utf-8-sig',newline='') as fp:
 w=csv.writer(fp,delimiter=';');w.writerow(['ID','Раздел','Квест','Проверка','Типы задач','Зависимости'])
 for row in coverage:
  node=byid[row['quest']];w.writerow([row['quest'],node['chapter'],node['title'],row['verification'],','.join(row['taskTypes']),','.join(node['dependencies'])])
print(json.dumps(dict(quests=len(quests),chapters=len(chapters),manual=sum(t['type']=='check' for q in quests for t in q['tasks']),taskTypes=dict(collections.Counter(t['type'] for q in quests for t in q['tasks'])),definitionBytes=sum(f.stat().st_size for f in [config/'quests.json',*folder.glob('*.json')])),ensure_ascii=True))
