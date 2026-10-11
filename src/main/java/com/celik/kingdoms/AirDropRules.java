package com.celik.kingdoms;

import java.util.*;

/** Capture calculations have no Minecraft/client dependency. Progress is normal-speed milliseconds. */
public final class AirDropRules {
    public enum Phase { DESCENDING, CAPTURABLE, CLAIMED, EXPIRED, CANCELLED }
    public enum LootStatus { AVAILABLE, PENDING, COLLECTED, EXPIRED }
    public static final class Progress {
        public double millis;
        public long missingSince;
        public boolean contested;
    }
    public static final class Reward {
        public UUID id=UUID.randomUUID(),receiver;
        public String itemJson;
        public String rareKey;
        public long money;
        public LootStatus status=LootStatus.AVAILABLE;
    }
    public static final class Drop {
        public UUID id=UUID.randomUUID();
        public String dimension,tierName,variant,variantName,winner,winnerName,capturingSide;
        public int x,y,z,tierDay,captureRadius,verticalRadius,requiredMembers;
        public int settingsVersion;
        public int protectionRadius=5,soloControlStrength=3;
        public boolean largerGroupTakeover=true;
        public boolean scatterLoot=true;
        public String takeoverSide;
        public long takeoverSince,takeoverMillis=15000;
        public double startY,soloMultiplier,interferenceMultiplier;
        public long createdAt,landsAt,expiresAt,closedAt,captureMillis,graceMillis,checkedAt;
        public Phase phase=Phase.DESCENDING;
        public Set<UUID> recipients=new HashSet<>();
        public Map<String,Progress> progress=new LinkedHashMap<>();
        public List<Reward> loot=new ArrayList<>();
        public boolean active(){return phase==Phase.DESCENDING||phase==Phase.CAPTURABLE;}
        public boolean withinCaptureArea(double px,double pz){double dx=px-(x+.5),dz=pz-(z+.5);return active()&&dx*dx+dz*dz<=captureRadius*(double)captureRadius;}
        public boolean protectsBlock(int bx,int by,int bz){if(!active())return false;if(protectionRadius==0)return bx==x&&bz==z&&(by==y||by==y-1);double dx=bx-(double)x,dz=bz-(double)z;return dx*dx+dz*dz<=protectionRadius*(double)protectionRadius;}
        public double height(long now){double fraction=Math.max(0,Math.min(1,(now-createdAt)/(double)(landsAt-createdAt)));return startY+(y-startY)*fraction;}
        public double required(String side){return captureMillis*(side.startsWith("p:")?soloMultiplier:1);}
    }
    public static final class Data {
        public int schemaVersion=1;
        public long ageEpoch,nextEvent;
        public List<Drop> drops=new ArrayList<>();
    }
    public static long day(long epoch,long now){return 1+Math.max(0,now-epoch)/86_400_000L;}
    public static String kingdom(UUID id){return "k:"+id;}
    public static String solo(UUID id){return "p:"+id;}
    public static boolean quorum(Drop d,String side,int count){return count>=(side.startsWith("p:")?1:d.requiredMembers);}
    static int strength(Drop d,String side,int count){return side.startsWith("p:")?Math.multiplyExact(count,d.soloControlStrength):count;}
    /** Legacy independent timers keep only the furthest progressed side when loaded. */
    static void normalizeCapture(Drop d){if(d.settingsVersion==0){if(d.verticalRadius==6)d.verticalRadius=3;if(d.protectionRadius==25)d.protectionRadius=5;d.settingsVersion=1;}if(d.capturingSide==null&&!d.progress.isEmpty())d.capturingSide=d.progress.entrySet().stream().max(Comparator.comparingDouble((Map.Entry<String,Progress> e)->e.getValue().millis).thenComparingLong(e->-tieOrder(d,e.getKey()))).map(Map.Entry::getKey).orElse(null);if(d.capturingSide!=null)d.progress.keySet().removeIf(side->!side.equals(d.capturingSide));}
    /** One side owns the timer. A stronger qualifying side starts fresh; ties preserve the holder. */
    public static String advance(Drop d,Map<String,Integer> presence,long elapsed,long now){
        if(d.phase!=Phase.CAPTURABLE)return null;normalizeCapture(d);boolean reset=false;
        Progress p=d.capturingSide==null?null:d.progress.get(d.capturingSide);
        if(p!=null&&p.missingSince!=0&&now-p.missingSince>=d.graceMillis){d.progress.clear();d.capturingSide=null;p=null;reset=true;}
        String candidate=presence.keySet().stream().filter(side->quorum(d,side,presence.get(side))).max(Comparator.comparingInt((String side)->strength(d,side,presence.get(side))).thenComparingLong(side->-tieOrder(d,side))).orElse(null);
        if(d.capturingSide==null){if(candidate==null)return null;d.capturingSide=candidate;p=new Progress();d.progress.put(candidate,p);if(reset)elapsed=0;}
        else if(d.largerGroupTakeover&&candidate!=null&&!candidate.equals(d.capturingSide)&&strength(d,candidate,presence.get(candidate))>strength(d,d.capturingSide,presence.getOrDefault(d.capturingSide,0))){
            if(!candidate.equals(d.takeoverSide)){d.takeoverSide=candidate;d.takeoverSince=now;}
            if(now-d.takeoverSince<d.takeoverMillis){p.contested=true;if(!quorum(d,d.capturingSide,presence.getOrDefault(d.capturingSide,0))&&p.missingSince==0)p.missingSince=now;return null;}
            d.progress.clear();d.capturingSide=candidate;p=new Progress();d.progress.put(candidate,p);elapsed=0;d.takeoverSide=null;d.takeoverSince=0;
        }else{if(d.takeoverSide!=null)elapsed=0;d.takeoverSide=null;d.takeoverSince=0;}
        String owner=d.capturingSide;boolean resuming=p.missingSince!=0;p.contested=presence.entrySet().stream().anyMatch(e->!e.getKey().equals(owner)&&e.getValue()>0);
        if(!quorum(d,owner,presence.getOrDefault(owner,0))){if(p.missingSince==0)p.missingSince=now;if(d.graceMillis==0){d.progress.clear();d.capturingSide=null;}return null;}
        p.missingSince=0;double delta=(resuming?0:Math.max(0,elapsed))/(p.contested?d.interferenceMultiplier:1);p.millis=Math.min(d.required(owner),p.millis+delta);return p.millis+1e-6>=d.required(owner)?owner:null;
    }
    private static long tieOrder(Drop d,String side){return side==null?Long.MAX_VALUE:Integer.toUnsignedLong(Objects.hash(d.id,side));}
    /** All original reward indices retain their positions after collection and restart. */
    static List<Integer> chestSlots(Drop d,int page,int size){List<Integer> slots=new ArrayList<>();for(int i=0;i<size;i++)slots.add(i);if(d.scatterLoot)Collections.shuffle(slots,new Random(d.id.getMostSignificantBits()^d.id.getLeastSignificantBits()^page));return slots;}
    public static void validate(Data data){
        if(data.schemaVersion!=1||data.ageEpoch<0||data.nextEvent<0||data.drops==null)throw new IllegalStateException("Invalid AirDrop data");
        Set<UUID> ids=new HashSet<>(),rewards=new HashSet<>();
        for(Drop d:data.drops){
            if(d==null||d.id==null||!ids.add(d.id)||d.dimension==null||d.phase==null||d.createdAt<0||d.landsAt<=d.createdAt||d.expiresAt<=d.landsAt||!Double.isFinite(d.startY)||d.startY<d.y||d.captureRadius<1||d.verticalRadius<0||d.protectionRadius<0||d.protectionRadius>256||d.settingsVersion<0||d.settingsVersion>1||d.soloControlStrength<1||d.soloControlStrength>100||d.requiredMembers<1||d.captureMillis<1||d.graceMillis<0||!Double.isFinite(d.soloMultiplier)||d.soloMultiplier<1||!Double.isFinite(d.interferenceMultiplier)||d.interferenceMultiplier<1||d.progress==null||d.loot==null||d.recipients==null)throw new IllegalStateException("Invalid AirDrop record");
            if(d.capturingSide!=null&&(!validSide(d.capturingSide)||d.progress.size()!=1||!d.progress.containsKey(d.capturingSide)))throw new IllegalStateException("Invalid exclusive AirDrop capture");
            if(d.takeoverMillis<0||d.takeoverMillis>3600000||d.takeoverSince<0||d.takeoverSide!=null&&!validSide(d.takeoverSide))throw new IllegalStateException("Invalid AirDrop takeover state");
            if(d.phase==Phase.CLAIMED&&(d.winner==null||d.winnerName==null||d.recipients.isEmpty()))throw new IllegalStateException("Invalid AirDrop winner");
            for(var e:d.progress.entrySet())if(!validSide(e.getKey())||e.getValue()==null||!Double.isFinite(e.getValue().millis)||e.getValue().millis<0||e.getValue().missingSince<0)throw new IllegalStateException("Invalid AirDrop capture progress");
            for(Reward r:d.loot)if(r==null||r.id==null||!rewards.add(r.id)||r.itemJson==null||r.money<0||r.money>999999999999L||r.status==null||(r.status==LootStatus.PENDING||r.status==LootStatus.COLLECTED)&&r.receiver==null)throw new IllegalStateException("Invalid AirDrop loot record");
        }
    }
    private static boolean validSide(String key){try{if(!(key.startsWith("k:")||key.startsWith("p:")))return false;UUID.fromString(key.substring(2));return true;}catch(Exception e){return false;}}
    private AirDropRules(){}
}
