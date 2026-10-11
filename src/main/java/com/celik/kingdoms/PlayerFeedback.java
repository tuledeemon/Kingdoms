package com.celik.kingdoms;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import java.util.*;
import static com.celik.kingdoms.Rules.*;

/** Server-only territory HUD and high-signal kingdom/war notifications. */
public final class PlayerFeedback {
    private final KingdomService service;
    private final Map<UUID, String> lastTerritory = new HashMap<>();

    public PlayerFeedback(KingdomService service) { this.service = service; }
    private static MutableComponent literal(String value) { return Component.literal(Ui.caps(value)); }

    public void join(ServerPlayer player) {
        lastTerritory.remove(player.getUUID());
        update(player, true);
    }

    public void leave(UUID player) { lastTerritory.remove(player); }

    public void tick() {
        for (ServerPlayer player : service.server.getPlayerList().getPlayers()) update(player, false);
    }

    private void update(ServerPlayer player, boolean joining) {
        Kingdom land = service.land(player.level(), player.blockPosition());
        String current = land == null ? "wilderness" : land.id.toString();
        String previous = lastTerritory.put(player.getUUID(), current);
        if (previous == null) {
            if (land != null) transition(player, null, land, true);
        } else if (!previous.equals(current)) {
            Kingdom old = previous.equals("wilderness") ? null : service.state.kingdoms.get(UUID.fromString(previous));
            transition(player, old, land, joining);
        }
        if (service.config.territoryActionBar || service.config.warActionBar) actionBar(player, land);
    }

    private void transition(ServerPlayer player, Kingdom old, Kingdom land, boolean joining) {
        if (service.config.territoryTitles) {
            if (land == null) {
                title(player, literal(Ui.caps("wilderness")).withStyle(ChatFormatting.GRAY),
                    literal(old == null ? "Unclaimed territory" : "Leaving " + old.display()).withStyle(ChatFormatting.DARK_GRAY), 8, 35, 10);
            } else {
                ChatFormatting color = territoryColor(player, land);
                title(player, literal(Ui.caps("territory")).withStyle(color),
                    identity(land).withStyle(color), 8, 40, 10);
            }
        }
        if (service.config.territorySounds) sound(player, land == null ? service.config.territoryLeaveSound : service.config.territoryEnterSound, 0.75f, land == null ? 0.85f : 1.15f);
    }

    private void actionBar(ServerPlayer player, Kingdom land) {
        MutableComponent line = Component.empty();
        if (service.config.territoryActionBar) {
            if (land == null) line.append(literal(Ui.caps("territory") + " › ").withStyle(ChatFormatting.DARK_GRAY)).append(literal("Wilderness").withStyle(ChatFormatting.GRAY));
            else line.append(literal(Ui.caps("territory") + " › ").withStyle(ChatFormatting.DARK_GRAY)).append(identity(land).withStyle(territoryColor(player, land)));
        }
        Kingdom own = service.own(player.getUUID());
        War conflict = own == null ? null : Rules.conflict(service.state, own.id, service.now());
        if (service.config.warActionBar && conflict != null) {
            Kingdom enemy = service.state.kingdoms.get(conflict.opponent(own.id));
            long remaining = (conflict.preparing(service.now()) ? conflict.startedAt : conflict.endsAt) - service.now();
            String state = conflict.preparing(service.now()) ? Ui.caps("preparing") : Ui.caps("vulnerable");
            if (!line.getSiblings().isEmpty()) line.append(literal("  •  ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(literal(Ui.caps("war") + " › ").withStyle(ChatFormatting.DARK_RED))
                .append(literal(enemy.display() + " • " + state + " " + Ui.clock(remaining)).withStyle(conflict.preparing(service.now()) ? ChatFormatting.GOLD : ChatFormatting.RED));
        }
        player.connection.send(new ClientboundSetActionBarTextPacket(line));
    }

    public void kingdomCreated(ServerPlayer founder, Kingdom kingdom) {
        MutableComponent message = literal("[" + Ui.caps("kingdom") + "] ").withStyle(ChatFormatting.GOLD)
            .append(KingdomIcons.styled(kingdom).withStyle(ChatFormatting.YELLOW))
            .append(literal(" was founded by " + founder.getName().getString() + ".").withStyle(ChatFormatting.WHITE));
        for (ServerPlayer player : service.server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
            sound(player, service.config.kingdomCreatedSound, 0.8f, 1.0f);
        }
        if(founder.connection!=null)title(founder, literal(Ui.caps("kingdom founded")).withStyle(ChatFormatting.GOLD), identity(kingdom).withStyle(ChatFormatting.YELLOW), 10, 55, 15);
        ((net.minecraft.server.level.ServerLevel)founder.level()).sendParticles(ParticleTypes.FIREWORK, founder.getX(), founder.getY() + 1.0, founder.getZ(), 36, 1.0, 1.0, 1.0, 0.12);
        if(founder.connection!=null)join(founder);
    }

    public void beaconPurchased(BeaconEffect effect) {
        Kingdom source=service.state.kingdoms.get(effect.sourceKingdom),target=service.state.kingdoms.get(effect.targetKingdom);if(source==null||target==null)return;
        String power=KingdomBeacon.label(effect.power)+(effect.level==2?" II":" I");long remaining=effect.expiresAt-service.now();
        for(ServerPlayer player:service.server.getPlayerList().getPlayers()){
            Kingdom own=service.own(player.getUUID());if(own==null)continue;
            if(own.id.equals(target.id)){
                boolean hostile=effect.hostile;player.sendSystemMessage(literal("["+Ui.caps("beacon")+"] ").withStyle(hostile?ChatFormatting.DARK_RED:ChatFormatting.DARK_AQUA).append(literal((hostile?source.display()+" inflicted ":"Your kingdom activated ")+power+" for "+Ui.time(remaining)+".").withStyle(hostile?ChatFormatting.RED:ChatFormatting.AQUA)));
                if(service.config.beaconFx){title(player,literal(Ui.caps(hostile?"hostile beacon":"kingdom beacon")).withStyle(hostile?ChatFormatting.DARK_RED:ChatFormatting.AQUA),literal(power+" • "+Ui.time(remaining)).withStyle(hostile?ChatFormatting.RED:ChatFormatting.GOLD),5,45,10);sound(player,hostile?service.config.beaconHostileSound:service.config.beaconPurchaseSound,1.0f,hostile?0.75f:1.15f);((net.minecraft.server.level.ServerLevel)player.level()).sendParticles(hostile?ParticleTypes.SMOKE:ParticleTypes.END_ROD,player.getX(),player.getY()+1.0,player.getZ(),28,0.7,1.0,0.7,0.05);}
            } else if(effect.hostile&&own.id.equals(source.id))player.sendSystemMessage(literal("["+Ui.caps("beacon")+"] ").withStyle(ChatFormatting.DARK_PURPLE).append(literal(power+" purchased against "+target.display()+" for "+Ui.time(remaining)+".").withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }

    public void warDeclared(War war) {
        Kingdom first = service.state.kingdoms.get(war.first), second = service.state.kingdoms.get(war.second);
        service.announce(literal("[" + Ui.caps("war") + "] ").withStyle(ChatFormatting.DARK_RED)
            .append(literal(first.display()).withStyle(ChatFormatting.RED))
            .append(literal(" declared war on ").withStyle(ChatFormatting.WHITE))
            .append(literal(second.display()).withStyle(ChatFormatting.RED))
            .append(literal(". Claims remain protected for " + Ui.clock(war.startedAt - service.now()) + ".").withStyle(ChatFormatting.GOLD)));
        forMembers(war, p -> {
            if(service.config.warTitles)title(p, literal(Ui.caps("war declared")).withStyle(ChatFormatting.DARK_RED), literal(first.display() + "  ⚔  " + second.display()).withStyle(ChatFormatting.RED), 10, 60, 15);
            if(service.config.warSounds)sound(p, service.config.warCountdownSound, 1.0f, 0.8f);
        });
    }

    public void warCountdown(War war, int seconds) {
        Kingdom enemyA = service.state.kingdoms.get(war.second), enemyB = service.state.kingdoms.get(war.first);
        forMembers(war, p -> {
            Kingdom own = service.own(p.getUUID()); Kingdom enemy = own != null && own.id.equals(war.first) ? enemyA : enemyB;
            String remaining = Ui.clock(seconds * 1000L);
            p.sendSystemMessage(literal("[" + Ui.caps("war") + "] ").withStyle(ChatFormatting.DARK_RED)
                .append(literal("Claims become vulnerable in " + remaining + " against " + enemy.display() + ".").withStyle(ChatFormatting.GOLD)));
            if (service.config.warTitles && (seconds <= 10 || seconds == 30 || seconds == 60 || seconds >= 180))
                title(p, literal(remaining).withStyle(seconds <= 5 ? ChatFormatting.RED : ChatFormatting.GOLD), literal("Until claims become vulnerable").withStyle(ChatFormatting.GRAY), 2, seconds <= 5 ? 14 : 30, 4);
            if(service.config.warSounds)sound(p, service.config.warCountdownSound, 0.9f, seconds <= 5 ? 1.5f : 1.1f);
        });
    }

    public void warStarted(War war) {
        Kingdom first = service.state.kingdoms.get(war.first), second = service.state.kingdoms.get(war.second);
        service.announce(literal("[" + Ui.caps("war") + "] ").withStyle(ChatFormatting.DARK_RED)
            .append(literal(first.display() + " and " + second.display() + " are now at war. Claims are vulnerable for " + Ui.clock(war.endsAt - service.now()) + ".").withStyle(ChatFormatting.RED)));
        forMembers(war, p -> {
            if(service.config.warTitles)title(p, literal(Ui.caps("claims vulnerable")).withStyle(ChatFormatting.RED), literal("Wartime has begun").withStyle(ChatFormatting.DARK_RED), 5, 70, 15);
            if(service.config.warSounds)sound(p, service.config.warStartSound, 1.0f, 1.0f);
        });
    }

    public void warEnded(War war, Kingdom first, Kingdom second) {
        service.announce(literal("[" + Ui.caps("war") + "] ").withStyle(ChatFormatting.DARK_GREEN)
            .append(literal("War ended: " + first.display() + " and " + second.display() + ". Claims are protected again.").withStyle(ChatFormatting.GREEN)));
        for (ServerPlayer player : service.server.getPlayerList().getPlayers()) {
            Kingdom own = service.own(player.getUUID());
            if (own != null && (own.id.equals(war.first) || own.id.equals(war.second))) {
                if(service.config.warTitles)title(player, literal(Ui.caps("war ended")).withStyle(ChatFormatting.GREEN), literal("Claims are protected again").withStyle(ChatFormatting.DARK_GREEN), 8, 55, 15);
                if(service.config.warSounds)sound(player, service.config.warEndSound, 0.9f, 1.1f);
            }
        }
    }

    private void forMembers(War war, java.util.function.Consumer<ServerPlayer> action) {
        for (ServerPlayer player : service.server.getPlayerList().getPlayers()) {
            Kingdom own = service.own(player.getUUID());
            if (own != null && war.involves(own.id)) action.accept(player);
        }
    }

    private MutableComponent identity(Kingdom kingdom) {
        return KingdomIcons.styled(kingdom);
    }

    private ChatFormatting territoryColor(ServerPlayer player, Kingdom land) {
        Kingdom own = service.own(player.getUUID());
        if (own != null && own.id.equals(land.id)) return ChatFormatting.GREEN;
        if (Rules.enemy(service.state, land, player.getUUID(), service.now())) return ChatFormatting.RED;
        return ChatFormatting.GOLD;
    }

    private static void title(ServerPlayer player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private static void sound(ServerPlayer player, String id, float volume, float pitch) {
        Identifier key = Identifier.tryParse(id);
        if (key == null) return;
        BuiltInRegistries.SOUND_EVENT.get(key).ifPresent(sound -> player.connection.send(new ClientboundSoundPacket(sound, SoundSource.UI, player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong())));
    }
}
