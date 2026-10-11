package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class GuardianPacketMixin {
 @Inject(method="send(Lnet/minecraft/network/protocol/Packet;)V",at=@At("RETURN"))
 private void mask(Packet<?> packet,CallbackInfo ci){if(KingdomsMod.service!=null&&(Object)this instanceof ServerGamePacketListenerImpl listener)KingdomsMod.service.artifacts.guardianPackets(listener.player,packet);}
}
