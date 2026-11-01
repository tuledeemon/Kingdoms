package com.celik.kingdoms;

import java.util.*;

/** Explicit balance preset; existing servers opt in without replacing other settings. */
final class AdvancedAirDropBuff {
    static final Map<String,Double> RARES=Map.of("golden_apple",.12,"enchanted_golden_apple",.005,"totem",.01,"trident",.01,"upgrade_template",.02,"netherite_ingot",.01,"beacon",.002);
    static void tier(AirDropConfig.Tier tier){
        tier.minRolls=10;tier.maxRolls=14;
        for(var loot:tier.entries)larger(loot);
        for(var category:tier.categories){
            category.chance=switch(category.key){case "diamonds","enchantment"->.60;case "combat"->.70;case "potions"->.50;case "netherite"->.25;default->category.chance;};
            for(var loot:category.entries){
                if(Set.of("survival","basic","valuable","potions","netherite").contains(category.key))larger(loot);
                loot.enchantments.replaceAll((key,level)->level==3?4:level);
                if(category.key.equals("diamonds")){loot.minCount=8;loot.maxCount=16;}
                if(loot.item.equals("minecraft:experience_bottle")){loot.minCount=12;loot.maxCount=24;}
                if(category.key.equals("combat")&&loot.item.equals("minecraft:arrow")){loot.minCount=32;loot.maxCount=64;}
            }
            if(category.key.equals("enchantment"))category.entries.add(AirDropLootDefaults.enchanted("enchanted_book",45,"unbreaking",3));
        }
    }
    private static void larger(AirDropConfig.Loot loot){
        if(loot.maxCount<=1||!loot.enchantments.isEmpty())return;
        loot.minCount=Math.min(64,(loot.minCount*3+1)/2);loot.maxCount=Math.min(64,(loot.maxCount*3+1)/2);
    }
    static void apply(AirDropConfig config){
        var targets=config.lootTiers.stream().filter(t->"advanced".equalsIgnoreCase(t.name)).toList();
        if(targets.size()!=1)throw new IllegalArgumentException("Expected exactly one tier named advanced; edit custom tier names in config.json first.");
        var old=targets.getFirst();int start=old.fromDay;
        int end=config.lootTiers.stream().filter(t->t.fromDay>start).mapToInt(t->t.fromDay).min().orElse(Integer.MAX_VALUE);
        var replacement=AirDropLootDefaults.tiers().stream().filter(t->t.name.equals("advanced")).findFirst().orElseThrow();replacement.fromDay=start;
        // Preserve the effective later-tier chance even if its boundary was implicit.
        for(var rare:config.rareRewards){Double buff=RARES.get(rare.key);if(buff==null)continue;
            double later=rare.chance(end);
            if(end!=Integer.MAX_VALUE)rare.chanceByDay.putIfAbsent(end,later);
            rare.chanceByDay.put(start,buff);
        }
        config.lootTiers.set(config.lootTiers.indexOf(old),replacement);
    }
    private AdvancedAirDropBuff(){}
}
