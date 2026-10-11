package com.celik.kingdoms;
import java.util.*;

/** Persistent identities authenticate abilities; native stacks own tool durability. */
public final class ArtifactRules {
    public static final class Record {
        public UUID id=UUID.randomUUID(),nonce=UUID.randomUUID(),drop,owner;
        public String type,name,baseItem,rarity;
        public int abilityBalanceVersion=2;
        public int uses,maxUses,cooldownSeconds,revision;
        public double chance,value,durationSeconds,upgradedValue,upgradedDurationSeconds;
        public double legacyRemainingFraction=1;
        public int upgradedMaxUses,amplifier;
        public long readyAt,nextHealingRollAt;
        public boolean healingActive;
        public boolean upgraded,retired,delivered;
    }
    public static final class Fixer {public UUID id=UUID.randomUUID(),nonce=UUID.randomUUID(),drop;public boolean consumed;}
    public static final class Data {
        public int schemaVersion=1;
        public Map<UUID,Record> records=new LinkedHashMap<>();
        public Map<UUID,Fixer> fixers=new LinkedHashMap<>();
        public Set<String> placedBlocks=new HashSet<>();
    }
    public static List<String> eligible(ArtifactConfig config,Data data,long day){
        return config.types.entrySet().stream().filter(e->e.getValue().enabled&&e.getValue().minDay<=day).map(Map.Entry::getKey).toList();
    }
    public static Record generate(String key,ArtifactConfig.Type type,Random random){
        double roll=random.nextDouble()*type.variants.stream().mapToDouble(v->v.weight).sum();ArtifactConfig.Variant v=type.variants.getLast();for(var choice:type.variants){roll-=choice.weight;if(roll<0){v=choice;break;}}
        Record r=new Record();r.type=key;r.name=type.name;r.baseItem=type.baseItem;r.rarity=v.rarity;r.uses=r.maxUses=type.maxUses;r.cooldownSeconds=type.cooldownSeconds;r.chance=v.chance;r.value=v.value;r.durationSeconds=v.durationSeconds;r.amplifier=v.amplifier;r.upgradedValue=type.upgradedValue;r.upgradedDurationSeconds=type.upgradedDurationSeconds;r.upgradedMaxUses=type.upgradedMaxUses;return r;
    }
    public static boolean active(Record r,long now){return r!=null&&!r.retired&&(!r.type.equals("guardian_angel")||r.uses>0)&&now>=r.readyAt;}
    public static boolean consume(Record r,long now){if(r==null||!r.type.equals("guardian_angel")||!active(r,now))return false;r.uses--;r.readyAt=now+r.cooldownSeconds*1000L;r.revision++;return true;}
    public static int repairedUses(Record r,double fraction){return Math.min(r.maxUses,r.uses+Math.max(1,(int)Math.ceil(r.maxUses*fraction)));}
    public static void validate(Data data){if(data.schemaVersion!=1||data.records==null||data.fixers==null||data.placedBlocks==null)throw new IllegalStateException("Invalid artifact save");for(var e:data.records.entrySet()){Record r=e.getValue();if(r==null||!e.getKey().equals(r.id)||r.nonce==null||r.type==null||!(ArtifactConfig.KEYS.contains(r.type)||r.retired&&ArtifactConfig.LEGACY_KEYS.contains(r.type))||r.uses<0||r.uses>r.maxUses||r.maxUses<0||r.baseItem==null||r.name==null||r.rarity==null||r.revision<0||r.abilityBalanceVersion!=2||!Double.isFinite(r.chance)||r.chance<0||r.chance>1||r.nextHealingRollAt<0)throw new IllegalStateException("Invalid artifact identity");}for(var e:data.fixers.entrySet())if(e.getValue()==null||!e.getKey().equals(e.getValue().id)||e.getValue().nonce==null)throw new IllegalStateException("Invalid artifact fixer");}
    private ArtifactRules(){}
}

