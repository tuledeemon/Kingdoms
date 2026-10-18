package com.celik.kingdoms;

import java.util.*;

/** Server-wide administrator policy, separate from mayor-controlled land flags. */
public final class WarConfig {
    public boolean enabled=true, blockPearlTeleport=false, preventLavaCasting=false;
    public String scope="war_claims";
    public Set<String> instantBreakBlocks=new LinkedHashSet<>();
    public Set<String> blockedPlacementBlocks=new LinkedHashSet<>();
    public Set<String> blockedEntities=new LinkedHashSet<>();
    public Set<String> blockedExplosions=new LinkedHashSet<>();
    public Set<String> blockedExplosionBlockDamage=new LinkedHashSet<>();

    public void validate(){
        if(!Set.of("war_claims","war","global").contains(scope==null?"":scope))throw new IllegalArgumentException("War scope must be war_claims, war or global.");
        validate(instantBreakBlocks,false);validate(blockedPlacementBlocks,false);validate(blockedEntities,false);validate(blockedExplosions,true);validate(blockedExplosionBlockDamage,true);
        if(blockedEntities.contains("minecraft:player"))throw new IllegalArgumentException("Players cannot be banned as entities.");
    }
    private static void validate(Set<String> entries,boolean wildcard){
        if(entries==null||entries.size()>10000)throw new IllegalArgumentException("War restriction lists must contain at most 10000 identifiers.");
        for(String entry:entries)if(entry==null||!(wildcard&&entry.equals("*"))&&!entry.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Use a namespaced block/entity identifier, for example minecraft:obsidian.");
    }
    public static boolean matches(Set<String> entries,String key){return entries.contains("*")||entries.contains(key);}
}
