chapter('tconstruct','Tinkers’ Construct: инструменты и литьё',['TConstruct'],'TConstruct:manualBook',parent='foundations')
seq('tic','''
manual|Материалы и вы|TConstruct:manualBook|Изучите свойства материалов и отличие головки, рукояти и крепления. Сочетание частей определяет свойства инструмента.
patterns|Шаблоны|TConstruct:blankPattern*8;TConstruct:woodPattern|Подготовьте станцию шаблонов и нужные рисунки. Не расходуйте редкий материал, пока не выбраны все части.
station|Сборка первого инструмента|TConstruct:ToolStationBlock;TConstruct:pickaxe|Соберите кирку из отдельных частей. Задание принимает инструмент с любым корректным набором материалов.
repair|Ремонт и модификаторы|TConstruct:toolRod;TConstruct:binding|Ремонтируйте подходящим материалом и учитывайте число свободных модификаторов. Перед улучшением сравните свойства всего инструмента.
grout|Обожжённый кирпич|TConstruct:materials:2*64|Подготовьте материалы плавильни и топливо. Количество кирпичей зависит от выбранного размера.
smeltery|Плавильня|TConstruct:Smeltery;TConstruct:LavaTank|Постройте правильную оболочку, контроллер и лавовый бак. Увеличение высоты повышает ёмкость, но не заменяет топливо.
casting|Литейная площадка|TConstruct:SearedBlock;TConstruct:metalPattern|Поставьте кран, стол и литейный резервуар. Различайте отливку детали, слитка и блока.
alloys|Управление сплавами|TConstruct:materials:3|Проверьте рецепт сплава перед загрузкой металлов. Избыток одного компонента остаётся в плавильне; разные жидкости могут смешаться в нежелательный сплав.
forge|Инструментальная кузня|TConstruct:ToolForgeBlock|Перейдите к большим инструментам после подготовки достаточного запаса металлических компонентов.
hammer|Молот|TConstruct:hammer|Соберите молот и испытайте область копания на безопасной стене. Для ценных блоков используйте режим с точным управлением.
lumber|Лесоруб и землекоп|TConstruct:lumberaxe;TConstruct:excavator|Выберите инструменты под ферму и выравнивание территории. Проверьте прочность и уровень добычи.
weapons|Составное оружие|TConstruct:cleaver;TConstruct:Crossbow|Подберите материал и модификаторы под назначение. Для арбалета нужны совместимые болты.
bolts|Боеприпасы|TConstruct:BoltAmmo|Изготовьте партию боеприпасов и организуйте ремонт. Разные стрелы и болты не взаимозаменяемы.
heart|Постоянная подготовка|TConstruct:heartCanister|Изготовьте допустимый контейнер сердца и проверьте слот экипировки.
travel|Снаряжение путешественника|TConstruct:travelBoots;TConstruct:travelGlove|Настройте улучшения и сравните их с основной бронёй. Задание проверяет предметы, а не все установленные модификаторы.
''',entry='start_tools')
for node in quests:
 if node['id'].startswith('tic_'):
  for task in node['tasks']:
   if task['type']=='item' and task['target'] in ['TConstruct:pickaxe','TConstruct:hammer','TConstruct:lumberaxe','TConstruct:excavator','TConstruct:cleaver','TConstruct:Crossbow','TConstruct:BoltAmmo']:task['ignoreDamage']=True

chapter('galacticraft','Galacticraft: от мастерской до астероидов',['GalacticraftCore','GalacticraftMars'],'GalacticraftCore:item.rocketFins',parent='exploration')
seq('gc','''
plates|Сжатые материалы|GalacticraftCore:tile.machine:12;GalacticraftCore:item.basicItem:9*8|Подготовьте компрессор и сжатую сталь. Внешне похожие пластины разных систем не всегда заменяют эти материалы.
wafers|Кремниевые пластины|GalacticraftCore:tile.machine2:4;GalacticraftCore:item.basicItem:13|Настройте изготовитель микросхем и положите ингредиенты в правильные слоты.
oxygen|Личная система кислорода|GalacticraftCore:item.oxygenGear;GalacticraftCore:item.oxygenMask;GalacticraftCore:item.oxygenTankHeavyFull|Подготовьте маску, кислородное снаряжение и заполненные баллоны. Обычный слот брони не заменяет космическую экипировку.
collector|Сбор и сжатие кислорода|GalacticraftCore:tile.oxygenCollector;GalacticraftCore:tile.oxygenCompressor|Обеспечьте растения или подходящую атмосферу и энергию. Проверьте наполнение баллона до запуска ракеты.
fuel|Ракетное топливо|GalacticraftCore:tile.refinery;GalacticraftCore:tile.fuelLoader|Переработайте нефть и заполните ракету через загрузчик. Контейнер с топливом в инвентаре не заправляет её автоматически.
workbench|Верстак NASA|GalacticraftCore:tile.rocketWorkbench|Соберите верстак и откройте доступную схему ракеты. Число грузовых сундуков определяет её грузовой вариант.
rocket|Первая ракета|GalacticraftCore:item.spaceship;GalacticraftCore:tile.landingPad*9|Подготовьте площадку, топливо, парашют и средства возвращения. После полёта сохраните посадочную добычу и ракету.
moon|Лунная экспедиция|visit:micdoodle8.mods.galacticraft.core.dimension.WorldProviderMoon|Посетите Луну. Проверяется фактический класс измерения, поэтому переназначение числового ID не ломает задание.
meteor|Метеоритное железо|GalacticraftCore:item.meteoricIronIngot*8|Соберите метеоритное железо и оставьте запас на дальнейшую технику.
seal|Герметичная база|GalacticraftCore:tile.sealer;GalacticraftCore:tile.airLockFrame*8|Замкните помещение герметичными блоками и поставьте кислородный герметизатор. Проверьте утечки до снятия личных баллонов.|Открытые двери, неподходящие блоки и неправильный шлюз нарушают герметичность. Обеспечьте постоянный кислород и энергию; при расширении базы проверяйте каждый новый отсек отдельно.
dungeon|Лунные подземелья|GalacticraftCore:item.key;GalacticraftMars:item.spaceshipTier2|Найдите подземелье, получите ключ и следующую схему. Ключ соответствует своему уровню сокровищницы.
mars|Марс|visit:micdoodle8.mods.galacticraft.planets.mars.dimension.WorldProviderMars|Подготовьте вторую ракету и посетите Марс. Возьмите запас кислорода и топлива на возвращение.
desh|Марсианская технология|GalacticraftMars:item.deshPick;GalacticraftMars:tile.marsMachine|Добудьте дэши и освоите марсианские машины. Редкое сырьё сначала используйте для продвижения, затем для запасной экипировки.
thermal|Термозащита|GalacticraftMars:item.thermalPadding|Подготовьте полный набор для нужных условий. Один предмет подтверждает начало подготовки, но не весь надетый комплект.
tier3|Третья ракета|GalacticraftMars:item.itemTier3Rocket|Получите схему в соответствующей сокровищнице и соберите ракету следующего уровня.
asteroids|Пояс астероидов|visit:micdoodle8.mods.galacticraft.planets.asteroids.dimension.WorldProviderAsteroids|Посетите астероиды с запасом кислорода, энергией и надёжным способом перемещения в пустоте.
miner|Астрошахтёр|GalacticraftMars:item.itemAstroMiner;GalacticraftMars:tile.minerBase*8|Соберите базу шахтёра и обеспечьте выгрузку. Учитывайте движение аппарата и запретные для добычи территории.
beam|Лучевая энергия|GalacticraftMars:tile.beamReceiver;GalacticraftMars:tile.beamReflector|Настройте видимость между устройствами и проверьте передачу на небольшом расстоянии.
telepad|Телепады|GalacticraftMars:tile.telepadShort*2|Задайте адреса и питание. Прежде чем убирать обычный маршрут, проверьте обе стороны перемещения.
''',entry='te_frame1')

chapter('building','Обустройство и оформление',['chisel','CarpentersBlocks','BiblioCraft','TravellersGear','thaumicdyes'],'minecraft:brick_block',parent='foundations')
seq('home','''
chisel|Палитра Chisel|chisel:chisel|Подберите отделку и сохраните образец. Варианты материала меняют внешний вид, а не всю физику блока.
carpenter|Каркасные блоки|CarpentersBlocks:blockCarpentersBlock*16|Нанесите покрытие на каркасы и проверьте нужную форму. Обычный блок покрытия и готовая конструкция не одно и то же.
lighting|Освещение мастерской|minecraft:glowstone*16|Осветите проходы и рабочие площадки. Сдайте запас для расширения освещения: наличие запаса не является проверкой всей базы.
library|Хранение книг|BiblioCraft:BiblioShelf|Организуйте библиотеку инструкций и зачарованных книг рядом с мастерской.
display|Витрины|BiblioCraft:BiblioCase|Разложите памятные образцы отдельно от рабочих расходников. Не прячьте единственный редкий ингредиент в декоре.
armour|Стойки снаряжения|BiblioCraft:Armor Stand|Организуйте сменные комплекты и место их зарядки. После смены проверьте космическую и магическую экипировку в отдельных слотах.
''',entry='store_iron')
