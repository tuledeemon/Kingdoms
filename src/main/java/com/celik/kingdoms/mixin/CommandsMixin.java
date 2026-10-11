package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.RootCommandNode;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.commands.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Filters the client command tree and scopes operator-issued world mutation commands. */
@Mixin(Commands.class)
public abstract class CommandsMixin {
 @ModifyArg(method="sendCommands",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ClientboundCommandsPacket;<init>(Lcom/mojang/brigadier/tree/RootCommandNode;Lnet/minecraft/network/protocol/game/ClientboundCommandsPacket$NodeInspector;)V"),index=0)
 private RootCommandNode<CommandSourceStack> kingdomCommandTree(RootCommandNode<CommandSourceStack> root){return KingdomCommandVisibility.clientTree(root);}
 @WrapMethod(method="performCommand")
 private void operatorCommand(ParseResults<CommandSourceStack> command,String commandString,Operation<Void> original){
  CommandSourceStack source=command.getContext().getSource();ServerPlayer player=source.getPlayer();
  if(player==null||!Protection.bypass(player.level(),player.getUUID())){original.call(command,commandString);return;}
  try(var scope=Protection.scope(player.level(),player.blockPosition(),player.getUUID(),player,Protection.Cause.PLAYER)){original.call(command,commandString);}
 }
}
