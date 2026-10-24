package com.celik.kingdoms;
import java.util.*;
/** Drop probabilities and ability balance; vanilla item stacks own tool durability. */
public final class ArtifactConfig {
    public boolean enabled=true,announcements=true,rebindOnTrade=true;
    public int abilityBalanceVersion=2;
    public int durabilityBalanceVersion=1;
    public int fixerCount=2,repairLevels=5,upgradeIngots=3;
    public double fixerChance=.5,repairFraction=.25;
    public Map<Integer,Double> chanceByDay=new TreeMap<>(Map.of(1,.001,3,.0015,5,.002,7,.003));
    public String fixerItem="minecraft:echo_shard";
    public Map<String,Type> types=defaults();
    public static final class Variant {
        public double weight=1,chance=1,value,durationSeconds;
        public int amplifier;
        public String rarity;
        public Variant(){}
        Variant(double value,double duration,String rarity){this.value=value;durationSeconds=duration;this.rarity=rarity;}
    }
    public static final class Type {
        public String name,baseItem,rarity;
        public boolean enabled=true,netheriteUpgradeable=true;
        // Zero selects normal item durability. Guardian Angel uses charges instead.
        public int weight,minDay=1,maxUses,upgradedMaxUses,cooldownSeconds;
        public double upgradedValue,upgradedDurationSeconds;
        public List<Variant> variants=new ArrayList<>();
        public Type(){}
        Type(String name,String item,String rarity,int weight,int durability,double value,double duration){
            this.name=name;baseItem="minecraft:"+item;this.rarity=rarity;this.weight=weight;maxUses=durability;
            upgradedValue=value;upgradedDurationSeconds=duration;variants.add(new Variant(value,duration,rarity));
        }
    }
    public static final Set<String> KEYS=Set.of("thunder_axe","thorns_shield","frostbite_sword","multi_pickaxe","vampire_sword","void_scythe","guardian_angel","wind_burst_spear");
    static final Set<String> LEGACY_KEYS=Set.of("smelter_pickaxe","healing_hoe","unbreakable_pickaxe","unbreakable_sword","berserker_axe","phantom_bow","treasure_digger","guardian_shield","miners_fortune","soulbound_pickaxe","chrono_tool");
    static Map<String,Type> defaults(){
        Map<String,Type> m=new LinkedHashMap<>();
        m.put("thunder_axe",new Type("Thunder Axe","diamond_axe","legendary",35,512,0,0));m.get("thunder_axe").upgradedMaxUses=768;
        m.put("thorns_shield",new Type("Thorns Shield","shield","mythic",10,0,1,0));
        m.put("frostbite_sword",new Type("Frostbite Sword","diamond_sword","arcane",80,0,0,1));m.get("frostbite_sword").upgradedDurationSeconds=1.5;
        m.put("multi_pickaxe",new Type("Multi Pickaxe","diamond_pickaxe","legendary",35,0,0,0));
        m.put("vampire_sword",new Type("Vampire Sword","diamond_sword","relic",120,0,.25,0));m.get("vampire_sword").upgradedValue=.5;
        m.put("void_scythe",new Type("Void Scythe","diamond_hoe","mythic",10,1000,0,2));
        m.put("guardian_angel",new Type("Guardian Angel","heart_of_the_sea","legendary",35,100,0,3));m.get("guardian_angel").netheriteUpgradeable=false;
        m.put("wind_burst_spear",new Type("Wind Burst Spear","diamond_spear","relic",120,0,0,0));m.get("wind_burst_spear").variants.getFirst().amplifier=2;
        return m;
    }
    public double chance(long day){return chanceByDay.entrySet().stream().filter(e->e.getKey()<=day).max(Map.Entry.comparingByKey()).map(Map.Entry::getValue).orElse(0d);}
    public void validate(){
        if(abilityBalanceVersion!=2||fixerCount<1||fixerCount>64||repairLevels<1||repairLevels>39||upgradeIngots<1||upgradeIngots>64||!prob(fixerChance)||!prob(repairFraction)||repairFraction==0||fixerItem==null||chanceByDay==null||types==null||!types.keySet().equals(KEYS))throw new IllegalArgumentException("Invalid artifact settings");
        if(chanceByDay.entrySet().stream().anyMatch(e->e.getKey()<1||!prob(e.getValue())))throw new IllegalArgumentException("Invalid artifact drop chance table");
        for(var e:types.entrySet()){
            Type t=e.getValue();if(t==null||t.name==null||t.baseItem==null||!rarity(t.rarity)||t.weight<1||t.weight>1000000||t.minDay<1||t.maxUses<0||t.maxUses>10000000||t.upgradedMaxUses<0||t.upgradedMaxUses>10000000||t.cooldownSeconds<0||t.cooldownSeconds>604800||!value(t.upgradedValue)||!duration(t.upgradedDurationSeconds)||t.variants==null||t.variants.isEmpty()||e.getKey().equals("guardian_angel")&&t.maxUses<1)throw new IllegalArgumentException("Invalid artifact type: "+e.getKey());
            for(Variant v:t.variants)if(v==null||!Double.isFinite(v.weight)||v.weight<=0||!prob(v.chance)||!value(v.value)||!duration(v.durationSeconds)||v.amplifier<0||v.amplifier>3||!rarity(v.rarity))throw new IllegalArgumentException("Invalid artifact variant: "+e.getKey());
        }
    }
    private static boolean rarity(String r){return r!=null&&Set.of("relic","arcane","legendary","mythic").contains(r);}
    private static boolean value(double n){return Double.isFinite(n)&&n>=0&&n<=1;}
    private static boolean duration(double n){return Double.isFinite(n)&&n>=0&&n<=60;}
    private static boolean prob(double n){return Double.isFinite(n)&&n>=0&&n<=1;}
}
