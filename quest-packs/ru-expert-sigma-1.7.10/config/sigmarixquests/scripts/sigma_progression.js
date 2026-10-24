/* Java 8 Nashorn. Read-only checks: no commands, world edits or granted research. */
(function () {
    'use strict';
    var cachedTypes = {}, warned = {};
    function type(name) {
        if (!cachedTypes[name]) cachedTypes[name] = Java.type(name);
        return cachedTypes[name];
    }
    function register(key, handler) {
        quests.task(key, function (player, task) {
            try { return handler(player, task) === true; }
            catch (error) {
                if (!warned[key]) {
                    warned[key] = true;
                    Java.type('java.lang.System').err.println('[Sigma quests] Check failed: ' + key + ': ' + error);
                }
                return false;
            }
        });
    }
    register('sigma.tc_research', function (player, task) {
        var name = typeof player.func_70005_c_ === 'function' ? player.func_70005_c_() : player.getCommandSenderName();
        return type('thaumcraft.common.lib.research.ResearchManager').isResearchComplete(String(name), String(task.criterion)) === true;
    });
    register('sigma.dimension', function (player, task) {
        var world = player.field_70170_p || player.worldObj;
        var provider = world.field_73011_w || world.provider;
        return String(provider.getClass().getName()) === String(task.criterion);
    });
    register('sigma.witch_vampire', function (player, task) {
        return Number(type('com.emoniph.witchery.common.ExtendedPlayer').get(player).getVampireLevel()) >= Number(task.count);
    });
    register('sigma.witch_werewolf', function (player, task) {
        return Number(type('com.emoniph.witchery.common.ExtendedPlayer').get(player).getWerewolfLevel()) >= Number(task.count);
    });
}());
