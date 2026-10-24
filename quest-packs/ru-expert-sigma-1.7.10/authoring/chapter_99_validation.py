# Corrections are explicit and based on the captured server stack index / installed bytecode.
byid={node['id']:node for node in quests}
def replace_goal(key,spec):
 node=byid[key];node['tasks']=proof(spec)
 for n,t in enumerate(node['tasks']):t['id']='goal' if len(node['tasks'])==1 else 'goal_'+str(n+1)
 first=next((t for t in node['tasks'] if t['type']=='item'),None)
 if first:node['icon']=first['target'];node['iconMeta']=first.get('meta',0)
 else:node['icon']=next(c['icon'] for c in chapters if c['id']==node['chapter']);node['iconMeta']=0
 for row in coverage:
  if row['quest']==key:row['taskTypes']=[t['type'] for t in node['tasks']];row['verification']='direct' if all(t['type'] in ['observation','custom'] for t in node['tasks']) else 'result'

for key,spec in {
 'ie_coke':'ImmersiveEngineering:stoneDecoration:1*27',
 'ie_waterwheel':'ImmersiveEngineering:woodenDevice:1',
 'ie_wind':'ImmersiveEngineering:woodenDevice:2',
 'ie_dynamo':'ImmersiveEngineering:metalDevice:9',
 'ie_capacitor':'ImmersiveEngineering:metalDevice:1',
 'ie_workbench':'ImmersiveEngineering:woodenDevice:5',
 'ie_conveyor':'ImmersiveEngineering:metalDevice:11*8',
 'te_cell':'ThermalExpansion:Cell:1',
 'te_accumulator':'ThermalExpansion:Machine:8',
 'te_extruder':'ThermalExpansion:Machine:7',
 'te_induction':'ThermalExpansion:Machine:6',
 'te_charger':'ThermalExpansion:Machine:10',
 'te_working_tesseract':'ThermalExpansion:Tesseract',
 'mek_smelter':'Mekanism:MachineBlock:10',
 'mek_chemicalinfuser':'Mekanism:MachineBlock2:2',
 'mek_salt':'Mekanism:BasicBlock:14;Mekanism:BasicBlock2*8',
 'mek_ports':'Mekanism:BasicBlock:15*2',
 'mek_sorter':'Mekanism:MachineBlock:15',
 'mek_transport':'Mekanism:PartTransmitter:12*8',
 'mek_laser':'Mekanism:MachineBlock2:13;Mekanism:MachineBlock2:14',
 'mek_anchor':'Mekanism:SpeedUpgrade;Mekanism:EnergyUpgrade;Mekanism:GasUpgrade',
 'mg_bio':'MekanismGenerators:Generator:4',
 'mg_solar':'MekanismGenerators:Generator:5',
 'mg_laser':'Mekanism:MachineBlock2:13;Mekanism:MachineBlock2:14;MekanismGenerators:ReactorGlass:1',
 'tic_alloys':'TConstruct:materials:14*8',
 'tc_pearl':'Thaumcraft:ItemEldritchObject:3',
}.items():replace_goal(key,spec)
for key,cls in {'crusher':'Crusher','press':'MetalPress','squeezer':'Squeezer','fermenter':'Fermenter','refinery':'Refinery','diesel':'DieselGenerator','arc':'ArcFurnace','excavator':'Excavator'}.items():
 replace_goal('ie_'+key,'tile:blusunrize.immersiveengineering.common.blocks.metal.TileEntity'+cls)
 byid['ie_'+key]['description']+=' После формирования направьте взгляд на рабочий блок установки на две секунды. Проверяется тип установленного механизма.'
byid['te_induction']['title']='Ледниковый осадитель'
byid['te_induction']['description']='Подайте воду и энергию для получения снега, льда и других поддерживаемых охлаждённых материалов. Проверьте выбранный рецепт и оба входа.'
byid['mek_anchor']['title']='Улучшения машин'
byid['mek_anchor']['description']='Сравните улучшения скорости, энергии и газа. Ускорение без энергосбережения может резко поднять расход; газовое улучшение имеет смысл только для совместимой машины.'
byid['tic_alloys']['description']='Получите алюминиевую латунь для литейных форм. Перед загрузкой металлов проверьте пропорции; остатки разных расплавов могут дать нежелательный сплав.'

# Explicit cross-mod milestones. These add to, rather than erase, ordinary local progression.
def gate(key,*deps):
 node=byid[key]
 for dep in deps:
  if dep not in node['dependencies']:node['dependencies'].append(dep)
for key,deps in {
 'te_frame0':['ie_components'],
 'mek_steelcase':['te_frame0'],
 'ic2_advanced_case':['mek_reinforced'],
 'te_frame1':['mek_alloy'],
 'te_frame2':['mek_reinforced','eio_octadic'],
 'te_frame3':['mek_atomic','ic2_advanced'],
 'ae_controller':['te_frame2'],
 'ae_drive':['eio_chassis'],
 'ae_assembler':['te_frame1'],
 'ic2_matter':['mek_atomic','eio_octadic','ae_engineering'],
 'tc_fabric':['for_silk'],
 'bot_runes':['tc_thaumium'],
 'bot_plate':['tc_infusion'],
 'bot_portal':['tc_void'],
 'crop_regular':['bot_runes'],
 'crop_strong':['bot_terrasteel','bot_manarune'],
 'crop_extreme':['tc_void'],
 'crop_master':['bot_gaia'],
 'crop_metals':['crop_master'],
 'crop_draconium':['de_awakenedcore','ava_catalyst'],
 'gen_processor':['ae_engineering','eio_soularium','ic2_advanced'],
 'gen_advanced':['binnie_splice'],
 'gen_replicate':['mek_atomic'],
 'flora_gaia':['bot_gaia2','bm_demonic','ae_largecell','eio_octadic'],
 'bn_spawner':['de_grinder','eio_spawner','mek_atomic'],
 'de_wyvern':['bm_imbued'],
 'de_awakenedcore':['bot_gaia','tc_void','bm_demonic','wit_demon'],
 'ava_neutron':['ic2_quantum','mek_atomic','ae_largecell','bot_gaia','tc_void'],
 'ava_catalyst':['tc_pearl','bm_demons','wit_demon','for_jelly','te_frame3'],
 'de_chaoticcore':['ava_catalyst'],
 'ava_photonic':['de_chaoticcore','sol_admin'],
}.items():gate(key,*deps)
# Split low-tier crops off before master stone; resource seeds stay late.
byid['crop_elements']['dependencies']=['crop_regular']
byid['crop_metals']['dependencies']=['crop_master','crop_elements']
# Distinct magical/tinkering equipment is optional and must not delay first void metal.
byid['tc_void']['dependencies']=['tc_infusion','tc_warp']
# Node/research utility branches are not prerequisites of the alchemical main line.
byid['tc_node']['dependencies']=['tc_goggles','tc_jar']
byid['tc_warp']['dependencies']=['tc_infusion']

# All captured registry names must exist. Multi-variant families keep exact metadata;
# equipment with durability accepts any damage. NBT flower types remain strict subsets.
for node in quests:
 for t in node['tasks']:
  if t['type']=='item' and (t['target'] in ['IC2:itemBatRE','IC2:reactorUraniumSimple','Thaumcraft:ItemPickaxeElemental'] or t['target'].startswith('IC2:itemTool') or t['target'].startswith('GraviSuite:') or t['target'].startswith('GalacticraftCore:item.oxygenTank')):t['ignoreDamage']=True
 for row in coverage:
  if row['quest']==node['id']:row['mechanic']=node['title']
