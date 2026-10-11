package com.celik.kingdoms;

import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.*;
import static com.celik.kingdoms.AirDropRules.*;

public final class AirDropLoot {
    public static ItemStack template(MinecraftServer server,AirDropConfig.Loot entry){
        try {
            StringReader reader=new StringReader(entry.item);ItemStack stack=new ItemParser(server.registryAccess()).parse(reader).createItemStack(1);
            if(reader.canRead()||stack.isEmpty())throw new IllegalArgumentException("Invalid AirDrop item: "+entry.item);
            if(!entry.enchantments.isEmpty()){
                var component=stack.is(Items.ENCHANTED_BOOK)?DataComponents.STORED_ENCHANTMENTS:DataComponents.ENCHANTMENTS;
                var enchantments=new ItemEnchantments.Mutable(stack.getOrDefault(component,ItemEnchantments.EMPTY));
                entry.enchantments.forEach((name,level)->enchantments.set(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse(name))),level));
                stack.set(component,enchantments.toImmutable());
            }
            if(!entry.potion.isEmpty())stack.set(DataComponents.POTION_CONTENTS,new PotionContents(server.registryAccess().lookupOrThrow(Registries.POTION).getOrThrow(ResourceKey.create(Registries.POTION,Identifier.parse(entry.potion)))));
            if(!entry.name.isEmpty())stack.set(DataComponents.CUSTOM_NAME,Component.literal(Ui.caps(entry.name)));
            if(!entry.lore.isEmpty())stack.set(DataComponents.LORE,new ItemLore(entry.lore.stream().map(line->(Component)Component.literal(Ui.caps(line))).toList()));
            if(entry.glint)stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
            return stack;
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalArgumentException("Invalid AirDrop item definition: "+entry.item,e);}
    }
    public static void validate(MinecraftServer server,AirDropConfig config){config.validate();for(var tier:config.lootTiers){for(var e:tier.entries)template(server,e);for(var category:tier.categories)for(var e:category.entries)template(server,e);}config.specialPools.values().forEach(pool->pool.forEach(e->template(server,e)));config.rareRewards.forEach(r->template(server,r.loot));}
    public static String encode(MinecraftServer server,ItemStack stack){return ItemStack.CODEC.encodeStart(server.registryAccess().createSerializationContext(JsonOps.INSTANCE),stack).getOrThrow().toString();}
    public static ItemStack decode(MinecraftServer server,String json){return ItemStack.CODEC.parse(server.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(json)).getOrThrow();}
    private static AirDropConfig.Loot weighted(List<AirDropConfig.Loot> pool,Random random){long total=pool.stream().mapToLong(e->e.weight).sum(),roll=random.nextLong(total);for(var e:pool){roll-=e.weight;if(roll<0)return e;}throw new IllegalStateException("Empty AirDrop loot pool");}
    public static String itemId(AirDropConfig.Loot entry){String id=entry.item.split("\\[",2)[0];return id.contains(":")?id:"minecraft:"+id;}
    public static boolean unlocked(AirDropConfig config,AirDropConfig.Loot entry,long day){
        String id=itemId(entry);if(!id.contains(":"))id="minecraft:"+id;String path=id.replace("minecraft:","");int required=Math.max(entry.minDay,config.itemUnlockDays.getOrDefault(id,1));
        if(path.startsWith("netherite_")||path.startsWith("crimson_")||path.startsWith("warped_")||path.startsWith("blackstone")||path.startsWith("polished_blackstone")||path.startsWith("nether_brick")||path.startsWith("red_nether_brick")||path.startsWith("basalt")||path.startsWith("polished_basalt")||path.startsWith("smooth_basalt")||Set.of("potion","splash_potion","lingering_potion","tipped_arrow","shroomlight","gilded_blackstone","magma_block","wither_skeleton_skull","nether_sprouts","twisting_vines","weeping_vines","soul_lantern","soul_torch","soul_campfire","red_nether_bricks").contains(path)||Set.of("ancient_debris","blaze_rod","blaze_powder","nether_wart","quartz","nether_quartz_ore","ghast_tear","magma_cream","soul_sand","soul_soil","nether_star","beacon","nether_brick","nether_bricks","netherrack","glowstone","glowstone_dust","crying_obsidian","respawn_anchor").contains(path)||!entry.potion.isEmpty())required=Math.max(required,config.netherUnlockDay);
        if(path.contains("shulker")||path.startsWith("purpur")||Set.of("elytra","end_stone","end_stone_bricks","end_rod","chorus_fruit","popped_chorus_fruit","chorus_flower","chorus_plant","dragon_breath","dragon_egg").contains(path))required=Math.max(required,config.endUnlockDay);
        return day>=required;
    }
    private static boolean ordinary(AirDropConfig config,AirDropConfig.Loot entry,long day){return unlocked(config,entry,day)&&config.rareRewards.stream().noneMatch(r->itemId(r.loot).equals(itemId(entry)));}
    public static AirDropConfig.Variant variant(AirDropConfig config,Random random){long roll=random.nextLong(config.variants.stream().mapToLong(v->v.weight).sum());for(var v:config.variants){roll-=v.weight;if(roll<0)return v;}throw new IllegalStateException("Empty AirDrop variants");}
    private static void add(MinecraftServer server,List<Reward> rewards,AirDropConfig.Loot entry,String rareKey,Random random){ItemStack template=template(server,entry);int count=random.nextInt(entry.minCount,entry.maxCount+1);while(count>0){int amount=Math.min(count,template.getMaxStackSize());Reward r=new Reward();r.rareKey=rareKey;r.itemJson=encode(server,template.copyWithCount(amount));rewards.add(r);count-=amount;}}
    /** Category and rare checks happen once per crate, never once per loot slot. */
    public static List<Reward> generate(MinecraftServer server,AirDropConfig config,AirDropConfig.Tier tier,AirDropConfig.Variant variant,long day,Random random){
        List<AirDropConfig.Loot> common=tier.entries.stream().filter(e->e.chance==1&&ordinary(config,e,day)).toList();
        if(common.isEmpty())throw new IllegalArgumentException("AirDrop tier has no unlocked common fallback loot");
        List<AirDropConfig.Loot> pool=new ArrayList<>();List<Reward> rewards=new ArrayList<>();int rolls=random.nextInt(tier.minRolls,tier.maxRolls+1),selected=0;
        for(var category:tier.categories){
            List<AirDropConfig.Loot> entries=category.entries.stream().filter(e->ordinary(config,e,day)).toList();
            if(entries.isEmpty()||random.nextDouble()>=Math.min(1,category.chance*variant.categoryMultipliers.getOrDefault(category.key,1.0)))continue;
            pool.addAll(entries);var entry=weighted(entries,random);if(random.nextDouble()>=entry.chance)entry=weighted(common,random);add(server,rewards,entry,null,random);selected++;
        }
        if(!config.specialPool.isEmpty()&&random.nextDouble()<config.specialPoolChance)pool.addAll(config.specialPools.get(config.specialPool).stream().filter(e->ordinary(config,e,day)).toList());
        if(pool.isEmpty())pool.addAll(common);
        for(int i=selected;i<rolls;i++){var entry=weighted(pool,random);if(random.nextDouble()>=entry.chance)entry=weighted(common,random);add(server,rewards,entry,null,random);}
        List<AirDropConfig.Rare> successful=new ArrayList<>();
        for(var rare:config.rareRewards)if(unlocked(config,rare.loot,day)&&(rare.variants.isEmpty()||rare.variants.contains(variant.key))&&random.nextDouble()<Math.min(1,rare.chance(day)*variant.rarityMultiplier))successful.add(rare);
        if(!successful.isEmpty()){var rare=successful.get(random.nextInt(successful.size()));add(server,rewards,rare.loot,rare.key,random);}
        if(rewards.size()>config.maxLootStacks)throw new IllegalArgumentException("AirDrop loot exceeds maxLootStacks; reduce quantities/rolls or increase the cap");
        return rewards;
    }
    private AirDropLoot(){}
}
