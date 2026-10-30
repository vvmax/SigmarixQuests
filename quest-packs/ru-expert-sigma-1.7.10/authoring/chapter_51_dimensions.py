chapter('divine','DivineRPG: последовательность миров',['divinerpg'],'divinerpg:divineStone',parent='exploration')
seq('div','''
realmite|Первая особая руда|divinerpg:realmiteIngot*8|Освойте подземные ресурсы DivineRPG. Сравнивайте броню, оружие и уровень добычи, а не только редкость материала.
advanced|Арлемит и рупий|divinerpg:arlemiteIngot*8;divinerpg:rupeeIngot*8|Подготовьте запас для оборудования и следующих рецептов. Не отправляйтесь к новым боссам в стартовой броне.
aquatic|Водная ветка|divinerpg:aquaticIngot*8|Соберите водные ресурсы и подготовьте дыхание под водой. Сохраняйте способ быстро вернуться на поверхность.
nether|Адские ресурсы|divinerpg:netheriteIngot*8;divinerpg:hellstoneIngot*8|Получите материалы Нижнего мира и защиту от огня. Учитывайте более опасных существ мода.
shards|Стихийные осколки|divinerpg:iceShards;divinerpg:jungleShards;divinerpg:moltenShards;divinerpg:enderShards|Подготовьте разные ветки трофеев. Каждый осколок связан со своим источником и не заменяется случайным камнем.
divine|Божественный камень|divinerpg:divineStone*10|Получите материал первого сумеречного портала. Сохраните запас для возвращения и следующих переходов.
clock|Сумеречные часы|divinerpg:twilightClock|Подготовьте активатор порталов и изучите требования к рамке. Обычный огниво не обязательно подходит.
eden|Эдем|visit:net.divinerpg.dimensions.twilight.eden.WorldProviderEden|Посетите первый сумеречный мир. Постройте безопасную точку прибытия и отметьте обратный портал.
eden_resources|Ресурсы Эдема|divinerpg:edenChunk;divinerpg:edenSoul|Получите местные материалы и души для продвижения. Не ломайте весь портал на расходники.
wildwood|Дикий лес|visit:net.divinerpg.dimensions.twilight.wildwood.WorldProviderWildwood|Перейдите в следующую ступень после подготовки местного снаряжения и запаса еды.
wildwood_resources|Ресурсы Дикого леса|divinerpg:wildwoodChunk;divinerpg:wildwoodSoul|Соберите материалы для следующего перехода и сохраните резерв.
apalachia|Апалачия|visit:net.divinerpg.dimensions.twilight.apalachia.WorldProviderApalachia|Продолжайте последовательность миров, проверяя новые угрозы и подходящий уровень оружия.
apalachia_resources|Ресурсы Апалачии|divinerpg:apalachiaChunk;divinerpg:apalachiaSoul|Подготовьте материалы следующей ступени.
skythern|Небесный терн|visit:net.divinerpg.dimensions.twilight.skythern.WorldProviderSkythern|Возьмите надёжные средства перемещения и защиту от падения.
skythern_resources|Ресурсы Небесного терна|divinerpg:skythernChunk;divinerpg:skythernSoul|Соберите материал дальнейшего перехода и подготовьте комплект для финального сумеречного мира.
mortum|Мортум|visit:net.divinerpg.dimensions.twilight.mortum.WorldProviderMortum|Завершите последовательность сумеречных миров и обеспечьте безопасную точку прибытия.
mortum_resources|Ресурсы Мортума|divinerpg:mortumChunk;divinerpg:mortumSoul|Получите местные ресурсы и спланируйте позднее снаряжение.
''',entry='start_end')
seq('div_side','''
iceika|Айсика|visit:net.divinerpg.dimensions.iceika.WorldProviderIceika|Изучите отдельную ледяную ветку, местные поселения и обмены. Она не обязана ждать завершения всех сумеречных миров.
arcana|Аркана|visit:net.divinerpg.dimensions.arcana.WorldProviderArcana|Подготовьте вход в Аркану и изучите её особую систему ресурсов и торговцев.
vethea|Ветеа|visit:net.divinerpg.dimensions.vethea.WorldProviderVethea|Изучите правила сновидческого путешествия и местной прогрессии. Не рассчитывайте на обычный комплект, пока не проверены правила переноса вещей.
''',entry='div_advanced',optional=True)

chapter('biomes','Природа Biomes O’ Plenty и Netherlicious',['BiomesOPlenty','netherlicious'],'BiomesOPlenty:biomeFinder',parent='exploration')
seq('biome','''
finder|Поиск биомов|BiomesOPlenty:biomeFinder|Подготовьте прибор и изучите его настройку. Климат важен для пасек и селекции, а не только для оформления.
gems|Самоцветы биомов|BiomesOPlenty:gems:1;BiomesOPlenty:gems:2;BiomesOPlenty:gems:3|Найдите разные биомные ресурсы и сохраняйте координаты мест добычи.
wood|Редкая древесина|BiomesOPlenty:logs1:1;BiomesOPlenty:logs2:2|Соберите образцы вместе с саженцами. Древесина без возобновляемого источника быстро заканчивается в строительстве.
amethyst|Аметист Края|BiomesOPlenty:gems|Подготовьте экспедицию в Край и получите аметист. Не отправляйтесь без способа преодолевать пустоту.
netherwood|Новые материалы Нижнего мира|netherlicious:CrimsonChest;netherlicious:Blackstone|Освойте новые адские материалы и хранение. Огнестойкость конкретного блока проверяйте до использования у лавы.
netherfood|Адское земледелие|netherlicious:DevilishMaizeSeeds;netherlicious:DevilishMaizeItem|Организуйте небольшой возобновляемый участок и приготовление местной пищи.
crystal|Кристаллы Нижнего мира|netherlicious:CrystalShard*8;netherlicious:CrystalLamp|Переработайте кристаллы в освещение и сохраните образцы разных цветов.
''',entry='start_nether')
