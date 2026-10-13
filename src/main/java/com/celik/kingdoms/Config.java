package com.celik.kingdoms;

import java.util.*;
import static com.celik.kingdoms.Rules.*;

public final class Config {
    public int schemaVersion = 1;
    public AirDropConfig airDrops = new AirDropConfig();
    public ArtifactConfig artifacts = new ArtifactConfig();
    public BlueMapConfig blueMap = new BlueMapConfig();
    public long creationFee = 100_000, expansionFee = 25_000, outpostFee = 150_000, upkeepPerMember = 1_000, upkeepBase = 0, protectionFee = 1_000_000;
    public int maxMembers = 20, maxClaims = 64, initialClaims = 1, kingdomCreationBufferChunks = 16, spawnClaimProtectionChunks = 16, outpostClaimWeight = 6, maxOutposts = 4;
    public long upkeepIntervalMinutes = 1440, gracePeriodMinutes = 4320, warMinutes = 15, warCooldownMinutes = 120, protectionMinutes = 1440, newKingdomProtectionMinutes = 1440, beaconDurationMinutes = 1440;
    public int warPreparationSeconds = 180;
    public int coreReturnMinutes = 10, coreTaxPercent = 20, kingdomRejoinCooldownMinutes = 60;
    public boolean coreParticles = true;
    public boolean coreGlow = true;
    public boolean coreGlowOnlyDuringWar = true;
    public int coreWarOutlineThicknessMilli = 60;
    public int coreHighlightRange = 48, coreParticleIntervalTicks = 10, coreDustSizePercent = 45, coreOutlineThicknessMilli = 12;
    public Set<Integer> warCountdownMilestonesSeconds = new LinkedHashSet<>(List.of(180, 60, 30, 10, 5, 4, 3, 2, 1));
    public boolean adjacentExpansion = true, allowMultipleDimensions = false, allowPublicPermissions = true, allowTreasuryWithdrawals = true;
    public boolean logActions = true, protectWildernessFromClaimMachines = true;
    public int warningReopenTicks = 20, menuRefreshTicks = 20, territoryUpdateTicks = 20, beaconRefreshTicks = 80, beaconLevel2Multiplier = 4;
    public int maxNameLength = 24, leaderboardSize = 10, opLevel = 2;
    public int maxTrackedHazards = 250000;
    public long[] treasuryPresets = {1000, 10000, 100000};
    public String title = "kingdoms", primaryColor = "gold", secondaryColor = "aqua", errorColor = "red";
    public String territoryEnterSound = "minecraft:block.note_block.chime", territoryLeaveSound = "minecraft:block.note_block.hat", warCountdownSound = "minecraft:block.note_block.pling", warStartSound = "minecraft:entity.wither.spawn", warEndSound = "minecraft:ui.toast.challenge_complete", kingdomCreatedSound = "minecraft:ui.toast.challenge_complete", beaconPurchaseSound = "minecraft:block.beacon.activate", beaconHostileSound = "minecraft:entity.elder_guardian.curse";
    public boolean territoryActionBar = true, territoryTitles = true, territorySounds = true, warTitles = true, warActionBar = true, warSounds = true, broadcastKingdomCreation = true, beaconEnabled = true, beaconFx = true;
    public boolean leaderboardShowPosition = true, leaderboardShowIcon = true, leaderboardShowName = true, leaderboardShowTreasury = true, leaderboardShowCenter = true;
    public String overdueWarning = "Your kingdom will be disbanded and ALL land will become unprotected if the overdue upkeep is not paid before the deadline. Deposit enough money in the treasury.";
    public String acknowledgeLabel = "I have read and understand";
    public Set<String> allowedDimensions = new HashSet<>(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"));
    public Set<Flag> defaultFlags = EnumSet.of(Flag.AUTOMATION);
    public Set<Permission> defaultMemberPermissions = EnumSet.of(Permission.PLACE, Permission.BREAK, Permission.INTERACT, Permission.CONTAINERS, Permission.ENTITIES, Permission.DEPOSIT);
    public Set<Permission> defaultPublicPermissions = EnumSet.noneOf(Permission.class);
    public Set<String> allowedMobTypes = new HashSet<>(Set.of("minecraft:villager"));
    public Map<String, String> messages = new LinkedHashMap<>();
    public Map<String, Long> beaconEffectFees = new LinkedHashMap<>(Map.ofEntries(
        Map.entry("speed",100_000L),Map.entry("haste",125_000L),Map.entry("resistance",150_000L),
        Map.entry("jump_boost",100_000L),Map.entry("strength",200_000L),Map.entry("regeneration",250_000L),
        Map.entry("slowness",300_000L),Map.entry("blindness",400_000L),Map.entry("mining_fatigue",350_000L)));
    public Map<String, Integer> menuSlots = new LinkedHashMap<>(Map.of("claims",10,"treasury",12,"members",14,"settings",16,"war",28,"protection",30,"help",32,"leaderboard",34,"beacon",36,"close",49));
    public Map<String, String> menuItems = new LinkedHashMap<>(Map.of("claims","minecraft:grass_block","treasury","minecraft:gold_ingot","members","minecraft:player_head","settings","minecraft:comparator","war","minecraft:iron_sword","protection","minecraft:shield","help","minecraft:book","leaderboard","minecraft:emerald","beacon","minecraft:beacon"));
    public void validate() {
        if(blueMap==null)throw new IllegalArgumentException("BlueMap configuration is missing");blueMap.validate();
        if(artifacts==null)throw new IllegalArgumentException("Artifact configuration is missing");artifacts.validate();
        if(airDrops==null)throw new IllegalArgumentException("AirDrop configuration is missing");airDrops.validate();
        if(coreWarOutlineThicknessMilli<2||coreWarOutlineThicknessMilli>100)throw new IllegalArgumentException("Invalid wartime core outline thickness");
        if(coreHighlightRange<4||coreHighlightRange>128||coreParticleIntervalTicks<5||coreParticleIntervalTicks>100||coreDustSizePercent<5||coreDustSizePercent>200||coreOutlineThicknessMilli<2||coreOutlineThicknessMilli>100)throw new IllegalArgumentException("Invalid core highlight range, interval, dust size or outline thickness");
        if(coreReturnMinutes<1||coreReturnMinutes>10080||coreTaxPercent<0||coreTaxPercent>100||kingdomRejoinCooldownMinutes<0||kingdomRejoinCooldownMinutes>10080)throw new IllegalArgumentException("Invalid core return timeout, tax percentage, or rejoin cooldown");
        if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported config schema");
        if (maxTrackedHazards < 1000 || maxTrackedHazards > 2000000) throw new IllegalArgumentException("maxTrackedHazards must be 1000..2000000");
        for(long x : new long[]{creationFee,expansionFee,outpostFee,upkeepPerMember,upkeepBase,protectionFee}) if(x < 0 || x > 999_999_999_999L) throw new IllegalArgumentException("Fees must be between 0 and 999999999999");
        for(long x : new long[]{upkeepIntervalMinutes,gracePeriodMinutes,warMinutes,warCooldownMinutes,protectionMinutes,beaconDurationMinutes}) if(x < 1 || x > 5_256_000) throw new IllegalArgumentException("Durations must be 1..5256000 minutes");
        if(maxMembers < 1 || maxMembers > 10000 || maxClaims < 1 || maxClaims > 100000 || initialClaims != 1 || kingdomCreationBufferChunks < 0 || kingdomCreationBufferChunks > 10000 || spawnClaimProtectionChunks < 0 || spawnClaimProtectionChunks > 10000 || outpostClaimWeight < 1 || outpostClaimWeight > 1000 || maxOutposts < 0 || maxOutposts > 10000) throw new IllegalArgumentException("Invalid caps, initial claims, creation buffer, or outpost settings");
        if(newKingdomProtectionMinutes < 0 || newKingdomProtectionMinutes > 5_256_000 || opLevel < 1 || opLevel > 4 || warningReopenTicks < 1 || menuRefreshTicks < 1 || territoryUpdateTicks < 1 || beaconRefreshTicks < 1 || beaconRefreshTicks > 1200 || beaconLevel2Multiplier < 1 || beaconLevel2Multiplier > 1000 || maxNameLength < 3 || maxNameLength > 48 || leaderboardSize < 1 || leaderboardSize > 45) throw new IllegalArgumentException("Invalid UI / permission limits");
        if(warPreparationSeconds < 0 || warPreparationSeconds > 86400 || warCountdownMilestonesSeconds == null || warCountdownMilestonesSeconds.stream().anyMatch(x -> x < 1 || x > 86400)) throw new IllegalArgumentException("Invalid war preparation countdown");
        if(treasuryPresets == null || treasuryPresets.length != 3 || Arrays.stream(treasuryPresets).anyMatch(x -> x <= 0 || x > 999_999_999_999L)) throw new IllegalArgumentException("Supply three positive treasury presets");
        if(beaconEffectFees==null||beaconEffectFees.entrySet().stream().anyMatch(e->Arrays.stream(BeaconPower.values()).noneMatch(p->p.key().equals(e.getKey()))||e.getValue()==null||e.getValue()<0||e.getValue()>999_999_999_999L))throw new IllegalArgumentException("Invalid beacon effect fee map");
        Set<Integer> used = new HashSet<>();
        for(int slot : menuSlots.values()) if(slot < 0 || slot > 53 || !used.add(slot)) throw new IllegalArgumentException("Menu slots must be distinct, 0..53");
        if(allowedDimensions == null || defaultFlags == null || defaultMemberPermissions == null || defaultPublicPermissions == null || allowedMobTypes == null || messages == null || menuItems == null || warCountdownMilestonesSeconds == null || territoryEnterSound == null || territoryLeaveSound == null || warCountdownSound == null || warStartSound == null || warEndSound == null || kingdomCreatedSound == null || beaconPurchaseSound == null || beaconHostileSound == null || overdueWarning == null || acknowledgeLabel == null) throw new IllegalArgumentException("Config collections/text cannot be null");
    }
    public String message(String key, String fallback) { return messages.getOrDefault(key, fallback); }
    public long minute(long count) { return Math.multiplyExact(count, 60_000L); }
    public long second(long count) { return Math.multiplyExact(count, 1_000L); }
}
