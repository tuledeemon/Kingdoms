package com.celik.kingdoms;

import java.util.*;

/** Separate AirDrop settings; all day values mean elapsed real-world 24-hour days. */
public final class AirDropConfig {
    public int settingsVersion;
    public boolean enabled=true, particles=true, sounds=true, titles=true, bossBars=true, allowChunkGeneration=true, requireGround=false, captureGlow=true, descentOutline=true, largerGroupTakeover=true;
    public int intervalMinutes=180, jitterMinutes=20, minimumPlayers=10, retrySeconds=60, maxDrops=6;
    public int descentSeconds=600, descentHeight=120, kingdomMembers=3, captureSeconds=90, graceSeconds=15;
    public int soloControlStrength=3;
    public int takeoverSeconds=15;
    public boolean parachute=true, scatterLoot=true;
    public float parachuteScale=1,parachuteSway=.12f;
    public String parachutePrimaryBlock="minecraft:orange_wool",parachuteSecondaryBlock="minecraft:white_wool",parachuteSeamBlock="minecraft:light_gray_wool",parachuteTrimBlock="minecraft:brown_wool",parachuteRopeBlock="minecraft:white_wool";
    public Map<Integer,Long> moneyByDay=new TreeMap<>(Map.of(1,1000L,3,10000L,5,50000L,7,100000L));
    public boolean artifactDropsHaveMoney=false;
    public double soloMultiplier=1.5, interferenceMultiplier=1.3;
    public int captureRadius=15, captureVerticalRadius=3, protectionRadius=5, nearbyRadius=96, pollTicks=20, checkpointSeconds=5;
    public int minSpawnDistance=320, maxSpawnDistance=5000, minDropSeparation=500, maxDropSeparation=3000;
    public int claimBufferBlocks=0, minLandingY=-64, maxLandingY=319, candidateAttempts=512, attemptsPerTick=1;
    public int particleIntervalTicks=10, ringPoints=96;
    public float dustSize=1.5f;
    public int captureExpiryMinutes=30, historyDays=7, menuSlot=38, maxLootStacks=108;
    public String dimension="minecraft:overworld", crateBlock="minecraft:barrel";
    public String startSound="minecraft:block.note_block.chime", landingSound="minecraft:entity.generic.explode", captureSound="minecraft:ui.toast.challenge_complete";
    public Map<Integer,Integer> playerThresholds=new TreeMap<>(Map.of(10,1,20,2));
    public List<Exclusion> excludedRegions=new ArrayList<>();
    public Set<String> excludedBiomes=new HashSet<>();
    public int netherUnlockDay=3,endUnlockDay=7;
    public List<Tier> lootTiers=AirDropLootDefaults.tiers();
    public List<Variant> variants=AirDropLootDefaults.variants();
    public List<Rare> rareRewards=AirDropLootDefaults.rare();
    public Map<String,Integer> itemUnlockDays=new LinkedHashMap<>();
    public String specialPool="";
    public double specialPoolChance=0.05;
    public Map<String,List<Loot>> specialPools=new LinkedHashMap<>();
    public Map<String,String> messages=new LinkedHashMap<>();
    public static final class Exclusion {public String dimension="minecraft:overworld";public int minX,minZ,maxX,maxZ;}
    public static final class Loot {
        public String item;
        public int weight=100,minCount=1,maxCount=1,minDay=1;
        public double chance=1;
        public Map<String,Integer> enchantments=new LinkedHashMap<>();
        public String potion="",name="";
        public List<String> lore=new ArrayList<>();
        public boolean glint;
        public Loot(){}
        Loot(String item,int weight,int min,int max){this.item=item;this.weight=weight;minCount=min;maxCount=max;}
    }
    public static final class Tier {
        public String name;
        public int fromDay,minRolls=5,maxRolls=8;
        public List<Loot> entries=new ArrayList<>();
        public List<Category> categories=new ArrayList<>();
        public Tier(){}
        Tier(String name,int day){this.name=name;fromDay=day;}
    }
    public static final class Category {public String key;public double chance;public List<Loot> entries=new ArrayList<>();public Category(){}Category(String key,double chance,List<Loot> entries){this.key=key;this.chance=chance;this.entries=entries;}}
    public static final class Variant {public String key,name;public int weight;public double rarityMultiplier=1;public Map<String,Double> categoryMultipliers=new LinkedHashMap<>();public Variant(){}Variant(String key,String name,int weight){this.key=key;this.name=name;this.weight=weight;}}
    public static final class Rare {public String key;public Loot loot;public Map<Integer,Double> chanceByDay=new TreeMap<>();public Set<String> variants=new HashSet<>();public Rare(){}Rare(String key,Loot loot,double... chances){this.key=key;this.loot=loot;int[] days={1,3,5,7};for(int i=0;i<4;i++)chanceByDay.put(days[i],chances[i]);}public double chance(long day){return chanceByDay.entrySet().stream().filter(e->e.getKey()<=day).max(Map.Entry.comparingByKey()).map(Map.Entry::getValue).orElse(0.0);}}
    private static Loot loot(String id,int weight,int min,int max){return new Loot("minecraft:"+id,weight,min,max);}
    private static Loot enchanted(String id,int weight,String enchantment,int level){Loot e=loot(id,weight,1,1);e.enchantments.put("minecraft:"+enchantment,level);return e;}
    private static Loot potion(String id,int weight){Loot e=loot("potion",weight,1,2);e.potion="minecraft:"+id;return e;}
    public void validate(){
        if(!Float.isFinite(parachuteScale)||parachuteScale<.25||parachuteScale>3||!Float.isFinite(parachuteSway)||parachuteSway<0||parachuteSway>1||parachutePrimaryBlock==null||parachuteSecondaryBlock==null||parachuteSeamBlock==null||parachuteTrimBlock==null||parachuteRopeBlock==null)throw new IllegalArgumentException("Invalid AirDrop parachute settings");
        if(moneyByDay==null||moneyByDay.entrySet().stream().anyMatch(e->e.getKey()==null||e.getValue()==null||e.getKey()<1||e.getValue()<0||e.getValue()>999999999999L))throw new IllegalArgumentException("Invalid AirDrop cash rewards");
        if(takeoverSeconds<0||takeoverSeconds>3600)throw new IllegalArgumentException("Invalid AirDrop takeover timer");
        if(rareRewards!=null)rareRewards.removeIf(r->r!=null&&"mythic_artifact".equals(r.key));
        if(settingsVersion<0||settingsVersion>1)throw new IllegalArgumentException("Unsupported AirDrop settings version");
        if(settingsVersion==0){if(captureVerticalRadius==6)captureVerticalRadius=3;if(protectionRadius==25)protectionRadius=5;settingsVersion=1;}
        if(intervalMinutes<1||intervalMinutes>5256000||jitterMinutes<0||jitterMinutes>10080||minimumPlayers<1||minimumPlayers>10000||retrySeconds<5||retrySeconds>86400||maxDrops<1||maxDrops>20)throw new IllegalArgumentException("Invalid AirDrop schedule/player limits");
        if(soloControlStrength<1||soloControlStrength>100)throw new IllegalArgumentException("Invalid AirDrop solo control strength");
        if(descentSeconds<1||descentSeconds>86400||descentHeight<1||descentHeight>256||kingdomMembers<1||kingdomMembers>100||captureSeconds<1||captureSeconds>86400||graceSeconds<0||graceSeconds>3600||!Double.isFinite(soloMultiplier)||soloMultiplier<1||soloMultiplier>100||!Double.isFinite(interferenceMultiplier)||interferenceMultiplier<1||interferenceMultiplier>100)throw new IllegalArgumentException("Invalid AirDrop capture timers");
        if(captureRadius<1||captureRadius>128||captureVerticalRadius<0||captureVerticalRadius>64||protectionRadius<0||protectionRadius>256||nearbyRadius<Math.max(captureRadius,protectionRadius)||nearbyRadius>512||pollTicks<1||pollTicks>100||checkpointSeconds<1||checkpointSeconds>60)throw new IllegalArgumentException("Invalid AirDrop radius/update settings");
        if(particleIntervalTicks<1||particleIntervalTicks>200||ringPoints<12||ringPoints>256||!Float.isFinite(dustSize)||dustSize<.1f||dustSize>4)throw new IllegalArgumentException("Invalid AirDrop particle settings");
        if(minSpawnDistance<0||maxSpawnDistance<=minSpawnDistance||maxSpawnDistance>1000000||minDropSeparation<Math.max(captureRadius,protectionRadius)*2||maxDropSeparation<minDropSeparation||maxDropSeparation>2000000||claimBufferBlocks<0||claimBufferBlocks>1024||minLandingY>maxLandingY||candidateAttempts<1||candidateAttempts>10000||attemptsPerTick<1||attemptsPerTick>4||captureExpiryMinutes<1||captureExpiryMinutes>10080||historyDays<1||historyDays>365||menuSlot<0||menuSlot>44)throw new IllegalArgumentException("Invalid AirDrop location/expiry settings");
        if(dimension==null||crateBlock==null||startSound==null||landingSound==null||captureSound==null||messages==null||excludedRegions==null||excludedBiomes==null||playerThresholds==null||playerThresholds.isEmpty()||playerThresholds.entrySet().stream().anyMatch(e->e.getKey()==null||e.getKey()<1||e.getValue()==null||e.getValue()<1||e.getValue()>maxDrops))throw new IllegalArgumentException("Invalid AirDrop collections/text");
        for(Exclusion e:excludedRegions)if(e==null||e.dimension==null||e.minX>e.maxX||e.minZ>e.maxZ)throw new IllegalArgumentException("Invalid AirDrop excluded region");
        if(lootTiers==null||lootTiers.isEmpty()||lootTiers.size()>50||specialPool==null||specialPools==null||maxLootStacks<36||maxLootStacks>2304||!Double.isFinite(specialPoolChance)||specialPoolChance<0||specialPoolChance>1)throw new IllegalArgumentException("Invalid AirDrop loot tiers");
        Set<Integer> days=new HashSet<>();for(Tier tier:lootTiers){if(tier==null||tier.name==null||tier.fromDay<1||!days.add(tier.fromDay)||tier.minRolls<1||tier.maxRolls<tier.minRolls||tier.maxRolls>36)throw new IllegalArgumentException("Invalid AirDrop loot tier");validateLoot(tier.entries);if(tier.entries.stream().noneMatch(e->e.chance==1))throw new IllegalArgumentException("Each AirDrop tier needs guaranteed common fallback entries");}
        if(!days.contains(1))throw new IllegalArgumentException("AirDrop loot needs a day-one tier");
        specialPools.values().forEach(AirDropConfig::validateLoot);if(!specialPool.isEmpty()&&!specialPools.containsKey(specialPool))throw new IllegalArgumentException("Unknown active AirDrop special pool");
        if(netherUnlockDay<1||endUnlockDay<netherUnlockDay||itemUnlockDays==null||itemUnlockDays.entrySet().stream().anyMatch(e->e.getKey()==null||e.getValue()==null||e.getValue()<1)||variants==null||variants.isEmpty()||rareRewards==null)throw new IllegalArgumentException("Invalid AirDrop unlocks/variants");
        Set<String> keys=new HashSet<>();for(Variant v:variants)if(v==null||v.key==null||v.name==null||!keys.add(v.key)||v.weight<1||v.weight>1000000||!Double.isFinite(v.rarityMultiplier)||v.rarityMultiplier<0||v.rarityMultiplier>5||v.categoryMultipliers==null||v.categoryMultipliers.values().stream().anyMatch(m->m==null||!Double.isFinite(m)||m<0||m>5))throw new IllegalArgumentException("Invalid AirDrop variant; multipliers must be 0..5");
        for(Tier t:lootTiers){if(t.categories==null||t.categories.size()>20||t.categories.size()>t.maxRolls)throw new IllegalArgumentException("Invalid AirDrop categories");Set<String> categoryKeys=new HashSet<>();for(Category c:t.categories){if(c==null||c.key==null||!categoryKeys.add(c.key)||!Double.isFinite(c.chance)||c.chance<0||c.chance>1)throw new IllegalArgumentException("Invalid AirDrop category chance");validateLoot(c.entries);}}
        Set<String> rareKeys=new HashSet<>();for(Rare r:rareRewards){if(r==null||r.key==null||!rareKeys.add(r.key)||r.loot==null||r.chanceByDay==null||r.chanceByDay.entrySet().stream().anyMatch(e->e.getKey()==null||e.getKey()<1||e.getValue()==null||!Double.isFinite(e.getValue())||e.getValue()<0||e.getValue()>1)||r.variants==null||!keys.containsAll(r.variants))throw new IllegalArgumentException("Invalid AirDrop rare reward");validateLoot(List.of(r.loot));}
    }
    private static void validateLoot(List<Loot> entries){if(entries==null||entries.isEmpty()||entries.size()>1000)throw new IllegalArgumentException("Invalid AirDrop loot pool");for(Loot e:entries)if(e==null||e.item==null||e.item.isBlank()||e.weight<1||e.weight>1000000||e.minDay<1||e.minCount<1||e.maxCount<e.minCount||e.maxCount>64||!Double.isFinite(e.chance)||e.chance<=0||e.chance>1||e.enchantments==null||e.enchantments.entrySet().stream().anyMatch(q->q.getKey()==null||q.getValue()==null||q.getValue()<1||q.getValue()>255)||e.potion==null||e.name==null||e.lore==null||e.lore.size()>20||e.lore.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Invalid AirDrop loot entry");}
    public int dropsFor(int players){if(players<minimumPlayers)return 0;return playerThresholds.entrySet().stream().filter(e->e.getKey()<=players).max(Map.Entry.comparingByKey()).map(e->e.getValue()).orElse(0);}
    public long money(long day){return moneyByDay.entrySet().stream().filter(e->e.getKey()<=day).max(Map.Entry.comparingByKey()).map(Map.Entry::getValue).orElse(0L);}
    public Tier tier(long day){return lootTiers.stream().filter(t->t.fromDay<=day).max(Comparator.comparingInt(t->t.fromDay)).orElseThrow();}
    public String message(String key,String fallback){return messages.getOrDefault(key,fallback);}
}
