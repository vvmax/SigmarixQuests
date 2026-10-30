chapter('automagy','Automagy: магическая логистика',['Automagy'],'Automagy:blockInventarium',parent='magic')
seq('auto','''
crystal|Красные кристаллы|Automagy:blockRedcrystal*8|Проложите сигнал по кристаллам. Проверьте направление, силу и разветвления на небольшой схеме.
tank|Жаждущий бак|Automagy:blockThirstyTank|Настройте сбор воды и вывод жидкости. Улучшения бака меняют поведение, поэтому проверяйте их по отдельности.
clock|Часы и дозирование|Automagy:blockHourglass|Используйте часы для ограниченной подачи ингредиентов, а не постоянного выброса предметов.
inventarium|Инвентариум|Automagy:blockInventarium|Подключите хранилища и задайте область учёта. Проверяйте, попадает ли каждый сундук в обслуживаемую систему.
request|Запрос предметов|Automagy:blockReqTome;Automagy:blockUnseenScribe|Подготовьте доступ к каталогу и выдаче. Запрос не создаёт отсутствующие материалы.
golem|Големный верстак|Automagy:blockGolemWorkbench;Automagy:blockGolemTaskmaster|Организуйте подачу ингредиентов големами, извлечение результата и возврат инструментов.
essentia|Узел эссенции|Automagy:blockEssentiaLocus|Разделите хранение аспектов и требования потребителей. Проверьте один точный рецепт наполнения.
experience|Хранение опыта|Automagy:blockXPJar|Настройте сбор опыта и выдачу. Контролируйте расход на зачарование отдельно от числа уровней.
''',entry='tc_golem')

chapter('thaumic_tinkerer','Thaumic Tinkerer и KAMI',['ThaumicTinkerer'],'ThaumicTinkerer:repairer',parent='magic')
seq('tt','''
repair|Восстановитель|ThaumicTinkerer:repairer|Подайте нужную эссенцию и проверьте совместимость предмета. Некоторые инструменты имеют собственную систему ремонта.
dislocate|Перемещение блоков|ThaumicTinkerer:dislocator|Начните с обычного блока. Не все механизмы безопасно переносятся с сохранением внутренних данных.
funnel|Воронка и интерфейс|ThaumicTinkerer:funnel;ThaumicTinkerer:interface|Организуйте перенос между инвентарями и проверьте ограничения стороны подключения.
enchant|Осмотический зачарователь|ThaumicTinkerer:enchanter|Выберите допустимое зачарование и обеспечьте эссенцию. Проверьте совместимость чар до расходования редкой вещи.
focus|Набалдашники для работы|ThaumicTinkerer:focusTelekinesis;ThaumicTinkerer:focusRecall|Настройте рабочий жезл и помните о стоимости вис каждого применения.
cloth|Ткань снятия чар|ThaumicTinkerer:spellCloth|Перед применением проверьте, что именно снимается и сохраняется. Не используйте дорогую экипировку как первый опыт.
ichor|Ихор|ThaumicTinkerer:kamiResource|Откройте поздние исследования KAMI и изготовьте ихор. Этот этап требует развитого Таумкрафта, а не только запаса ресурсов.
tools|Ихориевые инструменты|ThaumicTinkerer:ichorPick;ThaumicTinkerer:ichorSword|Настройте режимы инструментов и испытайте область действия вдали от базы.
armour|Ихориевая ткань|ThaumicTinkerer:ichorclothChest|Создайте снаряжение после освоения ихора и его улучшений. Проверяйте активацию особых способностей отдельно.
bedrock|Портал KAMI|ThaumicTinkerer:bedrockPortal|Подготовьте позднюю экспедицию по записи в Таумономиконе. Возьмите способ возвращения и запас необходимой энергии.
''',entry='tc_void')

chapter('forbidden','Forbidden Magic',['ForbiddenMagic'],'ForbiddenMagic:NetherShard',parent='magic')
seq('fm','''
shards|Осколки пороков|ForbiddenMagic:NetherShard|Изучите условия получения каждого вида осколков. Номер разновидности важен для рецепта.
ink|Чернильный цветок|ForbiddenMagic:InkFlower|Организуйте возобновляемый источник чернил и расходных материалов для исследований.
taint|Заражённая древесина|ForbiddenMagic:TaintSapling|Выращивайте заражённые растения отдельно от жилой базы. Изучите распространение перед масштабированием.
wand|Гибридные жезлы|ForbiddenMagic:WandCores;ForbiddenMagic:WandCaps|Выбирайте стержень под доступную магическую систему. Стержень и наконечник — компоненты, готовый жезл требует сборки.
cage|Клетка гнева|ForbiddenMagic:WrathCage|Поставьте клетку на подготовленной ферме и ограничьте число существ. Предусмотрите эссенцию и сбор добычи.
blood|Кровавая рапира|ForbiddenMagic:BloodRapier|Проверьте привязку и питание оружия. Магический интерфейс не освобождает от требований личной сети крови.
''',entry='tc_infusion')

chapter('gadomancy','Gadomancy',['gadomancy'],'gadomancy:BlockInfusionClaw',parent='magic')
seq('gado','''
claw|Клешня наполнения|gadomancy:BlockInfusionClaw|Подключите устройство к рабочей матрице. Автоматический запуск не устраняет требования симметрии и эссенции.
jars|Особые банки|gadomancy:BlockStickyJar;gadomancy:BlockRemoteJar|Сначала испытайте один аспект. Проверьте связь и условия передачи между удалёнными устройствами.
node|Работа с узлом|gadomancy:BlockExtendedNodeJar;gadomancy:BlockNodeManipulator|Переносите узел только после изучения инструмента. Сохраняйте доступ к запасному узлу для зарядки.
compress|Сжатие эссенции|gadomancy:BlockEssentiaCompressor|Подготовьте питание и запас аспектов. Контролируйте скорость входа и свободное место для результата.
aura|Пилон ауры|gadomancy:BlockAuraPylon;gadomancy:ItemAuraCore|Соберите устройство по исследованию и проверьте рабочую область до подключения всей мастерской.
golem|Улучшенные големы|gadomancy:itemSilverwoodGolemPlacer;gadomancy:ItemGolemCoreBreak|Назначьте область разрушения на тестовой площадке. Фильтры должны исключать стены и механизмы базы.
''',entry='tc_infusion')

chapter('horizons','Thaumic Horizons',['ThaumicHorizons'],'ThaumicHorizons:vat',parent='magic')
seq('th','''
lenses|Линзы наблюдения|ThaumicHorizons:lensCase;ThaumicHorizons:lensAir|Подготовьте футляр и сменные линзы. Проверяйте активную линзу перед использованием.
blood|Кровяное наполнение|ThaumicHorizons:bloodInfuser;ThaumicHorizons:syringeEmpty|Подготовьте эссенцию и шприцы. Испытайте выбранный эффект на безопасном образце.
vat|Чан преобразований|ThaumicHorizons:vat;ThaumicHorizons:vatSolid*8|Соберите структуру по исследованию. Не помещайте редкое существо до проверки питания и ингредиентов.
soul|Обработка душ|ThaumicHorizons:soulJar;ThaumicHorizons:soulforge|Подготовьте сосуды, источник и приёмник. Уникальные души храните отдельно от расходных.
dynamo|Энергия из магии|ThaumicHorizons:essentiaDynamo;ThaumicHorizons:visDynamo|Сравните расход эссенции и вис. Не считайте одинаковыми разные типы выходной энергии.
node|Синтетический узел|ThaumicHorizons:synthNode;ThaumicHorizons:nodeMonitor|Следите за состоянием узла и условиями стабилизации. Для первого опыта используйте отдельную лабораторию.
recombine|Рекомбинация|ThaumicHorizons:recombinator|Изучите входные состояния и риски прежде, чем соединять ценные узлы.
vortex|Стабилизатор вихря|ThaumicHorizons:vortexStabilizer|Подготовьте позднюю установку по исследованию. Уберите посторонние объекты из рабочей зоны.
''',entry='tc_void')

chapter('witchgadgets','Witching Gadgets',['WitchingGadgets'],'WitchingGadgets:item.WG_ScanCamera',parent='magic')
seq('wg','''
camera|Камера исследований|WitchingGadgets:item.WG_ScanCamera|Используйте камеру по назначению и сохраняйте результаты исследования отдельно от расходных материалов.
bag|Магическая сумка|WitchingGadgets:item.WG_Bag|Проверьте вместимость и настройки. Не храните единственный способ возвращения в недоступном вложенном инвентаре.
cluster|Кластеры руд|WitchingGadgets:item.WG_Cluster|Перерабатывайте кластеры в предназначенном устройстве. Обычная плавка и специальная переработка могут давать разный выход.
gems|Наполненные камни|WitchingGadgets:item.WG_InfusedGem|Подготовьте соответствующие исследования и аспекты. Разновидности камней не взаимозаменяемы.
robe|Продвинутая мантия|WitchingGadgets:item.WG_AdvancedRobeChest|Соберите магическую экипировку и проверьте её эффект при выбранном жезле.
primordial|Первобытные инструменты|WitchingGadgets:item.WG_PrimordialHammer|Освойте первобытные материалы до изготовления инструмента. Испытайте режим разрушения на отдельной площадке.
''',entry='tc_void')

chapter('taintedmagic','Tainted Magic',['TaintedMagic'],'TaintedMagic:ItemVoidmetalGoggles',parent='magic')
seq('tm','''
wood|Искажённая древесина|TaintedMagic:BlockWarpwoodSapling|Выделите участок под магическое дерево. Сохраните саженцы для восстановления.
metal|Теневой металл|TaintedMagic:BlockShadowmetal|Изучите получение металла и подготовьте расходные материалы для дальнейшей ветки.
goggles|Пустотные очки|TaintedMagic:ItemVoidmetalGoggles|Сопоставьте свойства с обычными очками откровения и сохраните доступ к наблюдению аспектов.
focus|Специализированные набалдашники|TaintedMagic:ItemFocusLumos;TaintedMagic:ItemFocusMeteorology|Испытывайте эффекты по одному и учитывайте стоимость вис.
fortress|Пустотная крепостная броня|TaintedMagic:ItemVoidFortressChestplate|Соберите развитую броню и проверьте совместимость с украшениями и остальным комплектом.
disassembler|Таум-разрушитель|TaintedMagic:ItemThaumicDisassembler|Проверьте режимы до работы рядом со складом. Большая область копания требует точного управления.
''',entry='tc_void')

chapter('electromagic','Electro-Magic Tools',['EMT'],'EMT:ElectricGogglesRevealing',parent='magic')
seq('emt','''
scribe|Электрические письменные принадлежности|EMT:ElectricScribingTools|Подготовьте заряд и проверьте работу на исследовательском столе.
goggles|Электрические очки|EMT:ElectricGogglesRevealing|Объедините наблюдение магии с электрическим снабжением. Следите за зарядом.
drill|Таум-бур|EMT:ThaumiumDrill|Проверьте скорость и стоимость работы. Система питания должна соответствовать инструменту.
wings|Магический полёт|EMT:ThaumiumWing|Испытывайте полёт над безопасной площадкой. Не рассчитывайте на полностью разряженное снаряжение.
focus|Энергетические набалдашники|EMT:ChargingFocus;EMT:WandChargingFocus|Проверьте направление преобразования и совместимые источники, чтобы не создавать бесполезную петлю зарядки.
quantum|Квантовая магическая экипировка|EMT:QuantumGogglesRevealing;EMT:QuantumWing|Переходите после освоения квантовой брони и магических исследований. Заряжайте комплект до экспедиций.
''',entry='ic2_advanced_case')

chapter('bloodarsenal','Blood Arsenal',['BloodArsenal'],'BloodArsenal:life_infuser',parent='magic')
seq('ba','''
glass|Стеклянные кинжалы|BloodArsenal:glass_sacrificial_dagger;BloodArsenal:glass_dagger_of_sacrifice|Различайте жертву и самопожертвование. Следите за прочностью и восстановлением здоровья.
iron|Наполненное железо|BloodArsenal:blood_infused_iron|Подготовьте кровавые материалы для улучшений. Обычное железо не всегда допустимая замена.
life|Наполнитель жизни|BloodArsenal:life_infuser|Подведите нужное питание и проверьте одну операцию до подключения МЭ.
portable|Переносной алтарь|BloodArsenal:portable_altar|Проверьте возможности переносного устройства: оно не обязано заменять все уровни большого алтаря.
amulets|Амулеты жертвоприношения|BloodArsenal:sacrifice_amulet;BloodArsenal:self_sacrifice_amulet|Подберите усиление под фактический источник LP, а не только под доступный рецепт.
sigils|Усиленные сигилы|BloodArsenal:sigil_of_augmented_holding|Настройте набор сигилов и постоянный расход личной сети.
''',entry='bm_demonic')
