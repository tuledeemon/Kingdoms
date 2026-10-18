package com.celik.kingdoms;

import java.util.*;
import java.util.function.Function;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import static net.minecraft.commands.Commands.*;

/** Registered only below the existing operator-gated /kingdomadmin root. */
public final class WarAdmin {
    private static final List<String> LISTS=List.of("block","placement","entity","explosion","blockdamage");
    static LiteralArgumentBuilder<CommandSourceStack> node(){
        var root=literal("war").executes(c->run(c,()->status(c)));
        root.then(literal("set").then(argument("key",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(AdminSettings.keys(WarConfig.class),b)).then(argument("value",StringArgumentType.greedyString()).suggests((c,b)->SharedSuggestionProvider.suggest(StringArgumentType.getString(c,"key").equals("scope")?List.of("war_claims","war","global"):List.of("true","false"),b)).executes(c->run(c,()->{String key=StringArgumentType.getString(c,"key"),value=StringArgumentType.getString(c,"value");AdminSettings.update(s(),next->AdminSettings.set(next.warRestrictions,key,value));say(c,"War restriction saved: "+key+" = "+value);})))));
        for(String name:LISTS){
            var list=literal(name).executes(c->run(c,()->show(c,name)));
            list.then(literal("list").executes(c->run(c,()->show(c,name))).then(argument("page",IntegerArgumentType.integer(1,500)).executes(c->run(c,()->show(c,name,IntegerArgumentType.getInteger(c,"page"))))));
            list.then(literal("clear").executes(c->run(c,()->{AdminSettings.update(s(),next->entries(next.warRestrictions,name).clear());say(c,"Cleared war restriction list: "+name);} )));
            for(boolean add:new boolean[]{true,false})list.then(literal(add?"add":"remove").then(argument("id",StringArgumentType.greedyString()).suggests((c,b)->SharedSuggestionProvider.suggest(add?suggestions(name):entries(s().config.warRestrictions,name),b)).executes(c->run(c,()->{
                String id=canonical(name,StringArgumentType.getString(c,"id"),add);AdminSettings.update(s(),next->{Set<String> entries=entries(next.warRestrictions,name);if(add)entries.add(id);else entries.remove(id);});say(c,(add?"Added ":"Removed ")+id+" in war list "+name+".");
            }))));
            root.then(list);
        }
        return root;
    }
    static Set<String> entries(WarConfig config,String name){return switch(name){case "block"->config.instantBreakBlocks;case "placement"->config.blockedPlacementBlocks;case "entity"->config.blockedEntities;case "explosion"->config.blockedExplosions;case "blockdamage"->config.blockedExplosionBlockDamage;default->throw new IllegalArgumentException("Unknown war restriction list.");};}
    static String canonical(String kind,String value,boolean adding){
        boolean explosive=kind.equals("explosion")||kind.equals("blockdamage");if(explosive&&(value.equals("*")||value.equalsIgnoreCase("all")))return "*";
        Identifier id=Identifier.tryParse(value.toLowerCase(Locale.ROOT));if(id==null)throw new IllegalArgumentException("Invalid block/entity identifier.");String key=id.toString();
        if(!adding)return key;
        if(kind.equals("block")||kind.equals("placement")){
            if(!BuiltInRegistries.BLOCK.containsKey(id))throw new IllegalArgumentException("Unknown block: "+key);
            var block=BuiltInRegistries.BLOCK.getValue(id);if(block.defaultBlockState().isAir())throw new IllegalArgumentException("Air cannot be banned as a block.");
            if(kind.equals("block")&&block.defaultDestroyTime()<0)throw new IllegalArgumentException("Unbreakable blocks cannot be made instant to mine.");
        }else if(!(explosive&&Set.of("minecraft:bed","minecraft:respawn_anchor","minecraft:other").contains(key))){
            if(!BuiltInRegistries.ENTITY_TYPE.containsKey(id))throw new IllegalArgumentException("Unknown entity type: "+key);
            if(BuiltInRegistries.ENTITY_TYPE.getValue(id)==EntityTypes.PLAYER)throw new IllegalArgumentException("Players cannot be banned as entities or explosion sources.");
        }
        return key;
    }
    private static Collection<String> suggestions(String kind){
        if(kind.equals("block")||kind.equals("placement"))return BuiltInRegistries.BLOCK.keySet().stream().map(Object::toString).sorted().toList();
        List<String> values=new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Object::toString).filter(id->!id.equals("minecraft:player")).toList());
        if(!kind.equals("entity"))values.addAll(List.of("*","minecraft:bed","minecraft:respawn_anchor","minecraft:other"));Collections.sort(values);return values;
    }
    private static void status(CommandContext<CommandSourceStack> c){WarConfig config=s().config.warRestrictions;say(c,"War restrictions • Enabled: "+config.enabled+" • Scope: "+config.scope);say(c,"Block pearl teleport: "+config.blockPearlTeleport+" • Prevent lava casting: "+config.preventLavaCasting);for(String name:LISTS){Set<String> values=entries(config,name);say(c,name+": "+values.size()+" entries • "+(values.isEmpty()?"none":String.join(", ",values.stream().sorted().limit(3).toList())+(values.size()>3?" …":"")));}}
    private static void show(CommandContext<CommandSourceStack> c,String name){show(c,name,1);}
    private static void show(CommandContext<CommandSourceStack> c,String name,int page){List<String> values=entries(s().config.warRestrictions,name).stream().sorted().toList();int pages=Math.max(1,(values.size()+19)/20);if(page>pages)throw new IllegalArgumentException("This list has "+pages+" pages.");say(c,name+" • page "+page+" / "+pages+" • "+values.size()+" entries");say(c,values.isEmpty()?"none":String.join(", ",values.subList((page-1)*20,Math.min(values.size(),page*20))));}
    private static KingdomService s(){KingdomService s=KingdomsMod.service;if(s==null)throw new IllegalArgumentException("Kingdoms is not ready.");return s;}
    private static void say(CommandContext<CommandSourceStack> c,String message){c.getSource().sendSuccess(()->Component.literal(Ui.caps(message)),false);}
    private static int run(CommandContext<CommandSourceStack> c,Runnable action){try{action.run();return 1;}catch(IllegalArgumentException e){c.getSource().sendFailure(Component.literal(Ui.caps(e.getMessage())));return 0;}}
    private WarAdmin(){}
}
