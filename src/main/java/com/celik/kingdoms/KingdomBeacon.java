package com.celik.kingdoms;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import static com.celik.kingdoms.Rules.*;

/** Minecraft bindings and exact pricing for persistent kingdom beacon powers. */
public final class KingdomBeacon {
    public static Holder<MobEffect> effect(BeaconPower power) {
        return switch(power) {
            case SPEED -> MobEffects.SPEED;
            case HASTE -> MobEffects.HASTE;
            case RESISTANCE -> MobEffects.RESISTANCE;
            case JUMP_BOOST -> MobEffects.JUMP_BOOST;
            case STRENGTH -> MobEffects.STRENGTH;
            case REGENERATION -> MobEffects.REGENERATION;
            case SLOWNESS -> MobEffects.SLOWNESS;
            case BLINDNESS -> MobEffects.BLINDNESS;
            case MINING_FATIGUE -> MobEffects.MINING_FATIGUE;
            case GLOWING -> MobEffects.GLOWING;
        };
    }
    public static Item icon(BeaconPower power) {
        return switch(power) {
            case SPEED -> Items.SUGAR;
            case HASTE -> Items.GOLDEN_PICKAXE;
            case RESISTANCE -> Items.SHIELD;
            case JUMP_BOOST -> Items.RABBIT_FOOT;
            case STRENGTH -> Items.BLAZE_POWDER;
            case REGENERATION -> Items.GHAST_TEAR;
            case SLOWNESS -> Items.COBWEB;
            case BLINDNESS -> Items.INK_SAC;
            case MINING_FATIGUE -> Items.DIAMOND_PICKAXE;
            case GLOWING -> Items.GLOW_INK_SAC;
        };
    }
    public static String label(BeaconPower power) {
        return switch(power) {
            case SPEED -> "Speed";
            case HASTE -> "Haste";
            case RESISTANCE -> "Resistance";
            case JUMP_BOOST -> "Jump boost";
            case STRENGTH -> "Strength";
            case REGENERATION -> "Regeneration";
            case SLOWNESS -> "Slowness";
            case BLINDNESS -> "Blindness";
            case MINING_FATIGUE -> "Mining fatigue";
            case GLOWING -> "Glowing";
        };
    }
    public static String description(BeaconPower power) {
        return switch(power) {
            case SPEED -> "Members move faster.";
            case HASTE -> "Members mine and attack faster.";
            case RESISTANCE -> "Members take less damage.";
            case JUMP_BOOST -> "Members jump higher.";
            case STRENGTH -> "Members deal more melee damage.";
            case REGENERATION -> "Members regenerate health faster.";
            case SLOWNESS -> "Enemy members move more slowly.";
            case BLINDNESS -> "Enemy members lose long-range vision.";
            case MINING_FATIGUE -> "Enemy members mine and attack more slowly.";
            case GLOWING -> "Reveals enemy members through blocks while in claimed land.";
        };
    }
    public static long cost(Config config,BeaconPower power,int level) {
        Long base=config.beaconEffectFees.get(power.key());
        if(base==null)throw new IllegalArgumentException("That beacon effect is disabled in the server configuration.");
        if(level!=1&&(!power.beneficial()||level!=2))throw new IllegalArgumentException("Choose a supported beacon level.");
        return level==2?Math.multiplyExact(base,config.beaconLevel2Multiplier):base;
    }
    private KingdomBeacon() {}
}
