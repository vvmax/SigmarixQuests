chapter('bigreactors','Big Reactors и компактная энергетика',['BigReactors','compactreactors'],'BigReactors:BRReactorPart',parent='technology')
seq('br','''
fuel|Топливо реактора|BigReactors:BRIngot*8|Подготовьте йеллорий и отдельное хранилище отходов. Учитывайте потребление топлива во времени.
casing|Корпус и контроллер|BigReactors:BRReactorPart*32;BigReactors:BRReactorPart:1|Соберите замкнутую прямоугольную оболочку. Стекло заменяет часть корпуса, но углы и рёбра проверяйте по правилам структуры.
rods|Топливные стержни|BigReactors:YelloriumFuelRod*4;BigReactors:BRReactorPart:2|Над каждой колонной стержней нужен управляющий стержень. Геометрия топлива и замедлитель влияют на эффективность.
ports|Порты|BigReactors:BRReactorPart:3;BigReactors:BRReactorPart:4|Разделите ввод топлива, вывод отходов и питание. Сначала включите реактор с ограниченной мощностью.
steam|Активное охлаждение|BigReactors:BRReactorPart:5*2|Добавьте вход воды и выход пара. Активный реактор требует правильно рассчитанной турбины или другого приёмника пара.
turbine|Паровая турбина|BigReactors:BRTurbinePart*32;BigReactors:BRTurbinePart:1;BigReactors:BRTurbineRotorPart*4;BigReactors:BRTurbineRotorPart:1*8|Соберите ротор, лопатки и индукционную катушку по правилам турбины.|Настраивайте поток пара по оборотам и эффективности. Избыточный поток не обязательно даёт лучший расход топлива. Верните конденсат в контур и проверьте, что вода не заканчивается при максимальном потреблении.
compact|Компактный реактор|compactreactors:compact_reactor|После обычной ядерной технологии подготовьте компактную установку. Изучите допустимые компоненты и охлаждение до загрузки топлива.
module1|Расширение I|compactreactors:expansion_module_1|Добавьте первый модуль, не меняя одновременно все топливные элементы. Проверьте изменение доступной ёмкости.
module2|Расширение II|compactreactors:expansion_module_2|Соберите второй модуль из развитых технических компонентов. Он остаётся промежуточным улучшением, без финальных магических катализаторов.
advanced|Продвинутый компактный реактор|compactreactors:advanced_compact_reactor|Настройте реактор под надёжное длительное питание. Уберите экспериментальные схемы из основного энергоконтура.
''',entry='ic2_advanced_case')

chapter('solar_gravi','Солнечная энергия и электрическое снаряжение',['AdvancedSolarPanel','supersolarpanel','GraviSuite','IC2CA','IC2NuclearControl','PowerConverters3'],'AdvancedSolarPanel:BlockAdvSolarPanel',parent='technology')
seq('sol','''
advanced|Продвинутая солнечная панель|AdvancedSolarPanel:BlockAdvSolarPanel|Поставьте панель под открытым небом и подключите совместимый накопитель. Проверьте дневную и ночную генерацию.
hybrid|Гибридная ступень|AdvancedSolarPanel:BlockAdvSolarPanel:1|Расширяйте генерацию вместе с пропускной способностью кабелей и накопителей EU.
ultimate|Ультимативная ступень|AdvancedSolarPanel:BlockAdvSolarPanel:2|Отделите высокую генерацию от низковольтных машин трансформаторами или подходящим вводом.
transform|Молекулярное преобразование|AdvancedSolarPanel:BlockMolecularTransformer|Подготовьте большой запас энергии. Учитывайте время операции при планировании заказов.
spectral|Спектральная панель|supersolarpanel:SpectralSolarPanel|Подготовьте компоненты нового уровня и проверьте фактическую выдачу энергии.
singular|Сингулярная панель|supersolarpanel:SingularSolarPanel|Планируйте сборку партиями предыдущего уровня. Не запускайте огромный заказ без оценки памяти МЭ.
admin|Светоадсорбционная панель|supersolarpanel:AdminSolarPanel|Соберите предфотонную ступень и проверьте передачу мощности в основной накопитель.
''',entry='ic2_advanced_case')
seq('gravi','''
jet|Продвинутый реактивный ранец|GraviSuite:advJetpack|Проверьте заряд и режим зависания над безопасной площадкой.
drill|Продвинутый бур|GraviSuite:advDDrill|Настройте режим копания и снабжение энергией. Перед работой рядом с механизмами выберите малую область.
lappack|Переносной накопитель|GraviSuite:advLappack|Подберите способ зарядки инструментов в полевых условиях. Накопитель и броня могут конкурировать за слот.
chest|Гравитационная кираса|GraviSuite:graviChestPlate|Освойте квантовую броню до сборки кирасы. Проверьте расход энергии в полёте.
vajra|Ваджра|GraviSuite:vajra|Настройте режим и испытайте его на простых блоках. Зарядите инструмент перед массовой добычей.
assembler|Сборка модулей брони|IC2CA:ArmorAssemblerBlock;IC2CA:exomodule|Соберите броню в предусмотренной установке и проверяйте совместимость модулей.
flight|Модули полёта и энергии|IC2CA:flightmodule;IC2CA:cellmodule|Подбирайте модули под назначение комплекта. Сохраняйте запас энергии на возвращение.
sensors|Датчики реактора|IC2NuclearControl:ItemToolDigitalThermometer;IC2NuclearControl:ItemRemoteSensorKit|Свяжите датчик с нужным устройством. Показания без привязки не являются измерением реактора.
monitor|Информационная панель|IC2NuclearControl:blockNuclearControlMain|Выведите температуру, энергию и предупреждения. Настройте аварийную остановку отдельно от декоративного дисплея.
convert|Преобразование энергии|PowerConverters3:powerconverters.common|Соберите мост из подходящих потребителя и производителя. Не соединяйте выход обратно со входом ради фиктивной выработки.
''',entry='ic2_advanced_case')

chapter('mfr','MineFactory Reloaded',['MineFactoryReloaded'],'MineFactoryReloaded:machine.0',parent='technology')
seq('mfr','''
plastic|Пластик|MineFactoryReloaded:plastic.sheet*16|Вырастите каучуковое дерево и подготовьте пластик. Он станет основой фермерских машин.
planter|Посадка и сбор|MineFactoryReloaded:machine.0;MineFactoryReloaded:machine.0:2|Согласуйте область сеятеля и комбайна. Выделите возврат части семян, прежде чем отправлять урожай на переработку.
fertilizer|Удобрение|MineFactoryReloaded:machine.0:4;MineFactoryReloaded:fertilizer*8|Ускоряйте только культуры, которые поддерживает установка. Проверьте расход удобрения на устойчивой ферме.
sludge|Переработка отходов|MineFactoryReloaded:machine.0:9|Отведите шлам от комбайна и предусмотрите выходы переработки. Забитый побочный выход может остановить ферму.
animals|Животноводство|MineFactoryReloaded:machine.0:12;MineFactoryReloaded:machine.0:15|Настройте разведение и разделение молодняка. Ограничьте поголовье и подачу корма.
grinder|Добыча эссенции|MineFactoryReloaded:machine.0:13|Разместите молотилку на закрытой ферме и собирайте эссенцию отдельно от предметной добычи.
net|Сафари-сеть|MineFactoryReloaded:safarinet.reusable|Захватывайте совместимое существо и сохраняйте сеть с его данными. Пустая сеть не задаёт тип спавнера.
spawner|Автоспавнер|MineFactoryReloaded:machine.1:9|Настройте режим появления, питание и эссенцию. Начните с небольшого лимита существ.
pink|Розовая слизь|MineFactoryReloaded:machine.1:13;MineFactoryReloaded:pinkslime|Организуйте бойню и получение розовой слизи. Разделяйте её и обычное жидкое мясо.
laser|Лазерный бур|MineFactoryReloaded:machine.2;MineFactoryReloaded:machine.2:1*4|Поставьте бур и зарядники с правильным направлением. Цветные фокусы меняют вероятности, а не гарантируют каждую руду.
bio|Биотопливо|MineFactoryReloaded:machine.1:10;MineFactoryReloaded:machine.1:11|Обеспечьте разнообразное сырьё и сравните реальную выработку со стоимостью выращивания.
enchant|Работа с чарами|MineFactoryReloaded:machine.0:14;MineFactoryReloaded:machine.1:12|Разделите зачарование и снятие чар. Контролируйте расход эссенции и выход книг.
rednet|Программируемая логика|MineFactoryReloaded:rednet.logic;MineFactoryReloaded:cable.redstone*8|Соберите маленькую схему с понятными входами и выходами. Сохраните настройки до подключения к реактору.
''',entry='te_frame0')

chapter('utility_transport','Транспорт и полезная автоматизация',['ThermalDynamics','ExtraUtilities','sigmarixindustrial'],'ThermalDynamics:servo',parent='networks')
seq('util','''
duct|Предметные трубы|ThermalDynamics:ThermalDynamics_32*8;ThermalDynamics:servo|Серво извлекает предметы, фильтр ограничивает приём. Задайте режим красного сигнала и проверьте маршрут.
retriever|Адресная доставка|ThermalDynamics:retriever;ThermalDynamics:filter|Получатель запрашивает нужное сырьё из доступной сети труб. Не делайте встречные запросы без запаса места.
fluid|Жидкостные трубы|ThermalDynamics:ThermalDynamics_16*8|Проверьте пропускную способность и возможность смешивания жидкостей. Отдельный контур проще диагностировать.
flux|Флаксовые трубы|ThermalDynamics:ThermalDynamics_0*8|Согласуйте ограничения соединений, источника и приёмника. Заполненные и пустые высокоуровневые трубы — разные состояния.
nodes|Передающие узлы|ExtraUtilities:extractor_base;ExtraUtilities:pipes*8|Настройте извлечение и маршруты на небольшом примере. Улучшения не должны создавать бесконечное перемещение одного предмета.
drum|Барабан|ExtraUtilities:drum|Организуйте большой запас жидкости с отдельным входом и выходом. Перед переносом проверьте сохранность содержимого.
ritual|Сигил деления|ExtraUtilities:divisionSigil|Изучите условия активации перед ритуалом. Выделите площадку и не проводите опасный этап внутри базы.
unstable|Нестабильный слиток|ExtraUtilities:unstableingot|Подготовьте рецепт заранее и изучите условия стабильности. Не начинайте производство без свободного рабочего места и нужных ингредиентов.
quarry|Эндер-карьер|ExtraUtilities:enderQuarry|Обозначьте область, подключите энергию и выход ресурсов. Выбирайте разрешённую территорию и контролируйте заполнение склада.
flight|Кольцо ангела|ExtraUtilities:angelRing|Получите компоненты продвинутого полёта. Проверьте его работу в текущей конфигурации сервера.
industrial|Машины Sigmarix Industrial|sigmarixindustrial:machine|Изучите интерфейс доступной машины и её режимы по установленной версии. Сначала проверьте малую операцию и направление ввода энергии.
rotor|Роторы Sigmarix Industrial|sigmarixindustrial:rotor0;sigmarixindustrial:rotor1;sigmarixindustrial:rotor2|Подберите ротор под механизм и проверьте ресурс работы. Замена ротора требует остановки соответствующего устройства.
''',entry='te_frame0')
