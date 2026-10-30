chapter('practical_projects','Практические производственные испытания',['appliedenergistics2','Mekanism','IC2','Botania','Forestry'],'minecraft:comparator',parent='integration',intro='Здесь принимаются расходуемые пробные партии. Книга проверяет сдачу результата; правильность трассировки труб и постоянную работу фабрики игрок проверяет запуском собственного заказа.')
seq('project','''
smelt|Запрос плавки|!minecraft:glass*64|Закажите стекло через шаблон обработки. После завершения проверьте, что машина и процессор снова свободны; затем сдайте партию.
processors|Три линии процессоров|!appliedenergistics2:item.ItemMultiMaterial:22*16;!appliedenergistics2:item.ItemMultiMaterial:23*16;!appliedenergistics2:item.ItemMultiMaterial:24*16|Закажите три типа процессоров. Разделите отпечатки, прессы и финальную сборку; исключите попадание прессов в расходуемый вход.
slates|Плитки разных ступеней|!AWWayofTime:blankSlate*8;!AWWayofTime:reinforcedSlate*8;!AWWayofTime:imbuedSlate*8|Настройте отдельные результаты по уровню плитки. Не позволяйте общей импортной шине забирать заготовку до нужной стадии.
runes|Партия стихийных рун|!Botania:rune:0*8;!Botania:rune:1*8;!Botania:rune:2*8;!Botania:rune:3*8|Закажите четыре набора рун. Учитывайте возвращаемые ингредиенты и ресурс маны.
alloys|Партия промышленных компонентов|!Mekanism:AtomicAlloy*16;!EnderIO:itemBasicCapacitor:2*4;!IC2:itemPartCircuitAdv*16|Настройте взаимосвязанные линии так, чтобы результат каждой возвращался в сеть, а буферы не накапливали бесконечный запас.
silk|Возобновляемая магическая одежда|!Forestry:craftingMaterial:3*8;!Thaumcraft:ItemResource:7*8|Организуйте полный путь от шёлковой пасеки до зачарованной ткани и сдайте образцы каждого этапа.
terrasteel|Террасталь по запросу|!Botania:manaResource:4*8|Закажите партию после наполнения бассейнов. Если заказ остановился, отдельно проверьте ману, входы, завершение и обратный импорт.
neutron|Финальная материальная база|!Avaritia:Resource:4*4|Обеспечьте возобновляемый сбор нейтрония и сдайте небольшую партию. Не выдавайте складской запас за скорость производства: оцените время восстановления.
''',entry='ae_processing')

# Return to the appropriate native section for optional expansions; no unrelated chain gates.
current=next(c for c in chapters if c['id']=='mekanism')
seq('mek_extra','''
induction|Индукционная матрица|Mekanism:BasicBlock2:1*16;Mekanism:BasicBlock2:2*2;Mekanism:BasicBlock2:3;Mekanism:BasicBlock2:4|Соберите оболочку, установите ячейку и поставщик. Ячейка определяет объём, поставщик — мощность ввода и вывода.
boiler|Термоэлектрический бойлер|Mekanism:BasicBlock2:7*16;Mekanism:BasicBlock2:8*2;Mekanism:BasicBlock2:5|Соберите замкнутый водяной и паровой контур. Подайте тепло через подходящее соединение и обеспечьте возврат воды.
heat|Передача тепла|Mekanism:PartTransmitter:18*8;Mekanism:MachineBlock3:4|Проверьте теплопотери и настройки нагревателя. Электрическая сеть, газовая труба и теплопровод решают разные задачи.
''',entry='mek_atomic')
current=next(c for c in chapters if c['id']=='mek_power')
seq('mg_extra','''
turbine|Промышленная турбина|MekanismGenerators:Generator:10*16;MekanismGenerators:Generator:7*4;MekanismGenerators:TurbineBlade*8;MekanismGenerators:Generator:9|Соберите ротор, лопатки, распределение пара и катушки. Проверьте ограничения по оборотам и подаче пара.
condense|Возврат воды|MekanismGenerators:Generator:13*4;MekanismGenerators:Generator:12*2|Обеспечьте конденсацию и обратную подачу воды в бойлер. Потери воды должны покрываться отдельным источником.
fuel|Топливо термоядерной установки|Mekanism:MachineBlock3:1;Mekanism:BasicBlock:14|Организуйте испарение и нейтронную активацию для топливной цепочки. Разделите подготовку дейтерия, трития и их смеси по рецептам установленных машин.
''',entry='mg_frame')
current=next(c for c in chapters if c['id']=='galacticraft')
seq('gc_extra','''
cargo|Грузовая логистика|GalacticraftCore:tile.cargo*2|Настройте загрузку и разгрузку. Проверяйте направление, питание и свободные слоты перед отправкой груза.
station|Орбитальная инфраструктура|GalacticraftCore:tile.spinThruster;GalacticraftCore:tile.telemetry|Подготовьте станцию и телеметрию после первой космической экспедиции. Список необходимых ресурсов для создания станции смотрите в интерфейсе полёта.
''',entry='gc_moon')
current=next(c for c in chapters if c['id']=='building')
seq('decor','''
travel|Гардероб путешественника|TravellersGear:armorstand;TravellersGear:simpleGear|Разделите дополнительные украшения и броню. Проверьте совместимые слоты и удобную смену комплекта.
fabric|Thaumic Dyes: ткани|thaumicdyes:ItemResourceFabric|Подготовьте магическую ткань для декоративных и специальных комплектов. Разновидность ткани имеет значение для рецепта.
robe|Мантия тауматурга|thaumicdyes:ThaumiumRobeChest|Соберите мантию после освоения магических материалов и проверьте свойства в сочетании с остальным комплектом.
''',entry='tc_fabric',optional=True)
current=next(c for c in chapters if c['id']=='avaritia')
seq('ava_extra','''
compressed|Сжатый сундук|avaritiaddons:CompressedChest|Соберите дополнительное хранение для массовых компонентов. Большое число слотов не заменяет фильтры и ограничители подачи.
infinite|Сундук бесконечности|avaritiaddons:InfinityChest|Подготовьте позднее хранилище и продумайте резервные копии ценных предметов перед переносом всей базы.
autocrafter|Большой автокрафтер|avaritiaddons:ExtremeAutoCrafter|Сравните возможности со сборщиком Applied Avaritia и выберите подходящую схему подачи и возврата результата.
''',entry='ava_infinity')
