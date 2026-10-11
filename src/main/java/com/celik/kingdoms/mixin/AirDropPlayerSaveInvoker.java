package com.celik.kingdoms.mixin;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(PlayerList.class)
public interface AirDropPlayerSaveInvoker {@Invoker("save") void kingdoms$saveAirDropPlayer(ServerPlayer player);}
