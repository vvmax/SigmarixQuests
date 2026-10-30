"""Reviewed prerequisite graph. Each quest is assigned explicitly; no implicit list order.

DSL: prefix: prerequisite > child child; another_parent > child.
Comma-separated parents mean AND. / starts an absolute, cross-section ID.
An empty left side makes a root. A child may occur only once.
"""
byid={n['id']:n for n in quests}
old_edges={(n['id'],d) for n in quests for d in n['dependencies']}
reviewed=set()
def graph(prefix,text):
 def full(k):return k[1:] if k.startswith('/') else prefix+'_'+k
 for rule in text.split(';'):
  if not rule.strip():continue
  parents,children=rule.strip().split('>')
  deps=[full(k.strip()) for k in parents.split(',') if k.strip()]
  for child in children.split():
   key=full(child);assert key in byid,key;assert key not in reviewed,('duplicate review',key)
   byid[key]['dependencies']=deps[:];reviewed.add(key)

graph('start','>wood;wood>tools food;tools>light iron;iron>redstone diamond water;diamond,water>nether;nether>quartz;quartz>end;end>dragon')
graph('qt','/start_tools>book;book>terminal chest screen;terminal>water;screen>redstone;chest>rewards')
graph('store','/start_wood>chest;chest,/start_iron>iron;iron,/start_diamond>diamond;chest>drawers;drawers>controller lock compact')
graph('ic2','/start_iron>rubber plates;rubber,plates,/start_redstone>circuit;plates>casing;rubber>cables;plates,rubber>generator;circuit>batbox wrench;generator>eu geothermal solar;casing,circuit>furnace macerator extractor compressor metalformer recycler;recycler>scrap;casing,circuit>washer;washer>thermal;circuit>advanced;advanced,casing,/mek_reinforced>advanced_case;advanced>mfe;advanced_case,furnace>induction;advanced_case>reactor;reactor>chambers;thermal>fuel;advanced_case>heat;reactor,heat>steam;steam>fluidreactor;advanced>drill nano;batbox>charge;circuit>overclock transformer storage_upgrade eject;advanced_case,/mek_atomic,/eio_octadic,/ae_engineering>matter;matter>scanner;scanner>patternstorage;patternstorage>replicator;matter,nano>quantum')
graph('ie','/start_iron>hammer;hammer>coke hemp; coke>treated steel;steel>components;treated>waterwheel wind;components>dynamo capacitor wire;treated>workbench;workbench>blueprint;hemp>conveyor;components,blueprint>crusher press squeezer fermenter;wire,dynamo>rf;squeezer,fermenter>refinery;refinery>diesel;steel,components>arc excavator railgun')
graph('te','/ic2_casing,/ie_components>frame0;frame0>dynamo pulverizer furnace sawmill accumulator extruder;dynamo>cell;frame0,/mek_alloy>frame1;frame1>smelter transposer crucible induction charger augments;frame1,/mek_reinforced,/eio_octadic>frame2;frame2,/mek_atomic,/ic2_advanced>frame3;frame3>tesseract;tesseract>working_tesseract')
graph('mek','/start_redstone>infuser;infuser>alloy;alloy,/te_frame0>steelcase;steelcase>enrichment crusher smelter power cable factory electrolyzer sorter transport bins robit;alloy>reinforced;reinforced,electrolyzer>purification;electrolyzer>oxygen;steelcase,electrolyzer>chemicalinfuser;purification,chemicalinfuser>injection;reinforced>atomic;injection> dissolution;dissolution>washer;washer>crystallizer;steelcase>salt; salt>ports;atomic>teleportcore laser;teleportcore>miner;factory>assembler anchor')
graph('mek_extra','/mek_atomic>induction;/mek_steelcase>boiler heat')
graph('mg','/mek_alloy>heat wind bio solar;bio,/mek_electrolyzer>prc;prc>gas;/mek_atomic,/mek_ports>fusion;fusion>frame;frame,/mek_laser>laser;laser>output')
graph('mg_extra','/mek_extra_boiler>turbine;/mek_salt>condense;condense,/mek_electrolyzer>fuel')
graph('eio','/start_redstone>chassis;chassis>generator sag smelter capacitor conduits reservoir darksteel soulvial;smelter>conductive pulsating energetic soularium;energetic>vibrant;capacitor>double;double,vibrant>octadic;conduits>filters;reservoir>vat;vat>farming;soularium,soulvial>soulbinder;soulbinder>spawner obelisk;soulbinder,darksteel>travel;octadic>transceiver')
graph('bc','/start_iron>wrench;wrench>pipes engine landmarks; pipes>diamondpipe fluidpipes;engine,fluidpipes>pump;pump>refinery;landmarks,engine>quarry;engine>laser;laser>assembly;assembly>chipset;chipset>gates robot;robot>docking;landmarks>architect;architect,assembly>builder')
graph('br','/start_iron>fuel;/ie_steel>casing;casing>rods ports;rods,ports,fuel>steam;steam>turbine;/ic2_advanced_case>compact;compact>module1 advanced;module1>module2')
graph('sol','/ic2_solar>advanced;advanced>hybrid;hybrid>ultimate;ultimate>transform;transform>spectral;spectral>singular;singular>admin')
graph('gravi','/ic2_mfe>jet lappack;/ic2_drill>drill;/ic2_quantum>chest vajra;chest>flight;/ic2_advanced>assembler;assembler>sensors monitor convert')
graph('mfr','/start_iron>plastic;plastic>planter fertilizer sludge animals grinder net bio enchant rednet;grinder>spawner;animals>pink;pink>laser')
graph('util','/start_redstone>duct nodes;duct>retriever fluid flux;nodes>drum;/start_diamond>ritual;ritual>unstable;unstable>quarry flight;/ic2_advanced>industrial rotor')
graph('ae','/start_redstone>quartz presses;quartz>charger;charger,/start_quartz>fluix;fluix>pure acceptor;presses,fluix>inscriber;inscriber>logic engineering;inscriber,pure>calculation;engineering,/te_frame2>controller;logic,/eio_chassis>drive;logic>cell;acceptor,logic>terminal bus import level;terminal,calculation>patterns;engineering,/te_frame1>assembler;engineering>cpu;patterns,assembler,cpu>order;patterns,cpu,import>processing;processing>blocking;engineering>largecell;cell>cellworkbench;engineering>security;security>wireless;engineering>p2p;engineering,fluix>quantum;engineering>spatial')
graph('stuff','/ae_fluix>grow;/ae_inscriber>inscribe;inscribe>processors;/ae_patterns>encoder;/ae_controller>wireless;/ae_acceptor>view')
graph('sae','/ae_assembler>interface;/ae_processing>buffer;/ae_cpu>cpu;cpu>core modular;/ae_patterns>editor;/ae_order>stock maintenance;/ae_largecell>archive;/ae_wireless>wireless;wireless>magnet booster')
graph('aef','/ae_drive>cell;cell>import terminal;terminal>interface;interface>buffer discrete;cell>ecell;ecell>ecdrive;/ae_wireless>wireless;/ae_terminal,/mek_electrolyzer>gas;/ae_terminal,/tc_jar>essentia;essentia>ess_terminal provider;provider,/tc_infusion>arcane;ess_terminal,/ae_wireless>ess_wireless')
graph('flora','/ae_engineering,/bot_manasteel>processor;processor>mana charger daisy petal pool rune;processor,/bot_terrasteel>terra;processor,/bot_elementium>elven;processor,/bot_portal>brew enchant;processor>crate machine;machine>advanced;advanced,/bot_gaia2,/bm_demonic,/ae_largecell,/eio_octadic>gaia;advanced>greenhouse capacity')
graph('bn','/bm_altar,/ae_logic>relay;relay>altar alchemy generator capsule;generator>upgrade;upgrade>improve;capsule>wither;wither,/de_grinder,/eio_spawner,/mek_atomic>spawner')
graph('tc','/start_iron>book;book>scan table;table,scan>thaumium goggles;thaumium,/for_silk>fabric;thaumium>furnace;furnace>jar;jar>tubes;table,thaumium>alchemical;goggles,alchemical>infusion;infusion>stability pick golem focus mirror warp;golem>cores;goggles,jar>node;node>recharge;infusion,warp>void;void>eldritch;eldritch>pearl')
graph('bot','/start_iron>book;book>petals;petals>living;living>wand;wand>pool;pool>manasteel mana spread lens;manasteel,/tc_thaumium>runes;runes>seasons manarune;manasteel>spark;manasteel,/tc_infusion>plate;plate>terrasteel;terrasteel,/tc_void>portal;portal>elementium;elementium,terrasteel>gaia;gaia>gaia2;elementium>corporea redstring;manasteel>alchemy conjuration tools baubles;spark>sparks')
graph('flower','/bot_pool>endoflame hydroangeas thermalily;/bot_runes>gourmaryllis kekimurus entropinnyum rosaArcana agricarnation hopperhock rannuncarpus exoflame jadedAmaranthus bellethorn;/bot_portal>orechid')
graph('bm','/start_iron>altar;altar>divination slate;slate>runes sigils alchemy;runes>speed sacrifice;speed>imbued;imbued>ritual bound holding;ritual>diviner;bound,ritual>demonic;demonic>armour telepose spell;altar>incense;alchemy>reagents;demonic,reagents>demons;demons>transcendent')
graph('wit','/start_tools>plants;plants>oven;oven>fumes;fumes>mutandis;mutandis>altar;altar>upgrade kettle chalk taglock spinning brazier familiar hobgoblin;upgrade>distill;chalk>attune circle;taglock>poppet;kettle>dream brew;dream>nightmare;brew>brewbag;attune,brazier>demon;brazier>necromancy;altar>protection;nightmare>bloodcloth;bloodcloth>vampire;vampire>vampire_master;demon>werewolf;werewolf>werewolf_master')
graph('auto','/tc_thaumium>crystal;crystal>tank clock;clock>inventarium;inventarium>request golem;/tc_jar>essentia;crystal>experience')
graph('tt','/tc_thaumium>repair dislocate funnel;repair>enchant;/tc_focus>focus;/tc_fabric>cloth;cloth,/tc_pearl>ichor;ichor>tools armour;tools>bedrock')
graph('fm','/tc_scan>shards;shards>ink taint;/tc_thaumium>wand;/tc_infusion>cage;/tc_infusion,/bm_altar>blood')
graph('gado','/tc_golem>claw;claw>golem;/tc_jar>jars;/tc_node>node;node>compress aura')
graph('th','/tc_goggles>lenses;/tc_infusion>blood;blood>vat soul;/tc_node>dynamo node;vat>recombine;node,soul>vortex')
graph('wg','/tc_goggles>camera;/tc_thaumium>bag;/tc_node>cluster;cluster>gems;/tc_fabric>robe;/tc_pearl>primordial')
graph('tm','/tc_warp>wood;wood>metal;metal>goggles focus fortress disassembler')
graph('emt','/tc_thaumium,/ic2_circuit>scribe;scribe>goggles;/tc_infusion,/ic2_drill>drill;/tc_infusion,/gravi_jet>wings;/tc_focus>focus;/tc_void,/ic2_quantum>quantum')
graph('ba','/bm_slate>glass;glass>iron;iron>life;life>portable amulets sigils')
graph('for','/start_iron>scoop;scoop>apiary analyzer;apiary>centrifuge;centrifuge>carpenter squeezer frames;apiary>silk jelly;carpenter,jelly>alveary;alveary>climate;carpenter>farm;apiary>trees butterflies;trees>fruit;squeezer>ferment;ferment>distill;carpenter>thermionic backpack')
graph('gen','/for_analyzer,/ae_engineering,/eio_soularium,/ic2_advanced>processor;processor>apiary sampler mutagen dna protein;apiary>upgrades;sampler>template;template>imprint;mutagen,template>mutatron;mutatron,/binnie_splice>advanced;advanced,/mek_atomic>replicate')
graph('crop','/start_iron>minicio;minicio,/bot_runes>regular;regular>elements;regular,/bot_terrasteel,/bot_manarune>strong;strong,/tc_void>extreme;extreme,/bot_gaia>master;master,elements>metals precious mob alloys magic;master>boost;magic,/de_awakenedcore,/ava_catalyst>draconium')
graph('binnie','/for_analyzer>analyst;analyst>isolate database;isolate>sequence polymer;sequence,polymer>inoculate;inoculate>splice;splice>lab;/for_apiary>extra;/for_alveary>alveary;/for_trees>magic;magic> aura')
graph('bt','/start_tools>soil;soil>flowers;flowers>seed beds pigment database;/for_trees>saw;saw>carpentry;carpentry>panels')
graph('tic','/start_tools>manual;manual>patterns station grout;station>repair;grout>smeltery;smeltery>casting alloys;casting,alloys>forge;forge>hammer lumber weapons bolts;manual>heart travel')
graph('gc','/ic2_advanced>plates;plates>wafers;wafers>oxygen collector fuel workbench;workbench,fuel,oxygen>rocket;rocket>moon;moon>meteor seal dungeon;dungeon>mars;mars>desh thermal;desh>tier3;tier3>asteroids;asteroids>miner beam telepad')
graph('gc_extra','/gc_rocket>cargo;/gc_rocket>station')
graph('home','/start_tools>chisel carpenter lighting library display armour')
graph('decor','/start_tools>travel;/tc_fabric>fabric;fabric>robe')
graph('div','/start_diamond>realmite;realmite>advanced aquatic nether;advanced>shards;shards>divine clock;divine>eden;eden>eden_resources;eden_resources>wildwood;wildwood>wildwood_resources;wildwood_resources>apalachia;apalachia>apalachia_resources;apalachia_resources>skythern;skythern>skythern_resources;skythern_resources>mortum;mortum>mortum_resources')
graph('div_side','/div_advanced>iceika arcana vethea')
graph('biome','/start_tools>finder wood;finder>gems;gems>amethyst;/start_nether>netherwood netherfood crystal')
graph('de','/mek_atomic>tablet;tablet>core;core>infuser grinder chest;core,/bm_imbued>wyvern;wyvern>armour; /start_dragon>heart;heart,core>awaken;awaken,/bot_gaia,/tc_void,/bm_demonic,/wit_demon>awakenedcore;awakenedcore>draconic storage crystals teleport;armour>modifier;draconic>chaos;chaos,/ava_catalyst>chaoticcore;awakenedcore>reactor')
graph('ava','/de_awakenedcore>matrix;matrix>table;table,/ic2_quantum,/mek_atomic,/ae_largecell,/bot_gaia,/tc_void>neutron;neutron>ingot;ingot>compressor;compressor>singularity;table>food;ingot,food,/tc_pearl,/bm_demons,/wit_demon,/for_jelly,/te_frame3>catalyst;catalyst,singularity>infinity;table,/ae_patterns>encoder;encoder,/ae_cpu>assembler;infinity>pick armour;infinity,/de_chaoticcore,/sol_admin>photonic;infinity>token;catalyst,/tc_pearl>researchbook')
graph('ava_extra','/ava_compressor>compressed;/ava_infinity>infinite;/ava_table,/ae_patterns>autocrafter')
graph('project','/te_smelter,/ae_processing>smelt;/ae_processing,/stuff_inscribe>processors;/ae_processing,/bm_imbued>slates;/ae_processing,/bot_runes>runes;/ae_processing,/mek_reinforced>alloys;/ae_processing,/for_silk>silk;/ae_processing,/bot_terrasteel>terrasteel;/ae_processing,/ava_ingot>neutron')

assert reviewed==set(byid),('quests without explicit review',sorted(set(byid)-reviewed))
# Experience means points, not levels. Routine crafting, side tasks and optional paths
# grant no XP; these sixteen milestones give 48 points across the whole campaign.
milestones='start_diamond ic2_eu ie_steel mek_injection ae_order tc_infusion bot_terrasteel bm_imbued wit_attune for_alveary gen_advanced gc_moon de_awakenedcore ava_neutron ava_infinity ava_photonic'.split()
for n in quests:n['rewards']=[dict(id='experience',type='xp',count=3)] if n['id'] in milestones else []
new_edges={(n['id'],d) for n in quests for d in n['dependencies']}
(ROOT/'docs/progression-review.json').write_text(json.dumps(dict(reviewedQuests=len(reviewed),oldEdges=len(old_edges),newEdges=len(new_edges),removedEdges=len(old_edges-new_edges),addedEdges=len(new_edges-old_edges),xpPoints=48,xpMilestones=milestones),ensure_ascii=False,indent=2),encoding='utf8')
