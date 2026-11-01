package com.celik.kingdoms;

import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;

final class AdvancedBuffIntegration {
    static void run(KingdomService service,BiConsumer<Boolean,String> check)throws Exception{
        Config original=service.config;var path=KingdomsMod.configStore.dir.resolve("config.json");byte[] saved=Files.readAllBytes(path);
        try{
            service.config=Store.JSON.fromJson(Store.JSON.toJson(original),Config.class);
            var old=service.config.airDrops;old.endUnlockDay=30;old.tier(5).minRolls=7;old.tier(5).maxRolls=12;old.moneyByDay.put(5,65432L);
            String later=Store.JSON.toJson(old.tier(7)),variants=Store.JSON.toJson(old.variants),artifactOdds=Store.JSON.toJson(service.config.artifacts.chanceByDay);
            service.server.getCommands().performPrefixedCommand(service.server.createCommandSourceStack(),"kingdomadmin airdrop advancedbuff");
            var current=service.config.airDrops;var loaded=KingdomsMod.configStore.read("config.json",Config.class,null);
            check.accept(current.tier(5).minRolls==10&&current.tier(5).maxRolls==14&&loaded.airDrops.tier(5).minRolls==10,"advancedbuff command updates and durably saves an existing Advanced tier");
            check.accept(current.endUnlockDay==30&&current.money(5)==65432L&&later.equals(Store.JSON.toJson(current.tier(7)))&&variants.equals(Store.JSON.toJson(current.variants))&&artifactOdds.equals(Store.JSON.toJson(service.config.artifacts.chanceByDay)),"Advanced preset preserves End day 30, cash, other tiers, variants and artifact odds");
            String once=Store.JSON.toJson(service.config);
            service.server.getCommands().performPrefixedCommand(service.server.createCommandSourceStack(),"kingdomadmin airdrop advancedbuff");
            check.accept(once.equals(Store.JSON.toJson(service.config)),"repeated Advanced buff commands do not multiply loot or chances again");
            boolean legal=true;for(int day:List.of(5,6,29))for(var variant:current.variants)for(int seed=0;seed<10;seed++){
                var rewards=AirDropLoot.generate(service.server,current,current.tier(day),variant,day,new Random(seed));
                for(var reward:rewards){var stack=AirDropLoot.decode(service.server,reward.itemJson);var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();legal&=AirDropLoot.unlocked(current,new AirDropConfig.Loot(id,1,1,1),day)&&stack.getCount()<=stack.getMaxStackSize();}
            }
            check.accept(legal,"buffed generated loot retains legal stacks and excludes End rewards before configured day 30");
        }finally{service.config=original;Files.write(path,saved);}
    }
    private AdvancedBuffIntegration(){}
}
