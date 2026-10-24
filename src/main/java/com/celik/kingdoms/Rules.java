package com.celik.kingdoms;

import java.util.*;

/** Minecraft-independent authorization rules; exercised by the unit suite. */
public final class Rules {
    public enum Permission { PLACE, BREAK, INTERACT, CONTAINERS, ENTITIES, PVP, DEPOSIT, WITHDRAW }
    public enum Flag { EXPLOSIONS, FIRE, FLUIDS, MOBS, MOB_GRIEFING, PISTONS, AUTOMATION, PVP }
    public enum BeaconPower {
        SPEED(true), HASTE(true), RESISTANCE(true), JUMP_BOOST(true), STRENGTH(true), REGENERATION(true),
        SLOWNESS(false), BLINDNESS(false), MINING_FATIGUE(false), GLOWING(false);
        private final boolean beneficial;
        BeaconPower(boolean beneficial){this.beneficial=beneficial;}
        public boolean beneficial(){return beneficial;}
        public String key(){return name().toLowerCase(Locale.ROOT);}
    }
    public static class Kingdom {
        public UUID id = UUID.randomUUID();
        public String name;
        public String displayName;
        public String iconItem = "minecraft:paper";
        public int highlightColor = -1;
        public Map<UUID, String> members = new LinkedHashMap<>();
        public Set<UUID> mayors = new HashSet<>();
        public Map<UUID, Set<Permission>> grants = new HashMap<>();
        public Set<Permission> memberPermissions = EnumSet.of(Permission.PLACE, Permission.BREAK, Permission.INTERACT, Permission.CONTAINERS, Permission.ENTITIES, Permission.DEPOSIT);
        public Set<Permission> publicPermissions = EnumSet.noneOf(Permission.class);
        public Set<Flag> flags = EnumSet.noneOf(Flag.class);
        public Set<String> claims = new HashSet<>();
        public Set<String> outposts = new HashSet<>();
        public Set<UUID> allowedMobs = new HashSet<>();
        public Set<UUID> allies = new LinkedHashSet<>();
        public long treasury, debt, nextUpkeep, overdueSince, warningAcknowledged, protectedUntil, cooldownUntil;
        public int memberCap, claimCap;
        public Core core;
        public Kingdom() {}
        public boolean mayor(UUID player) { return mayors.contains(player); }
        public String display() { return displayName == null || displayName.isBlank() ? name : displayName; }
    }
    /** A random kingdom UUID selects a bright hue; old saves keep the same hue until migrated. */
    public static int highlightColor(UUID id) {
        int hue=Math.floorMod(id.hashCode(),1536),r=255,g=64,b=64;
        int rise=64+(hue%256)*191/255,fall=255-(hue%256)*191/255;
        switch(hue/256){case 0->g=rise;case 1->{r=fall;g=255;}case 2->{r=64;g=255;b=rise;}case 3->{r=64;g=fall;b=255;}case 4->{r=rise;b=255;}case 5->b=fall;}
        return r<<16|g<<8|b;
    }
    /** A core keeps its owner even when another kingdom holds it. A nonce invalidates old items. */
    public static class Core {
        public UUID nonce = UUID.randomUUID();
        public UUID tributeTo, carriedByKingdom;
        public String dimension, block = "minecraft:stone";
        public int x, y, z;
        public boolean carried;
        public long takenAt, coveredSince;
        public boolean skyMoveBlocked;
        public Map<UUID, Integer> taxRemainders = new HashMap<>();
        public Core() {}
        public String claimKey(){return dimension+"|"+(x>>4)+"|"+(z>>4);}
    }
    public static boolean holdsForeignCore(State state,UUID holder){
        return state.kingdoms.values().stream().anyMatch(k->!k.id.equals(holder)&&k.core!=null&&(holder.equals(k.core.tributeTo)||k.core.carried&&holder.equals(k.core.carriedByKingdom)));
    }
    public record Tribute(long amount,int remainder) {}
    public static Tribute tribute(long income,int percent,int remainder){
        if(income<0||percent<0||percent>100||remainder<0||remainder>=100)throw new IllegalArgumentException("Invalid tribute calculation");
        java.math.BigInteger value=java.math.BigInteger.valueOf(income).multiply(java.math.BigInteger.valueOf(percent)).add(java.math.BigInteger.valueOf(remainder));
        java.math.BigInteger[] parts=value.divideAndRemainder(java.math.BigInteger.valueOf(100));
        return new Tribute(parts[0].longValueExact(),parts[1].intValueExact());
    }
    public static class BeaconEffect {
        public UUID id = UUID.randomUUID();
        public UUID sourceKingdom, targetKingdom;
        public BeaconPower power;
        public int level = 1;
        public long expiresAt;
        public boolean hostile;
        public BeaconEffect() {}
        public boolean active(long now){return expiresAt>now;}
    }
    public static class War {
        public UUID id = UUID.randomUUID();
        public UUID first, second;
        public Set<UUID> firstAllies = new LinkedHashSet<>(), secondAllies = new LinkedHashSet<>();
        public long declaredAt, startedAt, endsAt, cooldownMillis;
        public Set<Integer> announcedMilestones = new HashSet<>();
        public boolean startAnnounced;
        public Set<UUID> side(boolean attacking) { Set<UUID> ids=new LinkedHashSet<>();ids.add(attacking?first:second);Set<UUID> allies=attacking?firstAllies:secondAllies;if(allies!=null)ids.addAll(allies);return ids; }
        public Set<UUID> participants() { Set<UUID> ids=side(true);ids.addAll(side(false));return ids; }
        public boolean attacker(UUID k) { return Objects.equals(first,k)||firstAllies!=null&&firstAllies.contains(k); }
        public boolean defender(UUID k) { return Objects.equals(second,k)||secondAllies!=null&&secondAllies.contains(k); }
        public boolean involves(UUID k) { return attacker(k)||defender(k); }
        /** Legacy leader lookup; use opponents for all opposing participants. */
        public UUID opponent(UUID k) { return attacker(k) ? second : first; }
        public Set<UUID> opponents(UUID k) { return involves(k)?side(!attacker(k)):Set.of(); }
        public boolean opposes(UUID a,UUID b) { return attacker(a)&&defender(b)||defender(a)&&attacker(b); }
        public boolean sameSide(UUID a,UUID b) { return involves(a)&&involves(b)&&!opposes(a,b); }
        public boolean preparing(long now) { return now < startedAt && now < endsAt; }
        public boolean active(long now) { return startedAt <= now && now < endsAt; }
    }
    public static class AllianceRequest {
        public UUID from, to;
        public long expiresAt;
    }
    public static class WarInvite {
        public UUID war, from, to;
        public boolean attacking;
    }
    public static class State {
        public Set<UUID> airDropMoneyReceipts=new HashSet<>();
        public int schemaVersion = 1;
        public Map<UUID, Kingdom> kingdoms = new LinkedHashMap<>();
        public List<War> wars = new ArrayList<>();
        public List<AllianceRequest> allianceRequests = new ArrayList<>();
        public List<WarInvite> warInvites = new ArrayList<>();
        public Map<UUID, UUID> invitations = new HashMap<>();
        public Map<UUID, Long> playerCooldowns = new HashMap<>();
        public Map<UUID, Map<UUID,Long>> rejoinCooldowns = new HashMap<>();
        public List<BeaconEffect> beaconEffects = new ArrayList<>();
    }
    public static Kingdom membership(State state, UUID player) {
        return state.kingdoms.values().stream().filter(k -> k.members.containsKey(player)).findFirst().orElse(null);
    }
    /** A vulnerable wartime phase only. */
    public static War war(State state, UUID kingdom, long now) {
        return state.wars.stream().filter(w -> w.active(now) && w.involves(kingdom)).findFirst().orElse(null);
    }
    /** Preparation or active wartime, used to freeze membership, claims, protection, and second wars. */
    public static War conflict(State state, UUID kingdom, long now) {
        return state.wars.stream().filter(w -> w.endsAt > now && w.involves(kingdom)).findFirst().orElse(null);
    }
    public static boolean enemy(State state, Kingdom land, UUID player, long now) {
        War w = war(state, land.id, now);
        Kingdom own = membership(state, player);
        return w != null && own != null && w.opposes(land.id,own.id);
    }
    public static boolean allied(State state,UUID first,UUID second) { Kingdom a=state.kingdoms.get(first),b=state.kingdoms.get(second);return a!=null&&b!=null&&a.allies.contains(second)&&b.allies.contains(first); }
    public static boolean permitted(State state, Kingdom land, UUID player, Permission permission, long now) {
        if (land == null) return true;
        boolean physical = permission != Permission.DEPOSIT && permission != Permission.WITHDRAW;
        War war=physical?war(state,land.id,now):null;
        if(war!=null){
            if(land.members.containsKey(player)||enemy(state,land,player,now))return true;
            Kingdom own=membership(state,player);
            if(own==null||!war.sameSide(land.id,own.id))return false;
        }
        if (land.mayor(player)) return true;
        return land.grants.getOrDefault(player, Set.of()).contains(permission)
            || (land.members.containsKey(player) && land.memberPermissions.contains(permission))
            || land.publicPermissions.contains(permission);
    }
    public static boolean flag(State state, Kingdom k, Flag flag, long now) {
        return k == null || war(state, k.id, now) != null || k.flags.contains(flag);
    }
    public static boolean majority(int online, int members) { return members > 0 && online > members / 2; }
    public static String warJoinFailure(State state,War war,Kingdom kingdom,boolean attacking,int sideCap,boolean requireMajority,Set<UUID> online,long now){
        if(!war.preparing(now))return "Allies can join only during war preparation.";
        if(conflict(state,kingdom.id,now)!=null)return "This kingdom is already preparing for or fighting a war.";
        if(kingdom.protectedUntil>now)return "This kingdom has war protection.";
        if(kingdom.cooldownUntil>now)return "This kingdom is on war cooldown.";
        if(war.side(attacking).size()>=sideCap)return "This war side has reached its kingdom limit.";
        for(UUID opponent:war.side(!attacking))if(allied(state,kingdom.id,opponent))return "This kingdom is allied with a kingdom on the opposing side.";
        if(requireMajority&&!majority((int)kingdom.members.keySet().stream().filter(online::contains).count(),kingdom.members.size()))return "More than half of the joining kingdom's members must be online.";
        return null;
    }
    public static String declarationFailure(State s, Kingdom attacker, Kingdom defender, UUID actor, Set<UUID> online, long now) {
        if (!attacker.mayor(actor)) return "Only a mayor can declare war.";
        if (attacker.id.equals(defender.id)) return "You cannot attack your own kingdom.";
        if (allied(s,attacker.id,defender.id)) return "End your alliance before declaring war on this kingdom.";
        if (conflict(s, attacker.id, now) != null || conflict(s, defender.id, now) != null) return "One of the kingdoms is already preparing for or fighting a war.";
        if (attacker.protectedUntil > now || defender.protectedUntil > now) return "One of the kingdoms has war protection.";
        if (attacker.cooldownUntil > now || defender.cooldownUntil > now) return "One of the kingdoms is on war cooldown.";
        if (!majority((int)defender.members.keySet().stream().filter(online::contains).count(), defender.members.size())) return "More than half of the defending members must be online.";
        if (defender.mayors.stream().noneMatch(online::contains)) return "At least one defending mayor must be online.";
        return null;
    }
    private Rules() {}
}
